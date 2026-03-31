<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  UserFilled,
  Calendar,
  VideoCamera,
  Finished,
  Download,
  Setting,
  Moon,
  Sunny,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const active = computed(() => {
  const p = route.path
  if (p.startsWith('/candidates')) return '/candidates'
  if (p.startsWith('/interview-plans')) return '/interview-plans'
  if (p.startsWith('/rooms')) return '/rooms'
  if (p.startsWith('/evaluations')) return '/evaluations'
  if (p.startsWith('/exports')) return '/exports'
  if (p.startsWith('/system')) return '/system'
  return '/candidates'
})

async function onLogout() {
  try {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
  } catch {
    return
  }
  auth.logout()
  await router.replace('/login')
}
</script>

<template>
  <el-container style="height: 100%">
    <el-aside width="208px" style="border-right: 1px solid var(--el-border-color)">
      <div style="padding: 14px 12px; font-weight: 700">openInterview 管理端</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/candidates">
          <el-icon><UserFilled /></el-icon>
          <span>候选人</span>
        </el-menu-item>
        <el-menu-item index="/interview-plans">
          <el-icon><Calendar /></el-icon>
          <span>面试计划</span>
        </el-menu-item>
        <el-menu-item index="/rooms">
          <el-icon><VideoCamera /></el-icon>
          <span>房间</span>
        </el-menu-item>
        <el-menu-item index="/evaluations">
          <el-icon><Finished /></el-icon>
          <span>评价</span>
        </el-menu-item>
        <el-menu-item index="/exports">
          <el-icon><Download /></el-icon>
          <span>导出中心</span>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Setting /></el-icon>
          <span>系统</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: flex-end; gap: 12px">
        <el-tooltip content="暗色模式" placement="bottom">
          <el-switch
            :model-value="auth.dark"
            inline-prompt
            :active-icon="Moon"
            :inactive-icon="Sunny"
            @update:model-value="auth.setDark"
          />
        </el-tooltip>
        <el-button :icon="SwitchButton" @click="onLogout">退出</el-button>
      </el-header>

      <el-main style="padding: 0">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

