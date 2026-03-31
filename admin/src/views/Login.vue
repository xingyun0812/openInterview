<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { login } from '../api/auth'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive({
  username: 'admin',
  password: 'admin123',
})
const loading = ref(false)

async function onSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await login({ username: form.username, password: form.password })
    auth.setToken(data.token)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/candidates'
    await router.replace(redirect)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div style="display: grid; place-items: center; height: 100%; padding: 16px">
    <el-card style="width: 420px; max-width: 100%">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <div style="font-weight: 700">面试管理后台</div>
          <el-tag type="info">Vue3</el-tag>
        </div>
      </template>

      <el-form ref="formRef" :model="form" label-position="top" @submit.prevent>
        <el-form-item label="用户名" prop="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="[{ required: true, message: '请输入密码' }]">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登录</el-button>
      </el-form>

      <div class="muted" style="margin-top: 10px; font-size: 12px">
        默认通过开发代理访问后端：`http://localhost:8080`
      </div>
    </el-card>
  </div>
</template>

