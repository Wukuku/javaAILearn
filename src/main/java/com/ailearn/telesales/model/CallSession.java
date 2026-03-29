package com.ailearn.telesales.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通话会话状态
 *
 * 面试要点：
 * 1. 状态机设计：ACTIVE → ENDED / ABANDONED，防止重复处理已结束的通话
 * 2. AtomicInteger 保证并发场景下轮次计数准确（多线程流式回调）
 * 3. 记录 startTime 用于通话时长统计和超时清理
 * 4. lastIntent 缓存最近意图：下一轮发言可直接用上一轮意图做话术预加载，降低延迟
 */
public record CallSession(
        String conversationId,
        String customerId,
        CallStatus status,
        Instant startTime,
        AtomicInteger turnCount,
        String lastIntent
) {

    public enum CallStatus {
        /** 通话进行中 */
        ACTIVE,
        /** 正常结束 */
        ENDED,
        /** 客户挂断 / 超时 */
        ABANDONED
    }

    /** 创建新会话（工厂方法） */
    public static CallSession create(String conversationId, String customerId) {
        return new CallSession(
                conversationId,
                customerId,
                CallStatus.ACTIVE,
                Instant.now(),
                new AtomicInteger(0),
                null
        );
    }

    /** 进入下一轮，返回当前轮次号 */
    public int nextTurn() {
        return turnCount.incrementAndGet();
    }

    /** 获取通话时长（秒） */
    public long durationSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }

    /** 是否仍在进行中 */
    public boolean isActive() {
        return status == CallStatus.ACTIVE;
    }
}
