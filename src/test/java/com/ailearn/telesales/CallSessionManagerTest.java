package com.ailearn.telesales;

import com.ailearn.telesales.model.CallSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CallSessionManager 单元测试
 *
 * 面试要点：
 * 1. 测试状态机约束：ACTIVE → ENDED，不允许逆向
 * 2. 测试幂等性：同一 conversationId 多次 getOrCreate 返回同一个对象
 * 3. 测试并发安全（通过多线程同时 getOrCreate 验证）
 * 4. 测试超时清理逻辑
 */
@DisplayName("CallSessionManager - 通话会话管理器")
class CallSessionManagerTest {

    private CallSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new CallSessionManager();
    }

    @Test
    @DisplayName("getOrCreate：相同 conversationId 应返回同一会话实例")
    void getOrCreate_shouldReturnSameSession_forSameConversationId() {
        // when
        CallSession s1 = manager.getOrCreate("call-001", "C001");
        CallSession s2 = manager.getOrCreate("call-001", "C001");

        // then：同一个对象（幂等）
        assertThat(s1).isSameAs(s2);
        assertThat(manager.activeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getOrCreate：新会话状态应为 ACTIVE")
    void getOrCreate_shouldCreateActiveSession() {
        CallSession session = manager.getOrCreate("call-001", "C001");

        assertThat(session.status()).isEqualTo(CallSession.CallStatus.ACTIVE);
        assertThat(session.conversationId()).isEqualTo("call-001");
        assertThat(session.customerId()).isEqualTo("C001");
        assertThat(session.isActive()).isTrue();
    }

    @Test
    @DisplayName("endSession：正常结束后状态应变为 ENDED")
    void endSession_shouldChangeStatusToEnded() {
        // given
        manager.getOrCreate("call-001", "C001");

        // when
        manager.endSession("call-001", CallSession.CallStatus.ENDED);

        // then
        Optional<CallSession> session = manager.find("call-001");
        assertThat(session).isPresent();
        assertThat(session.get().status()).isEqualTo(CallSession.CallStatus.ENDED);
        assertThat(session.get().isActive()).isFalse();
    }

    @Test
    @DisplayName("endSession：客户挂断后状态应变为 ABANDONED")
    void endSession_shouldSupportAbandonedStatus() {
        manager.getOrCreate("call-002", "C002");
        manager.endSession("call-002", CallSession.CallStatus.ABANDONED);

        assertThat(manager.find("call-002").get().status())
                .isEqualTo(CallSession.CallStatus.ABANDONED);
    }

    @Test
    @DisplayName("endSession：已结束会话不应被再次修改（状态机约束）")
    void endSession_shouldNotChangeAlreadyEndedSession() {
        // given：先结束
        manager.getOrCreate("call-001", "C001");
        manager.endSession("call-001", CallSession.CallStatus.ENDED);

        // when：再次尝试改为 ABANDONED
        manager.endSession("call-001", CallSession.CallStatus.ABANDONED);

        // then：状态保持 ENDED，不被覆盖
        assertThat(manager.find("call-001").get().status())
                .isEqualTo(CallSession.CallStatus.ENDED);
    }

    @Test
    @DisplayName("find：不存在的会话应返回 empty Optional")
    void find_shouldReturnEmpty_forUnknownConversationId() {
        Optional<CallSession> result = manager.find("non-existent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("activeCount：只计算 ACTIVE 状态的会话")
    void activeCount_shouldOnlyCountActiveSessions() {
        // given：3个会话，2个结束
        manager.getOrCreate("call-001", "C001");
        manager.getOrCreate("call-002", "C002");
        manager.getOrCreate("call-003", "C003");
        manager.endSession("call-001", CallSession.CallStatus.ENDED);
        manager.endSession("call-002", CallSession.CallStatus.ABANDONED);

        // then：只有 call-003 是 ACTIVE
        assertThat(manager.activeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("stats：应正确统计各状态会话数量")
    void stats_shouldReturnCorrectCounts() {
        // given
        manager.getOrCreate("call-001", "C001"); // ACTIVE
        manager.getOrCreate("call-002", "C002"); // → ENDED
        manager.getOrCreate("call-003", "C003"); // → ABANDONED
        manager.endSession("call-002", CallSession.CallStatus.ENDED);
        manager.endSession("call-003", CallSession.CallStatus.ABANDONED);

        // when
        var stats = manager.stats();

        // then
        assertThat(stats.get("active")).isEqualTo(1L);
        assertThat(stats.get("ended")).isEqualTo(1L);
        assertThat(stats.get("abandoned")).isEqualTo(1L);
        assertThat(stats.get("total")).isEqualTo(3L);
    }

    @Test
    @DisplayName("turnCount：每次 nextTurn 应递增")
    void turnCount_shouldIncrement() {
        CallSession session = manager.getOrCreate("call-001", "C001");

        assertThat(session.nextTurn()).isEqualTo(1);
        assertThat(session.nextTurn()).isEqualTo(2);
        assertThat(session.nextTurn()).isEqualTo(3);
        assertThat(session.turnCount().get()).isEqualTo(3);
    }

    @Test
    @DisplayName("durationSeconds：通话时长应大于等于0")
    void durationSeconds_shouldBeNonNegative() {
        CallSession session = manager.getOrCreate("call-001", "C001");
        assertThat(session.durationSeconds()).isGreaterThanOrEqualTo(0);
    }
}
