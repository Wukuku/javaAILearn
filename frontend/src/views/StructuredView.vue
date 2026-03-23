<template>
  <div class="flex flex-col h-full overflow-hidden">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center gap-3 px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 shrink-0">
      <span class="text-xs text-gray-500 dark:text-gray-400">模式</span>
      <button
        v-for="tab in tabs"
        :key="tab.value"
        @click="activeTab = tab.value; result = null; error = null"
        class="text-xs px-3 py-1 rounded-full transition-colors"
        :class="activeTab === tab.value
          ? 'bg-sky-500 text-white'
          : 'bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 border border-gray-300 dark:border-gray-600 hover:bg-gray-50'"
      >
        {{ tab.icon }} {{ tab.label }}
      </button>
    </div>

    <div class="flex-1 overflow-y-auto">
      <div class="max-w-3xl mx-auto p-6 space-y-6">

        <!-- Book form -->
        <div v-if="activeTab === 'book'" class="space-y-4">
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300">输入书名，AI 返回结构化书籍信息</div>
          <div class="flex gap-3">
            <input
              v-model="bookTitle"
              placeholder="例：三体、活着、百年孤独"
              class="flex-1 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              @keydown.enter="fetchBook"
            />
            <button @click="fetchBook" :disabled="!bookTitle || loading" class="btn-primary">查询</button>
          </div>
          <div class="flex gap-2 flex-wrap">
            <button v-for="b in ['三体', '百年孤独', 'Clean Code', 'Spring in Action']" :key="b"
              @click="bookTitle = b; fetchBook()"
              class="text-xs px-3 py-1 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-sky-50 dark:hover:bg-sky-900/20 hover:text-sky-600 transition-colors"
            >{{ b }}</button>
          </div>
        </div>

        <!-- Movie form -->
        <div v-if="activeTab === 'movies'" class="space-y-4">
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300">输入电影类型，AI 返回结构化推荐列表</div>
          <div class="flex gap-3">
            <input
              v-model="movieGenre"
              placeholder="例：科幻、爱情、悬疑"
              class="flex-1 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
              @keydown.enter="fetchMovies"
            />
            <input v-model.number="movieCount" type="number" min="1" max="5" class="w-16 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500" />
            <button @click="fetchMovies" :disabled="!movieGenre || loading" class="btn-primary">推荐</button>
          </div>
          <div class="flex gap-2 flex-wrap">
            <button v-for="g in ['科幻', '爱情', '悬疑', '动作', '喜剧']" :key="g"
              @click="movieGenre = g; fetchMovies()"
              class="text-xs px-3 py-1 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-sky-50 dark:hover:bg-sky-900/20 hover:text-sky-600 transition-colors"
            >{{ g }}</button>
          </div>
        </div>

        <!-- Resume form -->
        <div v-if="activeTab === 'resume'" class="space-y-4">
          <div class="text-sm font-medium text-gray-700 dark:text-gray-300">粘贴简历文本，AI 解析成结构化数据</div>
          <textarea
            v-model="resumeText"
            placeholder="粘贴简历内容..."
            rows="8"
            class="w-full rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 resize-none"
          />
          <div class="flex gap-3">
            <button @click="loadSampleResume" class="btn-outline text-xs">加载示例简历</button>
            <button @click="fetchResume" :disabled="!resumeText || loading" class="btn-primary">解析简历</button>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="flex items-center gap-3 text-sm text-gray-500 dark:text-gray-400">
          <svg class="animate-spin w-5 h-5 text-sky-500" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
          </svg>
          AI 正在生成结构化数据...
        </div>

        <!-- Error -->
        <div v-if="error" class="px-3 py-2 rounded-lg bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 text-sm">
          ⚠️ {{ error }}
        </div>

        <!-- Result: Book -->
        <div v-if="result && activeTab === 'book'" class="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 overflow-hidden">
          <div class="px-4 py-3 bg-sky-50 dark:bg-sky-900/20 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <span class="text-sm font-semibold text-sky-700 dark:text-sky-300">📖 书籍信息</span>
            <button @click="copyJson" class="text-xs text-gray-400 hover:text-gray-600">复制 JSON</button>
          </div>
          <div class="p-4 space-y-2 text-sm">
            <div class="grid grid-cols-2 gap-x-4 gap-y-2">
              <span class="text-gray-500 dark:text-gray-400">书名</span><span class="font-medium text-gray-800 dark:text-white">{{ (result as BookInfo).title }}</span>
              <span class="text-gray-500 dark:text-gray-400">作者</span><span class="text-gray-800 dark:text-white">{{ (result as BookInfo).author }}</span>
              <span class="text-gray-500 dark:text-gray-400">年份</span><span class="text-gray-800 dark:text-white">{{ (result as BookInfo).year }}</span>
              <span class="text-gray-500 dark:text-gray-400">类型</span><span class="text-gray-800 dark:text-white">{{ (result as BookInfo).genre }}</span>
              <span class="text-gray-500 dark:text-gray-400">评分</span><span class="text-gray-800 dark:text-white">⭐ {{ (result as BookInfo).rating }}</span>
            </div>
            <div class="mt-3 pt-3 border-t border-gray-100 dark:border-gray-700">
              <div class="text-gray-500 dark:text-gray-400 text-xs mb-1">简介</div>
              <div class="text-gray-700 dark:text-gray-300">{{ (result as BookInfo).summary }}</div>
            </div>
          </div>
          <pre class="px-4 pb-4 text-xs text-gray-400 dark:text-gray-600 overflow-x-auto">{{ JSON.stringify(result, null, 2) }}</pre>
        </div>

        <!-- Result: Movies -->
        <div v-if="result && activeTab === 'movies'" class="space-y-3">
          <div class="text-sm font-semibold text-gray-700 dark:text-gray-300">🎬 推荐电影</div>
          <div v-for="(movie, i) in (result as MovieInfo[])" :key="i"
            class="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 p-4 text-sm"
          >
            <div class="flex items-center justify-between mb-2">
              <span class="font-semibold text-gray-800 dark:text-white">{{ movie.title }}</span>
              <span class="text-xs text-amber-500">⭐ {{ movie.rating }}</span>
            </div>
            <div class="text-xs text-gray-500 dark:text-gray-400 mb-2">{{ movie.director }} · {{ movie.year }}</div>
            <div class="text-gray-700 dark:text-gray-300 text-xs">{{ movie.summary }}</div>
          </div>
          <pre class="text-xs text-gray-400 dark:text-gray-600 overflow-x-auto p-3 bg-gray-50 dark:bg-gray-800 rounded-xl">{{ JSON.stringify(result, null, 2) }}</pre>
        </div>

        <!-- Result: Resume -->
        <div v-if="result && activeTab === 'resume'" class="rounded-xl border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 overflow-hidden">
          <div class="px-4 py-3 bg-purple-50 dark:bg-purple-900/20 border-b border-gray-200 dark:border-gray-700">
            <span class="text-sm font-semibold text-purple-700 dark:text-purple-300">👤 简历解析结果</span>
          </div>
          <div class="p-4 space-y-4 text-sm">
            <div>
              <div class="font-semibold text-lg text-gray-800 dark:text-white">{{ (result as ResumeInfo).name }}</div>
              <div class="text-gray-500 dark:text-gray-400 text-xs mt-0.5">
                {{ (result as ResumeInfo).email }} · {{ (result as ResumeInfo).phone }}
              </div>
            </div>
            <div v-if="(result as ResumeInfo).summary">
              <div class="text-xs text-gray-500 mb-1 font-medium">个人简介</div>
              <div class="text-gray-700 dark:text-gray-300">{{ (result as ResumeInfo).summary }}</div>
            </div>
            <div v-if="(result as ResumeInfo).skills?.length">
              <div class="text-xs text-gray-500 mb-1 font-medium">技能</div>
              <div class="flex flex-wrap gap-1">
                <span v-for="s in (result as ResumeInfo).skills" :key="s"
                  class="text-xs px-2 py-0.5 rounded-full bg-sky-100 dark:bg-sky-900/30 text-sky-700 dark:text-sky-400">
                  {{ s }}
                </span>
              </div>
            </div>
            <div v-if="(result as ResumeInfo).experience?.length">
              <div class="text-xs text-gray-500 mb-2 font-medium">工作经历</div>
              <div v-for="(e, i) in (result as ResumeInfo).experience" :key="i" class="mb-2 pl-3 border-l-2 border-sky-200 dark:border-sky-700">
                <div class="font-medium text-gray-800 dark:text-white">{{ e.position }} @ {{ e.company }}</div>
                <div class="text-xs text-gray-400">{{ e.duration }}</div>
                <div v-if="e.description" class="text-xs text-gray-600 dark:text-gray-400 mt-0.5">{{ e.description }}</div>
              </div>
            </div>
            <div v-if="(result as ResumeInfo).education?.length">
              <div class="text-xs text-gray-500 mb-2 font-medium">教育背景</div>
              <div v-for="(e, i) in (result as ResumeInfo).education" :key="i" class="mb-1 text-gray-700 dark:text-gray-300 text-xs">
                {{ e.school }} · {{ e.degree }} {{ e.major }} {{ e.year }}
              </div>
            </div>
          </div>
          <pre class="px-4 pb-4 text-xs text-gray-400 dark:text-gray-600 overflow-x-auto">{{ JSON.stringify(result, null, 2) }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { structuredApi, type BookInfo, type MovieInfo, type ResumeInfo } from '@/api/structured'

const activeTab = ref<'book' | 'movies' | 'resume'>('book')
const loading = ref(false)
const error = ref<string | null>(null)
const result = ref<unknown>(null)

const bookTitle = ref('')
const movieGenre = ref('')
const movieCount = ref(3)
const resumeText = ref('')

const tabs = [
  { value: 'book', icon: '📖', label: '书籍信息' },
  { value: 'movies', icon: '🎬', label: '电影推荐' },
  { value: 'resume', icon: '👤', label: '简历解析' },
]

async function fetchBook() {
  if (!bookTitle.value) return
  loading.value = true; error.value = null; result.value = null
  try {
    const res = await structuredApi.book(bookTitle.value)
    result.value = res.data
  } catch (e: unknown) { error.value = e instanceof Error ? e.message : String(e) }
  finally { loading.value = false }
}

async function fetchMovies() {
  if (!movieGenre.value) return
  loading.value = true; error.value = null; result.value = null
  try {
    const res = await structuredApi.movies(movieGenre.value, movieCount.value)
    result.value = res.data
  } catch (e: unknown) { error.value = e instanceof Error ? e.message : String(e) }
  finally { loading.value = false }
}

async function fetchResume() {
  if (!resumeText.value) return
  loading.value = true; error.value = null; result.value = null
  try {
    const res = await structuredApi.resume(resumeText.value)
    result.value = res.data
  } catch (e: unknown) { error.value = e instanceof Error ? e.message : String(e) }
  finally { loading.value = false }
}

function loadSampleResume() {
  resumeText.value = `张三，男，28岁
手机：138-0000-0000  邮箱：zhangsan@example.com

求职意向：Java 高级工程师

工作经历：
2021.06 - 至今  ABC科技有限公司  Java开发工程师
- 负责后端微服务架构设计和开发
- 参与 Spring Boot + Spring Cloud 技术栈落地
- 完成日活百万级订单系统性能优化

2019.07 - 2021.05  XYZ互联网公司  Java初级工程师
- 参与电商平台后端开发
- 编写单元测试，代码覆盖率提升至80%

教育背景：
2015.09 - 2019.06  北京邮电大学  本科  计算机科学与技术

技能：Java、Spring Boot、Spring Cloud、MySQL、Redis、Kafka、Docker、K8s`
}

function copyJson() {
  navigator.clipboard.writeText(JSON.stringify(result.value, null, 2))
}
</script>

<style scoped>
.btn-primary {
  @apply px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-600 text-white text-sm disabled:opacity-50 disabled:cursor-not-allowed transition-colors;
}
.btn-outline {
  @apply px-3 py-1.5 rounded-xl border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors;
}
</style>
