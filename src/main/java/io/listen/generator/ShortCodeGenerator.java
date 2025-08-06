package io.listen.generator;

import io.listen.config.SegmentConfig;
import io.listen.model.Segment;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ShortCodeGenerator {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = BASE62_CHARS.length();
    private static final String BIZ_TYPE = "short_link";
    private static final int CODE_LENGTH = 7;
    private static final long MOD = 3521614606208L; // 62^7，保证可映射7位范围
    private static final long PRIME = 1580030173L; // 与MOD互质的素数

    @Inject
    SnowflakeIdGenerator snowflakeIdGenerator;

    @Inject
    SegmentAllocator segmentAllocator;

    @Inject
    SegmentConfig segmentConfig;

    private IdObfuscator idObfuscator;

    //当前使用的号段
    private volatile Segment currentSegment;

    //获取下一个号段
    private volatile Segment nextSegment;

    // 当前ID计数器
    private final AtomicLong currentId = new AtomicLong(0);
    // 号段切换锁
    private final Object segmentLock = new Object();
    // 预取锁
    private final Object prefetchLock = new Object();
    // 预取状态
    private volatile boolean prefetching = false;

    @PostConstruct
    public void init() {
        idObfuscator = new IdObfuscator(PRIME, MOD);
        //初始化业务类型
        segmentAllocator.initBizTypeIfNotExists(BIZ_TYPE, segmentConfig.initialValue(), segmentConfig.step());

        try {
            currentSegment = segmentAllocator.getNextSegment(BIZ_TYPE);
            currentId.set(currentSegment.start);
            Log.infof("Initialized with segment: %s", currentSegment);
        } catch (Exception e) {
            Log.error("Failed to initialize segment", e);
            throw new RuntimeException("Failed to initialize ShortCodeGenerator", e);
        }
    }

    /**
     * 生成短码
     */
    public String generateShortCode() {
        // 分布式ID生成
        //long id = snowflakeIdGenerator.nextId();
        // 使用模运算将 ID 映射到一个较小的范围（例如 62^8 范围内）
        // long smallId = id % (long) Math.pow(BASE, MIN_LENGTH);
        long id = nextId();
        long obfuscate = idObfuscator.obfuscate(id);
        return encodeFixedLength(obfuscate, CODE_LENGTH);
    }

    private Long nextId() {
        while (true) {
            long current = currentId.getAndIncrement();
            // 检查是否超出当前号段范围
            if (current <= currentSegment.end) {
                // 检查是否需要预取下一个号段
                long remaining = currentSegment.end - current;
                if (remaining <= segmentConfig.prefetchThreshold() && !prefetching) {
                    asyncPrefetchNextSegment();
                }
                return current;
            } else {
                // 需要切换到下一个号段
                switchToNextSegment();
                // 重试获取ID
            }
        }
    }

    /**
     * 切换到下一个号段
     */
    private void switchToNextSegment() {
        synchronized (segmentLock) {
            // 双重检查，避免重复切换
            if (currentId.get() <= currentSegment.end) {
                return;
            }

            try {
                if (nextSegment != null) {
                    // 使用预取的号段
                    currentSegment = nextSegment;
                    nextSegment = null;
                    prefetching = false;
                    Log.infof("Switched to prefetched segment: %s", currentSegment);
                } else {
                    // 没有预取，立即申请新号段
                    currentSegment = segmentAllocator.getNextSegment(BIZ_TYPE);
                    Log.infof("Switched to new segment: %s", currentSegment);
                }

                currentId.set(currentSegment.start);

            } catch (Exception e) {
                Log.error("Failed to switch segment", e);
                throw new RuntimeException("Failed to get next segment", e);
            }
        }
    }

    /**
     * 异步预取下一个号段
     */
    private void asyncPrefetchNextSegment() {
        synchronized (prefetchLock) {
            if (prefetching || nextSegment != null) {
                return;
            }

            prefetching = true;

            // 使用线程池异步执行
            Uni.createFrom().item(() -> nextSegment = segmentAllocator.getNextSegment(BIZ_TYPE))
                    .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                    .subscribe().with(
                            segment -> {
                                nextSegment = segment;
                                Log.infof("Prefetched next segment: %s", nextSegment);
                            },
                            failure -> {
                                Log.error("Failed to prefetch next segment", failure);
                                prefetching = false;
                            });
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

    private String encodeFixedLength(long num, int length) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        while (sb.length() < length) {
            sb.append(BASE62_CHARS.charAt((int) (Math.random() * BASE)));
        }
        return sb.reverse().toString();
    }

}
