<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { idemKey } from '../utils/idem'
import {
  cancelInterviewPlan,
  completeInterviewPlan,
  createInterviewPlan,
  pageInterviewPlans,
  startInterviewPlan,
  updateInterviewPlan,
  type InterviewPlan,
} from '../api/interviewPlan'

const loading = ref(false)
const page = reactive({ current: 1, size: 10, total: 0, status: undefined as number | undefined })
const rows = ref<InterviewPlan[]>([])

const drawer = reactive({
  open: false,
  mode: 'create' as 'create' | 'edit',
  row: null as InterviewPlan | null,
})

const formRef = ref<FormInstance>()
const form = reactive({
  candidateId: 10001,
  applyPosition: 'Java 高级工程师',
  interviewRound: '一面',
  interviewType: 1,
  templateId: 1,
  interviewStartTime: '',
  interviewEndTime: '',
  interviewRoomId: '',
  interviewRoomLink: '',
  hrUserId: 1,
  interviewerIds: '2,3',
  remark: '',
})

const title = computed(() => (drawer.mode === 'create' ? '新建面试计划' : `编辑面试计划 #${drawer.row?.id}`))

async function fetchList() {
  loading.value = true
  try {
    const res = await pageInterviewPlans(page.current, page.size, page.status)
    rows.value = res.records
    page.total = res.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  drawer.mode = 'create'
  drawer.row = null
  drawer.open = true
  Object.assign(form, {
    candidateId: 10001,
    applyPosition: 'Java 高级工程师',
    interviewRound: '一面',
    interviewType: 1,
    templateId: 1,
    interviewStartTime: '',
    interviewEndTime: '',
    interviewRoomId: '',
    interviewRoomLink: '',
    hrUserId: 1,
    interviewerIds: '2,3',
    remark: '',
  })
}

function openEdit(row: InterviewPlan) {
  drawer.mode = 'edit'
  drawer.row = row
  drawer.open = true
  Object.assign(form, {
    candidateId: row.candidateId,
    applyPosition: row.applyPosition,
    interviewRound: row.interviewRound,
    interviewType: row.interviewType,
    templateId: row.templateId,
    interviewStartTime: row.interviewStartTime,
    interviewEndTime: row.interviewEndTime,
    interviewRoomId: row.interviewRoomId || '',
    interviewRoomLink: row.interviewRoomLink || '',
    hrUserId: row.hrUserId || 1,
    interviewerIds: row.interviewerIds || '',
    remark: row.remark || '',
  })
}

async function onSubmit() {
  await formRef.value?.validate()
  loading.value = true
  try {
    if (drawer.mode === 'create') {
      await createInterviewPlan(
        {
          candidateId: Number(form.candidateId),
          applyPosition: form.applyPosition,
          interviewRound: form.interviewRound,
          interviewType: Number(form.interviewType),
          templateId: Number(form.templateId),
          interviewStartTime: form.interviewStartTime,
          interviewEndTime: form.interviewEndTime,
          interviewRoomId: form.interviewRoomId || undefined,
          interviewRoomLink: form.interviewRoomLink || undefined,
          hrUserId: Number(form.hrUserId),
          interviewerIds: form.interviewerIds || undefined,
        },
        idemKey('plan-create'),
      )
      ElMessage.success('已创建')
    } else if (drawer.row) {
      await updateInterviewPlan(drawer.row.id, {
        applyPosition: form.applyPosition,
        interviewRound: form.interviewRound,
        interviewType: Number(form.interviewType),
        templateId: Number(form.templateId),
        interviewStartTime: form.interviewStartTime,
        interviewEndTime: form.interviewEndTime,
        interviewRoomId: form.interviewRoomId || undefined,
        interviewRoomLink: form.interviewRoomLink || undefined,
        hrUserId: Number(form.hrUserId),
        interviewerIds: form.interviewerIds || undefined,
        remark: form.remark || undefined,
      })
      ElMessage.success('已更新')
    }
    drawer.open = false
    await fetchList()
  } finally {
    loading.value = false
  }
}

async function doStart(row: InterviewPlan) {
  loading.value = true
  try {
    await startInterviewPlan(row.id)
    await fetchList()
  } finally {
    loading.value = false
  }
}
async function doComplete(row: InterviewPlan) {
  loading.value = true
  try {
    await completeInterviewPlan(row.id)
    await fetchList()
  } finally {
    loading.value = false
  }
}
async function doCancel(row: InterviewPlan) {
  loading.value = true
  try {
    await cancelInterviewPlan(row.id)
    await fetchList()
  } finally {
    loading.value = false
  }
}

fetchList()
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">面试计划</div>
        <div class="muted" style="font-size: 12px">对接后端 `/api/interview-plans`（分页 + 新建/更新 + 状态流转）。</div>
      </div>
      <div style="display: flex; gap: 8px; align-items: center">
        <el-select v-model="page.status" clearable placeholder="状态过滤" style="width: 140px" @change="fetchList">
          <el-option label="PENDING(0)" :value="0" />
          <el-option label="IN_PROGRESS(1)" :value="1" />
          <el-option label="COMPLETED(2)" :value="2" />
          <el-option label="CANCELLED(3)" :value="3" />
        </el-select>
        <el-button @click="fetchList">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建</el-button>
      </div>
    </div>

    <el-table :data="rows" border v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="id" width="90" />
      <el-table-column prop="interviewCode" label="code" width="160" show-overflow-tooltip />
      <el-table-column prop="candidateId" label="candidateId" width="120" />
      <el-table-column prop="applyPosition" label="岗位" min-width="160" show-overflow-tooltip />
      <el-table-column prop="interviewRound" label="轮次" width="90" />
      <el-table-column prop="interviewStartTime" label="开始时间" width="180" />
      <el-table-column prop="interviewEndTime" label="结束时间" width="180" />
      <el-table-column prop="interviewStatus" label="状态" width="90" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="doStart(row)">开始</el-button>
          <el-button link type="success" @click="doComplete(row)">完成</el-button>
          <el-button link type="danger" @click="doCancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display: flex; justify-content: flex-end; margin-top: 12px">
      <el-pagination
        background
        layout="prev, pager, next, sizes, total"
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        @change="fetchList"
      />
    </div>

    <el-drawer v-model="drawer.open" :title="title" size="560px">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="candidateId" prop="candidateId" :rules="[{ required: true, message: '必填' }]">
          <el-input-number v-model="form.candidateId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="applyPosition" prop="applyPosition" :rules="[{ required: true, message: '必填' }]">
          <el-input v-model="form.applyPosition" />
        </el-form-item>
        <el-form-item label="interviewRound" prop="interviewRound" :rules="[{ required: true, message: '必填' }]">
          <el-input v-model="form.interviewRound" />
        </el-form-item>
        <el-form-item label="interviewType" prop="interviewType" :rules="[{ required: true, message: '必填' }]">
          <el-input-number v-model="form.interviewType" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="templateId" prop="templateId" :rules="[{ required: true, message: '必填' }]">
          <el-input-number v-model="form.templateId" :min="1" style="width: 100%" />
        </el-form-item>

        <el-form-item label="interviewStartTime" prop="interviewStartTime" :rules="[{ required: true, message: '必填' }]">
          <el-date-picker
            v-model="form.interviewStartTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="interviewEndTime" prop="interviewEndTime" :rules="[{ required: true, message: '必填' }]">
          <el-date-picker
            v-model="form.interviewEndTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="interviewRoomId">
          <el-input v-model="form.interviewRoomId" />
        </el-form-item>
        <el-form-item label="interviewRoomLink">
          <el-input v-model="form.interviewRoomLink" />
        </el-form-item>
        <el-form-item label="hrUserId">
          <el-input-number v-model="form.hrUserId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="interviewerIds（逗号分隔）">
          <el-input v-model="form.interviewerIds" />
        </el-form-item>
        <el-form-item v-if="drawer.mode === 'edit'" label="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>

        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">保存</el-button>
      </el-form>
    </el-drawer>
  </div>
</template>

