package io.listen.generator;

import io.listen.model.Segment;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class SegmentAllocator {

    @Transactional
    public Segment getNextSegment(String bizType) {
        try {
            // 使用悲观锁，防止并发问题
            Segment segment = Segment.find("bizType", bizType).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult();

            // 计算新的最大值
            Long currentMax = segment.currentMax;
            Integer step = segment.step;
            long newMax = currentMax + segment.step;
            // 更新数据库
            segment.currentMax = newMax;
            segment.persist();

            // 返回分配的号段
            Log.infof("Allocated new segment for %s: %s", bizType, segment);
            return new Segment(currentMax + 1, newMax, step);

        } catch (Exception e) {
            Log.errorf(e, "Failed to allocate segment for bizType: %s", bizType);
            throw new RuntimeException("Failed to allocate segment", e);
        }
    }

    /**
     * 初始化业务类型（如果不存在）
     */
    @Transactional
    public void initBizTypeIfNotExists(String bizType, long initialValue, int step) {
        long counted = Segment.count("bizType", bizType);
        if (counted == 0) {
            Segment segment = new Segment();
            segment.currentMax = initialValue;
            segment.step = step;
            segment.bizType = bizType;
            segment.persist();
            Log.infof("Initialized segment for bizType: %s, initialValue: %d, step: %d",
                    bizType, initialValue, step);
        }
    }
}
