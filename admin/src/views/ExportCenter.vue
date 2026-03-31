<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createUnifiedExportTask, getExportTask, retryExportTask, type ExportTask } from '../api/export'
import { idemKey } from '../utils/idem'

const loading = ref(false)
const tasks = ref<ExportTask[]>([])

const form = reactive({
  exportType: 0 as 0 | 1 | 2,
  candidateIds: '10001,10002',
  jobCode: 'JAVA_ADV_01',
  interviewIds: '1,2',
})

const canCreate = computed(() => {
  if (form.exportType === 0) return form.candidateIds.trim() && form.jobCode.trim()
  return form.interviewIds.trim()
})

function parseIds(s: string) {
  return s
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
    .map((x) => Number(x))
    .filter((n) => Number.isFinite(n))
}

async function createTask() {
  if (!canCreate.value) {
    ElMessage.warning('请补全参数')
    return
  }
  loading.value = true
  try {
    let task: ExportTask
    if (form.exportType === 0) {
      task = await createUnifiedExportTask(
        { exportType: 0, candidateIds: parseIds(form.candidateIds), jobCode: form.jobCode.trim() },
        idemKey('export-create'),
      )
    } else {
      task = await createUnifiedExportTask({ exportType: form.exportType, interviewIds: parseIds(form.interviewIds) }, idemKey('export-create'))
    }
    tasks.value = [task, ...tasks.value]
    ElMessage.success('已创建导出任务')
  } finally {
    loading.value = false
  }
}

async function refreshTask(taskId: number) {
  loading.value = true
  try {
    const latest = await getExportTask(taskId)
    tasks.value = tasks.value.map((t) => (t.taskId === taskId ? { ...t, ...latest } : t))
  } finally {
    loading.value = false
  }
}

async function retryTask(taskId: number) {
  loading.value = true
  try {
    await retryExportTask(taskId, idemKey('export-retry'))
    ElMessage.success('已触发重试')
    await refreshTask(taskId)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">导出中心</div>
        <div class="muted" style="font-size: 12px">对接 `/api/v1/export/task` 创建任务，`/api/v1/export/task/{id}` 查询状态。</div>
      </div>
    </div>

    <el-card>
      <el-form label-position="top">
        <el-form-item label="exportType">
          <el-radio-group v-model="form.exportType">
            <el-radio :value="0">筛选 Excel</el-radio>
            <el-radio :value="1">面试 Excel</el-radio>
            <el-radio :value="2">面试 Word</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.exportType === 0" label="candidateIds（逗号分隔）">
          <el-input v-model="form.candidateIds" />
        </el-form-item>
        <el-form-item v-if="form.exportType === 0" label="jobCode">
          <el-input v-model="form.jobCode" />
        </el-form-item>

        <el-form-item v-else label="interviewIds（逗号分隔）">
          <el-input v-model="form.interviewIds" />
        </el-form-item>

        <el-button type="primary" :loading="loading" :disabled="!canCreate" @click="createTask">创建导出任务</el-button>
      </el-form>
    </el-card>

    <div style="height: 12px" />

    <el-table :data="tasks" border v-loading="loading" style="width: 100%">
      <el-table-column prop="taskId" label="taskId" width="120" />
      <el-table-column prop="taskStatusLabel" label="状态" width="120" />
      <el-table-column prop="exportTypeLabel" label="类型" width="120" />
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="failReason" label="失败原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="下载" width="220">
        <template #default="{ row }">
          <el-link v-if="row.fileUrl" :href="row.fileUrl" target="_blank">下载</el-link>
          <span v-else class="muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link @click="refreshTask(row.taskId)">刷新</el-button>
          <el-button link type="warning" @click="retryTask(row.taskId)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="tasks.length === 0" description="暂无任务，先创建一个试试" />
  </div>
</template>

