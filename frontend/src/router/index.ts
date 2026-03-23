import { createRouter, createWebHashHistory } from 'vue-router'
import ChatView from '@/views/ChatView.vue'
import MemoryView from '@/views/MemoryView.vue'
import StructuredView from '@/views/StructuredView.vue'
import ToolsView from '@/views/ToolsView.vue'
import RagView from '@/views/RagView.vue'
import AgentView from '@/views/AgentView.vue'

const routes = [
  { path: '/', redirect: '/chat' },
  { path: '/chat', component: ChatView, meta: { title: 'Chat', icon: '💬' } },
  { path: '/memory', component: MemoryView, meta: { title: 'Memory', icon: '🧠' } },
  { path: '/structured', component: StructuredView, meta: { title: '结构化输出', icon: '📦' } },
  { path: '/tools', component: ToolsView, meta: { title: 'Function Calling', icon: '🔧' } },
  { path: '/rag', component: RagView, meta: { title: 'RAG', icon: '📚' } },
  { path: '/agent', component: AgentView, meta: { title: 'Agent', icon: '🤖' } },
]

export default createRouter({
  history: createWebHashHistory(),
  routes,
})
