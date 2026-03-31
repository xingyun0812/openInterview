<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getInterviewPlanById } from '../api/interviewPlan'

const interviewId = ref<number>(1)
const loading = ref(false)
const plan = ref<any>(null)

async function fetchRoom() {
  loading.value = true
  try {
    plan.value = await getInterviewPlanById(interviewId.value)
  } catch {
    ElMessage.error('查询失败（确认后端已启动且该 id 存在）')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">房间</div>
        <div class="muted" style="font-size: 12px">最小实现：通过 interviewId 查询计划，展示 roomId/roomLink。</div>
      </div>
    </div>

    <el-card>
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <div class="muted">interviewId：</div>
        <el-input-number v-model="interviewId" :min="1" />
        <el-button type="primary" :loading="loading" @click="fetchRoom">查询</el-button>
      </div>

      <el-divider />
      <el-descriptions v-if="plan" :column="1" border>
        <el-descriptions-item label="interviewCode">{{ plan.interviewCode }}</el-descriptions-item>
        <el-descriptions-item label="candidateId">{{ plan.candidateId }}</el-descriptions-item>
        <el-descriptions-item label="interviewStatus">{{ plan.interviewStatus }}</el-descriptions-item>
        <el-descriptions-item label="interviewRoomId">{{ plan.interviewRoomId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="interviewRoomLink">
          <el-link v-if="plan.interviewRoomLink" :href="plan.interviewRoomLink" target="_blank">{{ plan.interviewRoomLink }}</el-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="请输入 interviewId 查询" />
    </el-card>
  </div>
</template>

