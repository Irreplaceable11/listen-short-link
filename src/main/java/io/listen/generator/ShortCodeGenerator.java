package io.listen.generator;

import io.listen.config.SegmentConfig;
import io.listen.model.Segment;
import io.listen.utils.JsonUtils;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class ShortCodeGenerator{

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
    private Uni<Void> ensureInitialized() {
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


//    @PostConstruct
//    public void init() {
//        Log.info("Init ShortCodeGenerator");
//        idObfuscator = new IdObfuscator(PRIME, MOD);
//        // 只初始化非响应式组件
//
//        //初始化业务类型
//        segmentAllocator.initBizTypeIfNotExists(BIZ_TYPE, segmentConfig.initialValue(), segmentConfig.step())
//                .flatMap(ignored -> segmentAllocator.getNextSegment(BIZ_TYPE))
//                .invoke(segment -> {
//                    synchronized (this) {
//                        currentSegment.set(segment);
//                        currentId.set(segment.start);
//                        Log.infof("Initialized with segment: %s", JsonUtils.toJson(currentSegment));
//                    }
//                })
//                .onFailure()
//                .invoke(e -> Log.error("Failed to initialize segment", e))
//                .subscribe()
//                .with(
//                        result -> {},
//                        Unchecked.consumer(failure -> {
//                            throw new RuntimeException("Failed to initialize ShortCodeGenerator", failure);
//                        })
//                );
//    }

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
                            // 异步预取，不阻塞当前请求
                            asyncPrefetchNextSegment()
                                    .subscribe()
                                    .with(
                                            segment -> Log.infof("Background prefetch completed: %s", segment),
                                            failure -> Log.error("Background prefetch failed", failure)
                                    );
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
                    if (currentId.get() <= current.end) {
                        return null; // 不需要切换了
                    }

                    // 尝试使用预取的号段
                    return nextSegment.getAndSet(null);
                })
                .flatMap(next -> {
                    if (next != null) {
                        // 使用预取的号段
                        return Uni.createFrom().item(next)
                                .invoke(segment -> Log.infof("Using prefetched segment: %s", segment));
                    } else {
                        // 没有预取的号段，立即获取新号段
                        return segmentAllocator.getNextSegment(BIZ_TYPE)
                                .invoke(segment -> Log.infof("Allocated new segment on demand: %s", segment));
                    }
                })
                .invoke(newSegment -> {
                    currentSegment.set(newSegment);
                    currentId.set(newSegment.start);
                })
                .replaceWithVoid()
                .onTermination()
                .invoke(() -> switchingUni.set(null)) // 清除切换状态
                .onFailure()
                .invoke(e -> Log.error("Failed to switch segment", e))
                .onFailure()
                .transform(e -> new RuntimeException("Failed to switch to next segment", e));

        // 尝试设置切换操作
        if (switchingUni.compareAndSet(null, switchingOperation)) {
            return switchingOperation;
        } else {
            // 其他线程已经开始切换，等待其完成
            return switchingUni.get();
        }
    }

    /**
     * 异步预取下一个号段
     */
     private Uni<Segment> asyncPrefetchNextSegment() {
        // 检查是否已经有预取操作在进行或已完成
        if (nextSegment.get() != null) {
            return Uni.createFrom().item(nextSegment.get());
        }

        Uni<Segment> existingPrefetch = prefetchingUni.get();
        if (existingPrefetch != null) {
            return existingPrefetch;
        }

        // 创建新的预取操作
        Uni<Segment> prefetchOperation = segmentAllocator.getNextSegment(BIZ_TYPE)
                .invoke(segment -> {
                    nextSegment.set(segment);
                    Log.infof("Successfully prefetched next segment: %s", segment);
                })
                .onTermination()
                .invoke(() -> prefetchingUni.set(null)) // 清除预取状态
                .onFailure()
                .invoke(failure -> {
                    Log.error("Failed to prefetch next segment", failure);
                    nextSegment.set(null); // 确保失败时清除状态
                })
                .runSubscriptionOn(Infrastructure.getDefaultExecutor());

        // 尝试设置预取操作
        if (prefetchingUni.compareAndSet(null, prefetchOperation)) {
            return prefetchOperation;
        } else {
            // 其他线程已经开始预取，返回其结果
            return prefetchingUni.get();
        }
    }

    /**
     * Base62编码
     */
    private String encode(long num) {
        if (num == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }

        return sb.reverse().toString();
    }

    private String encodeFixedLength(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        while (sb.length() < ShortCodeGenerator.CODE_LENGTH) {
            sb.append(BASE62_CHARS.charAt((int) (Math.random() * BASE)));
        }
        return sb.reverse().toString();
    }

}
