package io.listen.generator;

import io.listen.config.SegmentConfig;
import io.listen.model.Segment;
import io.listen.utils.JsonUtils;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ShortCodeGenerator {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_CHARS.length();
    private static final String BIZ_TYPE = "short_link";
    private static final int CODE_LENGTH = 7;
    private static final long MOD = 3521614606208L; // 62^7，保证可映射7位范围
    private static final long PRIME = 1580030173L; // 与MOD互质的素数

    @Inject
    SegmentAllocator segmentAllocator;

    @Inject
    SegmentConfig segmentConfig;

    private IdObfuscator idObfuscator;

    // 使用 AtomicReference 来线程安全地管理当前号段
    private final AtomicReference<Segment> currentSegment = new AtomicReference<>();

    // 预取的下一个号段
    private final AtomicReference<Segment> nextSegment = new AtomicReference<>();

    // 当前ID计数器
    private final AtomicLong currentId = new AtomicLong(0);

    // 预取状态 - 使用 AtomicReference 来管理异步预取状态
    private final AtomicReference<Uni<Segment>> prefetchingUni = new AtomicReference<>();

    // 号段切换状态 - 防止并发切换
    private final AtomicReference<Uni<Void>> switchingUni = new AtomicReference<>();

    // 初始化状态
    private volatile boolean initialized = false;

    private final Object initLock = new Object();

    @PostConstruct
    public void init() {
        Log.info("Init ShortCodeGenerator");
        idObfuscator = new IdObfuscator(PRIME, MOD);
    }

    /**
     * 确保已初始化 - 懒初始化模式
     */
    public Uni<Void> ensureInitialized() {
        if (initialized) {
            return Uni.createFrom().voidItem();
        }

        synchronized (initLock) {
            if (initialized) {
                return Uni.createFrom().voidItem();
            }

            Log.info("Lazy initializing ShortCodeGenerator");
            return initializeSegment()
                    .invoke(() -> initialized = true);
        }
    }

    private Uni<Void> initializeSegment() {
        return segmentAllocator.initBizTypeIfNotExists(BIZ_TYPE, segmentConfig.initialValue(), segmentConfig.step())
                .flatMap(ignored -> segmentAllocator.getNextSegment(BIZ_TYPE))
                .invoke(segment -> {
                    currentSegment.set(segment);
                    currentId.set(segment.start);
                    Log.infof("Initialized with segment: %s", JsonUtils.toJson(segment));
                })
                .replaceWithVoid()
                .onFailure()
                .invoke(e -> Log.error("Failed to initialize segment", e));
    }

    /**
     * 生成短码
     */
    public Uni<String> generateShortCode() {
        return ensureInitialized()
                .flatMap(ignored -> nextId())
                .map(id -> {
                    long obfuscated = idObfuscator.obfuscate(id);
                    return encodeFixedLength(obfuscated);
                });
    }

    private Uni<Long> nextId() {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
                    Segment current = currentSegment.get();
                    if (current == null) {
                        throw new IllegalStateException("ShortCodeGenerator not initialized");
                    }

                    long id = currentId.getAndIncrement();

                    // 检查是否超出当前号段范围
                    if (id <= current.end) {
                        // 检查是否需要预取下一个号段
                        long remaining = current.end - id;
                        if (remaining <= segmentConfig.prefetchThreshold()) {
                            // 触发异步预取，但不等待结果
                            triggerAsyncPrefetch();
                        }
                        return id;
                    } else {
                        // 需要切换号段，返回 null 表示需要重试
                        return null;
                    }
                }))
                .flatMap(id -> {
                    if (id != null) {
                        return Uni.createFrom().item(id);
                    } else {
                        // 需要切换号段后重试
                        return switchToNextSegment()
                                .flatMap(ignored -> nextId()); // 递归重试
                    }
                });
    }

    /**
     * 触发异步预取（不等待结果）
     */
    private void triggerAsyncPrefetch() {
        // 检查是否已经有预取的号段或正在预取
        if (nextSegment.get() != null || prefetchingUni.get() != null) {
            return;
        }

        // 创建预取操作
        Uni<Segment> prefetchOperation = segmentAllocator.getNextSegment(BIZ_TYPE)
                .invoke(segment -> {
                    nextSegment.set(segment);
                    Log.infof("Successfully prefetched next segment: %s", JsonUtils.toJson(segment));
                })
                .onTermination()
                .invoke(() -> prefetchingUni.set(null))
                .onFailure()
                .invoke(failure -> {
                    Log.error("Failed to prefetch next segment", failure);
                    nextSegment.set(null);
                })
                // 重要：使用 memoize() 确保操作只执行一次
                .memoize().indefinitely();

        // 尝试设置预取操作
        if (prefetchingUni.compareAndSet(null, prefetchOperation)) {
            // 触发执行但不等待结果
            prefetchOperation.subscribe().with(
                    segment -> {}, // 成功时已经在 invoke 中处理
                    failure -> {}  // 失败时已经在 onFailure 中处理
            );
        }
    }

    /**
     * 异步切换到下一个号段
     */
    private Uni<Void> switchToNextSegment() {
        // 检查是否已经有切换操作在进行
        Uni<Void> existingSwitching = switchingUni.get();
        if (existingSwitching != null) {
            // 等待现有的切换完成
            return existingSwitching;
        }

        // 创建新的切换操作
        Uni<Void> switchingOperation = Uni.createFrom().item(() -> {
                    // 双重检查，确保还需要切换
                    Segment current = currentSegment.get();
                    long currentIdValue = currentId.get();
                    if (current != null && currentIdValue <= current.end) {
                        return null; // 不需要切换了
                    }

                    // 尝试使用预取的号段
                    Segment prefetched = nextSegment.getAndSet(null);

                    // 同时清除预取操作状态
                    prefetchingUni.set(null);

                    return prefetched;
                })
                .flatMap(next -> {
                    if (next != null) {
                        // 使用预取的号段
                        return Uni.createFrom().item(next)
                                .invoke(segment -> Log.infof("Using prefetched segment: %s", JsonUtils.toJson(segment)));
                    } else {
                        // 检查是否有正在进行的预取操作
                        Uni<Segment> ongoingPrefetch = prefetchingUni.get();
                        if (ongoingPrefetch != null) {
                            // 等待正在进行的预取完成
                            return ongoingPrefetch
                                    .onItem().ifNull().switchTo(() ->
                                            // 如果预取失败，重新获取
                                            segmentAllocator.getNextSegment(BIZ_TYPE)
                                    );
                        } else {
                            // 没有预取的号段，立即获取新号段
                            return segmentAllocator.getNextSegment(BIZ_TYPE)
                                    .invoke(segment -> Log.infof("Allocated new segment on demand: %s", JsonUtils.toJson(segment)));
                        }
                    }
                })
                .invoke(newSegment -> {
                    currentSegment.set(newSegment);
                    currentId.set(newSegment.start);
                    Log.infof("Switched to new segment: start=%d, end=%d", newSegment.start, newSegment.end);
                })
                .replaceWithVoid()
                .onTermination()
                .invoke(() -> switchingUni.set(null)) // 清除切换状态
                .onFailure()
                .invoke(e -> Log.error("Failed to switch segment", e))
                .onFailure()
                .transform(e -> new RuntimeException("Failed to switch to next segment", e))
                // 使用 memoize 确保同一个切换操作只执行一次
                .memoize().indefinitely();

        // 尝试设置切换操作
        if (switchingUni.compareAndSet(null, switchingOperation)) {
            return switchingOperation;
        } else {
            // 其他线程已经开始切换，等待其完成
            return switchingUni.get();
        }
    }

    /**
     * Base62编码（固定长度）
     */
    private String encodeFixedLength(long num) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.reverse().toString();
    }
}