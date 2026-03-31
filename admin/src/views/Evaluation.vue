<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { draftEvaluation, getEvaluationSummary, listEvaluations, reviewEvaluation, submitEvaluation } from '../api/evaluation'

const interviewId = ref<number>(1)
const loading = ref(false)
const summary = ref<any>(null)
const list = ref<any[]>([])

const draft = reactive({
  interviewerId: 2,
  interviewerName: '面试官A',
  totalScore: 80,
  interviewResult: 1,
  advantageComment: '基础扎实',
  disadvantageComment: '系统设计需加强',
  comprehensiveComment: '总体可推进下一轮',
  detailsJson: JSON.stringify(
    [
      { itemId: 1, itemName: '基础', itemFullScore: 100, score: 80, itemComment: 'OK', aiScorePreview: 82 },
    ],
    null,
    2,
  ),
})

const review = reactive({
  interviewResult: 1,
  remark: '通过（示例）',
})

async function refreshAll() {
  loading.value = true
  try {
    summary.value = await getEvaluationSummary(interviewId.value)
    const res = await listEvaluations(interviewId.value)
    list.value = res.evaluations || []
  } finally {
    loading.value = false
  }
}

async function doDraft() {
  loading.value = true
  try {
    const details = JSON.parse(draft.detailsJson)
    await draftEvaluation({
      interviewId: interviewId.value,
      interviewerId: Number(draft.interviewerId),
      interviewerName: draft.interviewerName,
      totalScore: Number(draft.totalScore),
      interviewResult: Number(draft.interviewResult),
      advantageComment: draft.advantageComment,
      disadvantageComment: draft.disadvantageComment,
      comprehensiveComment: draft.comprehensiveComment,
      details,
    })
    ElMessage.success('已保存草稿')
    await refreshAll()
  } catch {
    ElMessage.error('草稿提交失败（检查 detailsJson 格式）')
  } finally {
    loading.value = false
  }
}

async function doSubmit() {
  loading.value = true
  try {
    await submitEvaluation({ interviewId: interviewId.value, interviewerId: Number(draft.interviewerId) })
    ElMessage.success('已提交')
    await refreshAll()
  } finally {
    loading.value = false
  }
}

async function doReview() {
  loading.value = true
  try {
    await reviewEvaluation({ interviewId: interviewId.value, interviewResult: Number(review.interviewResult), remark: review.remark })
    ElMessage.success('已复核')
    await refreshAll()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">评价</div>
        <div class="muted" style="font-size: 12px">对接后端 `/api/v2/evaluations/*`（summary/list/draft/submit/review）。</div>
      </div>
    </div>

    <el-card>
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <div class="muted">interviewId：</div>
        <el-input-number v-model="interviewId" :min="1" />
        <el-button type="primary" :loading="loading" @click="refreshAll">刷新</el-button>
      </div>

      <el-divider />
      <el-descriptions v-if="summary" :column="2" border>
        <el-descriptions-item label="finalScore">{{ summary.finalScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="humanAvgScore">{{ summary.humanAvgScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="submittedEvaluateCount">{{ summary.submittedEvaluateCount }}</el-descriptions-item>
        <el-descriptions-item label="totalEvaluateCount">{{ summary.totalEvaluateCount }}</el-descriptions-item>
        <el-descriptions-item label="flowStatus" :span="2">{{ summary.flowStatus }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="请输入 interviewId 刷新" />
    </el-card>

    <div style="height: 12px" />

    <el-row :gutter="12">
      <el-col :span="12">
        <el-card>
          <template #header>草稿 / 提交</template>
          <el-form label-position="top">
            <el-form-item label="interviewerId">
              <el-input-number v-model="draft.interviewerId" :min="1" style="width: 100%" />
            </el-form-item>
            <el-form-item label="interviewerName">
              <el-input v-model="draft.interviewerName" />
            </el-form-item>
            <el-form-item label="totalScore">
              <el-input-number v-model="draft.totalScore" :min="0" :max="1000" style="width: 100%" />
            </el-form-item>
            <el-form-item label="interviewResult（1 通过 / 0 拒绝）">
              <el-radio-group v-model="draft.interviewResult">
                <el-radio :value="1">通过</el-radio>
                <el-radio :value="0">拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="comprehensiveComment">
              <el-input v-model="draft.comprehensiveComment" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="detailsJson（数组，示例已给）">
              <el-input v-model="draft.detailsJson" type="textarea" :rows="8" />
            </el-form-item>
            <div style="display: flex; gap: 8px">
              <el-button type="primary" :loading="loading" @click="doDraft">保存草稿</el-button>
              <el-button type="success" :loading="loading" @click="doSubmit">提交</el-button>
            </div>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>复核（HR）</template>
          <el-form label-position="top">
            <el-form-item label="interviewResult">
              <el-radio-group v-model="review.interviewResult">
                <el-radio :value="1">通过</el-radio>
                <el-radio :value="0">拒绝</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="remark">
              <el-input v-model="review.remark" type="textarea" :rows="3" />
            </el-form-item>
            <el-button type="warning" :loading="loading" @click="doReview">提交复核</el-button>
          </el-form>

          <el-divider />
          <div class="muted" style="margin-bottom: 6px">当前 evaluations：</div>
          <el-input :model-value="JSON.stringify(list, null, 2)" readonly type="textarea" :rows="10" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

