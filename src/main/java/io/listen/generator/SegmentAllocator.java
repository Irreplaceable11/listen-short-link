package io.listen.generator;

import io.listen.model.Segment;
import io.listen.utils.JsonUtils;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;


@ApplicationScoped
public class SegmentAllocator {

    @WithTransaction
    public Uni<Segment> getNextSegment(String bizType) {
        return Panache.withTransaction(() ->
                Segment.find("bizType", bizType).withLock(LockModeType.PESSIMISTIC_WRITE)
                        .<Segment>firstResult()
                        .flatMap(segment -> {
                            // 计算新的最大值
                            Long currentMax = segment.currentMax;
                            Integer step = segment.step;
                            long newMax = currentMax + segment.step;
                            // 更新数据库
                            segment.currentMax = newMax;
                            return segment.<Segment>persist()
                                    .invoke(val -> Log.infof("Allocated new segment for %s: %s", bizType, JsonUtils.toJson(val)))
                                    .replaceWith(new Segment(currentMax + 1, newMax, step));
                        })
                        .onFailure()
                        .invoke(e -> Log.errorf(e, "Failed to allocate segment for bizType: %s", bizType))
                        .onFailure()
                        .transform(e -> new RuntimeException("Failed to allocate segment", e)));
    }

    /**
     * 初始化业务类型（如果不存在）
     */
    public Uni<Void> initBizTypeIfNotExists(String bizType, long initialValue, int step) {
        return Panache.withTransaction(() ->
                Segment.count("bizType", bizType)
                .flatMap(count -> {
                    if (count == 0) {
                        Segment segment = new Segment();
                        segment.currentMax = initialValue;
                        segment.step = step;
                        segment.bizType = bizType;
                        return segment.persist().invoke(() -> Log.infof("Initialized segment for bizType: %s, initialValue: %d, step: %d",
                                bizType, initialValue, step))
                                .replaceWithVoid();
                    }
                    return Uni.createFrom().voidItem();
                }));
    }
}
