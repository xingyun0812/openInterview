<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, type UploadFile } from 'element-plus'
import { idemKey } from '../utils/idem'
import {
  getParseResult,
  getScreenResult,
  parseResume,
  reviewScreen,
  screenResume,
  uploadResume,
} from '../api/candidateResume'

type CandidateRow = {
  candidateId: number
  name: string
  jobCode: string
  resumeUrl?: string
}

const rows = ref<CandidateRow[]>([
  { candidateId: 10001, name: '候选人A', jobCode: 'JAVA_ADV_01' },
  { candidateId: 10002, name: '候选人B', jobCode: 'JAVA_ADV_01' },
])

const working = ref(false)
const drawer = reactive({
  open: false,
  row: null as CandidateRow | null,
})

const uploadFile = ref<File | null>(null)
const parseOut = ref<any>(null)
const screenOut = ref<any>(null)
const reviewForm = reactive({
  reviewResult: 1,
  reviewComment: '通过（示例）',
})

const title = computed(() => (drawer.row ? `候选人：${drawer.row.name}（${drawer.row.candidateId}）` : '候选人'))

function openRow(row: CandidateRow) {
  drawer.row = row
  drawer.open = true
  uploadFile.value = null
  parseOut.value = null
  screenOut.value = null
}

async function onPick(file: UploadFile) {
  uploadFile.value = file.raw ?? null
  return false
}

async function doUpload() {
  if (!drawer.row) return
  if (!uploadFile.value) {
    ElMessage.warning('请先选择简历文件')
    return
  }
  working.value = true
  try {
    const res = await uploadResume(drawer.row.candidateId, uploadFile.value, idemKey('resume-upload'))
    drawer.row.resumeUrl = res.resumeUrl
    ElMessage.success('上传成功')
  } finally {
    working.value = false
  }
}

async function doParse() {
  if (!drawer.row?.resumeUrl) {
    ElMessage.warning('请先上传简历')
    return
  }
  working.value = true
  try {
    await parseResume(drawer.row.candidateId, drawer.row.resumeUrl, idemKey('resume-parse'))
    ElMessage.success('已触发解析（异步）')
  } finally {
    working.value = false
  }
}

async function refreshParse() {
  if (!drawer.row) return
  working.value = true
  try {
    parseOut.value = await getParseResult(drawer.row.candidateId)
  } finally {
    working.value = false
  }
}

async function doScreen() {
  if (!drawer.row) return
  working.value = true
  try {
    await screenResume(drawer.row.candidateId, drawer.row.jobCode, idemKey('resume-screen'))
    ElMessage.success('已触发筛选')
  } finally {
    working.value = false
  }
}

async function refreshScreen() {
  if (!drawer.row) return
  working.value = true
  try {
    screenOut.value = await getScreenResult(drawer.row.candidateId, drawer.row.jobCode)
  } finally {
    working.value = false
  }
}

async function doReview() {
  if (!drawer.row) return
  working.value = true
  try {
    await reviewScreen(
      drawer.row.candidateId,
      drawer.row.jobCode,
      reviewForm.reviewResult,
      reviewForm.reviewComment,
      idemKey('resume-review'),
    )
    ElMessage.success('已提交复核')
    await refreshScreen()
  } finally {
    working.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">候选人</div>
        <div class="muted" style="font-size: 12px">候选人列表暂用 mock；简历链路对接后端 `/api/v1/candidate/resume/*`。</div>
      </div>
    </div>

    <el-table :data="rows" border style="width: 100%">
      <el-table-column prop="candidateId" label="candidateId" width="120" />
      <el-table-column prop="name" label="姓名" width="160" />
      <el-table-column prop="jobCode" label="岗位 code" width="160" />
      <el-table-column prop="resumeUrl" label="resumeUrl" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="openRow(row)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawer.open" :title="title" size="520px">
      <el-alert type="info" show-icon :closable="false">
        <template #default>
          这是“最小链路”操作台：上传 → 解析 → 筛选 → 复核。后端是幂等接口，需要 `X-Idempotency-Key`（前端自动生成）。
        </template>
      </el-alert>

      <div style="margin-top: 12px; display: grid; gap: 12px">
        <el-card>
          <template #header>1) 上传简历</template>
          <div style="display: grid; gap: 10px">
            <el-upload :auto-upload="false" :show-file-list="true" :before-upload="onPick">
              <el-button>选择文件</el-button>
              <template #tip>
                <div class="muted" style="font-size: 12px">字段：candidateId + resumeFile</div>
              </template>
            </el-upload>
            <el-button type="primary" :loading="working" @click="doUpload">上传</el-button>
          </div>
        </el-card>

        <el-card>
          <template #header>2) 解析简历</template>
          <div style="display: grid; gap: 10px">
            <el-button :loading="working" @click="doParse">触发解析</el-button>
            <el-button :loading="working" @click="refreshParse">刷新解析结果</el-button>
            <el-input
              v-if="drawer.row?.resumeUrl"
              :model-value="drawer.row.resumeUrl"
              readonly
              type="textarea"
              :rows="2"
            />
            <el-input v-if="parseOut" :model-value="JSON.stringify(parseOut, null, 2)" readonly type="textarea" :rows="8" />
          </div>
        </el-card>

        <el-card>
          <template #header>3) 筛选 + 复核</template>
          <div style="display: grid; gap: 10px">
            <el-button type="primary" :loading="working" @click="doScreen">触发筛选</el-button>
            <el-button :loading="working" @click="refreshScreen">刷新筛选结果</el-button>
            <el-input v-if="screenOut" :model-value="JSON.stringify(screenOut, null, 2)" readonly type="textarea" :rows="6" />

            <el-divider />
            <el-form :model="reviewForm" label-position="top">
              <el-form-item label="reviewResult（1 通过 / 0 拒绝）">
                <el-radio-group v-model="reviewForm.reviewResult">
                  <el-radio :value="1">通过</el-radio>
                  <el-radio :value="0">拒绝</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="reviewComment">
                <el-input v-model="reviewForm.reviewComment" type="textarea" :rows="3" />
              </el-form-item>
              <el-button type="success" :loading="working" @click="doReview">提交复核</el-button>
            </el-form>
          </div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

