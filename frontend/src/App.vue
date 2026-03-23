<template>
  <div class="flex h-screen overflow-hidden" :class="store.darkMode ? 'dark' : ''">
    <!-- Sidebar -->
    <Sidebar />

    <!-- Main content -->
    <div class="flex-1 flex flex-col min-w-0 bg-white dark:bg-gray-900 transition-colors">
      <!-- Header -->
      <header class="flex items-center justify-between px-6 py-3 border-b border-gray-200 dark:border-gray-700 shrink-0">
        <div class="flex items-center gap-2">
          <span class="text-xl font-bold text-gray-800 dark:text-white">🤖 Spring AI Lab</span>
          <span class="text-xs px-2 py-0.5 rounded-full bg-sky-100 text-sky-700 dark:bg-sky-900 dark:text-sky-300 font-medium">
            Spring AI 1.0 学习平台
          </span>
        </div>
        <button
          @click="store.toggleDarkMode()"
          class="p-2 rounded-lg text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 dark:text-gray-400 transition-colors"
          :title="store.darkMode ? '切换浅色模式' : '切换深色模式'"
        >
          {{ store.darkMode ? '☀️' : '🌙' }}
        </button>
      </header>

      <!-- Router view -->
      <main class="flex-1 overflow-hidden">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/Sidebar.vue'
import { useChatStore } from '@/stores/chat'

const store = useChatStore()
</script>
