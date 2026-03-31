import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/candidates' },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'candidates', name: 'Candidates', component: () => import('../views/Candidate.vue') },
      { path: 'interview-plans', name: 'InterviewPlans', component: () => import('../views/InterviewPlan.vue') },
      { path: 'rooms', name: 'Rooms', component: () => import('../views/Room.vue') },
      { path: 'evaluations', name: 'Evaluations', component: () => import('../views/Evaluation.vue') },
      { path: 'exports', name: 'ExportCenter', component: () => import('../views/ExportCenter.vue') },
      { path: 'system', name: 'System', component: () => import('../views/System.vue') },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  auth.bootstrap()

  if ((to.meta as any)?.public) return true
  if ((to.meta as any)?.requiresAuth && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

