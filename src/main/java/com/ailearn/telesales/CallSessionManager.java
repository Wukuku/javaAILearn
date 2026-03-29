package com.ailearn.telesales;

import com.ailearn.telesales.model.CallSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通话会话管理器
 *
 * 面试重点：生产级状态管理的三个关键点
 *
 * 1. 并发安全：ConcurrentHashMap 保证多线程下的会话读写安全
 *    电销系统同时有几百路通话，每路通话的流式回调在不同线程执行
 *
 * 2. 内存泄漏防护：cleanExpiredSessions() 清理超时会话
 *    生产环境会用定时任务（@Scheduled）每5分钟调用一次
 *    本项目简化为手动调用，演示思路即可
 *
 * 3. 状态机约束：已结束的会话不允许重新激活
 *    防止异步回调把已结束的通话重新计入统计
 *
 * 生产扩展方向：
 * - 替换 ConcurrentHashMap → Redis（支持多实例、持久化、TTL 自动过期）
 * - 新增 WebSocket 推送：会话状态变更实时通知前端
 */
@Slf4j
@Component
public class CallSessionManager {

    /** 通话超时时间（秒），超过此时间未有新消息视为放弃 */
    private static final long SESSION_TIMEOUT_SECONDS = 1800; // 30分钟

    private final Map<String, CallSession> sessions = new ConcurrentHashMap<>();

    /**
     * 创建或恢复会话
     * 已有 ACTIVE 会话直接返回（幂等），避免重复初始化
     */
    public CallSession getOrCreate(String conversationId, String customerId) {
        return sessions.computeIfAbsent(conversationId,
                id -> {
                    log.info("[会话管理] 新建会话 conversationId={} customerId={}", id, customerId);
                    return CallSession.create(id, customerId);
                });
    }

    /**
     * 查询会话（不创建）
     */
    public Optional<CallSession> find(String conversationId) {
        return Optional.ofNullable(sessions.get(conversationId));
    }

    /**
     * 更新会话状态（状态机：只允许 ACTIVE → ENDED / ABANDONED）
     */
    public void endSession(String conversationId, CallSession.CallStatus status) {
        sessions.computeIfPresent(conversationId, (id, session) -> {
            if (!session.isActive()) {
                log.warn("[会话管理] 会话 {} 已处于 {} 状态，忽略本次结束请求", id, session.status());
                return session;
            }
            CallSession ended = new CallSession(
                    session.conversationId(),
                    session.customerId(),
                    status,
                    session.startTime(),
                    session.turnCount(),
                    session.lastIntent()
            );
            log.info("[会话管理] 会话 {} 结束，状态={} 共{}轮 时长{}秒",
                    id, status, ended.turnCount().get(), ended.durationSeconds());
            return ended;
        });
    }

    /**
     * 清理超时会话（生产环境用 @Scheduled 定期调用）
     *
     * @return 清理的会话数量
     */
    public int cleanExpiredSessions() {
        long now = Instant.now().getEpochSecond();
        int[] count = {0};
        sessions.entrySet().removeIf(entry -> {
            CallSession s = entry.getValue();
            boolean expired = s.isActive()
                    && (now - s.startTime().getEpochSecond()) > SESSION_TIMEOUT_SECONDS;
            if (expired) {
                count[0]++;
                log.info("[会话管理] 清理超时会话 conversationId={} 时长{}秒",
                        s.conversationId(), s.durationSeconds());
            }
            return expired;
        });
        return count[0];
    }

    /**
     * 获取当前活跃通话数
     */
    public long activeCount() {
        return sessions.values().stream().filter(CallSession::isActive).count();
    }

    /**
     * 获取全部会话统计（用于监控大屏）
     */
    public Map<String, Long> stats() {
        long active = 0, ended = 0, abandoned = 0;
        for (CallSession s : sessions.values()) {
            switch (s.status()) {
                case ACTIVE -> active++;
                case ENDED -> ended++;
                case ABANDONED -> abandoned++;
            }
        }
        return Map.of("active", active, "ended", ended, "abandoned", abandoned, "total", (long) sessions.size());
    }
}
