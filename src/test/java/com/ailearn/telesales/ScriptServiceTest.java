package com.ailearn.telesales;

import com.ailearn.telesales.model.ScriptMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ScriptService 单元测试
 *
 * 面试要点：
 * 1. 使用 Mockito 隔离 VectorStore，不依赖真实向量数据库
 * 2. ArgumentCaptor 验证存入向量库的元数据是否正确
 * 3. 测试边界条件：空结果、null intent、score 为 null 的文档
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScriptService - 话术知识库服务")
class ScriptServiceTest {

    @Mock
    private VectorStore vectorStore;

    private ScriptService scriptService;

    @BeforeEach
    void setUp() {
        scriptService = new ScriptService(vectorStore);
    }

    // ===================== addScript =====================

    @Test
    @DisplayName("addScript：应将话术以正确元数据存入向量库")
    void addScript_shouldStoreDocumentWithCorrectMetadata() {
        // given
        String content = "您关注利率完全正常，我们年化利率7.2%-18%，先帮您做个预审？";
        String intent = "PRICE_INQUIRY";
        String segment = "INDIVIDUAL";

        // when
        scriptService.addScript(content, intent, segment);

        // then：验证 vectorStore.add() 被调用，且文档元数据正确
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> docs = captor.getValue();
        assertThat(docs).hasSize(1);

        Document doc = docs.get(0);
        assertThat(doc.getText()).isEqualTo(content);
        assertThat(doc.getMetadata()).containsEntry("type", "telesales_script");
        assertThat(doc.getMetadata()).containsEntry("intent", "PRICE_INQUIRY");
        assertThat(doc.getMetadata()).containsEntry("segment", "INDIVIDUAL");
    }

    // ===================== recommend =====================

    @Test
    @DisplayName("recommend：向量库无结果时应返回空列表")
    void recommend_shouldReturnEmptyList_whenNoResults() {
        // given
        when(vectorStore.similaritySearch(any())).thenReturn(List.of());

        // when
        List<ScriptMatch> result = scriptService.recommend("利率太高了", "PRICE_INQUIRY", 3);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("recommend：应将 Document 正确映射为 ScriptMatch")
    void recommend_shouldMapDocumentsToScriptMatch() {
        // given
        Document doc = new Document(
                "您关注利率这个完全正常，我先帮您预审？",
                Map.of("type", "telesales_script", "intent", "PRICE_INQUIRY", "segment", "ALL")
        );
        // 模拟 score（需通过反射或使用带 score 的构造，这里用 spy 模拟）
        Document spyDoc = spy(doc);
        doReturn(0.85).when(spyDoc).getScore();

        when(vectorStore.similaritySearch(any())).thenReturn(List.of(spyDoc));

        // when
        List<ScriptMatch> result = scriptService.recommend("利率有点高", "PRICE_INQUIRY", 3);

        // then
        assertThat(result).hasSize(1);
        ScriptMatch match = result.get(0);
        assertThat(match.content()).contains("预审");
        assertThat(match.intent()).isEqualTo("PRICE_INQUIRY");
        assertThat(match.segment()).isEqualTo("ALL");
        assertThat(match.score()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("recommend：score 为 null 时应默认为 0.0")
    void recommend_shouldDefaultScoreToZero_whenScoreIsNull() {
        // given
        Document doc = new Document("话术内容", Map.of(
                "type", "telesales_script", "intent", "HESITATION", "segment", "ALL"));
        // score 默认为 null
        when(vectorStore.similaritySearch(any())).thenReturn(List.of(doc));

        // when
        List<ScriptMatch> result = scriptService.recommend("我再想想", "HESITATION", 1);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("recommend：intent 为 null 时应使用仅 type 的过滤条件")
    void recommend_shouldUseTypeOnlyFilter_whenIntentIsNull() {
        // given
        when(vectorStore.similaritySearch(any())).thenReturn(List.of());

        // when（不抛异常即通过）
        List<ScriptMatch> result = scriptService.recommend("随便问问", null, 3);

        // then
        assertThat(result).isEmpty();
        verify(vectorStore, times(1)).similaritySearch(any());
    }

    @Test
    @DisplayName("recommend：intent 为空字符串时应使用仅 type 的过滤条件")
    void recommend_shouldUseTypeOnlyFilter_whenIntentIsBlank() {
        when(vectorStore.similaritySearch(any())).thenReturn(List.of());

        List<ScriptMatch> result = scriptService.recommend("随便问问", "  ", 3);

        assertThat(result).isEmpty();
    }

    // ===================== initDefaultScripts =====================

    @Test
    @DisplayName("initDefaultScripts：应返回 15 并调用 vectorStore 15 次")
    void initDefaultScripts_shouldAddFifteenScripts() {
        // when
        int count = scriptService.initDefaultScripts();

        // then
        assertThat(count).isEqualTo(15);
        verify(vectorStore, times(15)).add(any());
    }
}
