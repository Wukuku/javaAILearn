<template>
  <nav class="w-56 shrink-0 flex flex-col bg-gray-900 dark:bg-gray-950 text-white">
    <!-- Logo area -->
    <div class="px-4 py-4 border-b border-gray-700">
      <div class="text-sm font-semibold text-gray-400 uppercase tracking-wider">模块导航</div>
    </div>

    <!-- Nav items -->
    <div class="flex-1 py-2 overflow-y-auto">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="flex items-center gap-3 px-4 py-3 text-sm text-gray-300 hover:bg-gray-700 hover:text-white transition-colors rounded-none"
        active-class="bg-sky-700 text-white hover:bg-sky-600"
      >
        <span class="text-lg leading-none">{{ item.icon }}</span>
        <div>
          <div class="font-medium">{{ item.title }}</div>
          <div class="text-xs text-gray-400 mt-0.5" v-if="item.subtitle">{{ item.subtitle }}</div>
        </div>
      </router-link>
    </div>

    <!-- Footer -->
    <div class="px-4 py-3 border-t border-gray-700 text-xs text-gray-500">
      <div>Spring AI 1.0.0-M6</div>
      <div>Spring Boot 3.4.3</div>
    </div>
  </nav>
</template>

<script setup lang="ts">
const navItems = [
  { path: '/chat', icon: '💬', title: 'Chat', subtitle: '基础聊天 / 流式' },
  { path: '/memory', icon: '🧠', title: 'Memory', subtitle: '多轮对话记忆' },
  { path: '/structured', icon: '📦', title: '结构化输出', subtitle: 'BeanOutputConverter' },
  { path: '/tools', icon: '🔧', title: 'Function Calling', subtitle: '工具调用' },
  { path: '/rag', icon: '📚', title: 'RAG', subtitle: '检索增强生成' },
  { path: '/agent', icon: '🤖', title: 'Agent', subtitle: '自主推理规划' },
]
</script>
