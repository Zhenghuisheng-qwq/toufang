<template>
  <div class="app-container">
    <el-card shadow="never">
      <template #header>付费报告管理</template>
      <el-form inline @submit.prevent="query">
        <el-form-item label="订单号">
          <el-input v-model="orderNo" maxlength="32" clearable placeholder="输入完整订单号" :disabled="busy" />
        </el-form-item>
        <el-form-item><el-button type="primary" :loading="busy" @click="query" v-hasPermi="['acq:report:query']">查询</el-button></el-form-item>
      </el-form>
      <el-alert title="补生成仅处理已确认支付且冻结快照完整的订单，不覆盖已有成功报告。模板渲染不包含测算算法。" type="info" :closable="false" />
      <el-empty v-if="!record" description="请输入订单号查询报告及任务状态" />
      <template v-else>
        <el-descriptions :column="1" border class="report-details">
          <el-descriptions-item label="订单号">{{ record.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ paymentNames[record.payStatus] ?? '未知' }}</el-descriptions-item>
          <el-descriptions-item label="报告状态">{{ record.reportStatus == null ? '无报告' : reportNames[record.reportStatus] ?? '未知' }}</el-descriptions-item>
          <el-descriptions-item label="冻结模板 / 版本">{{ record.templateId ?? '缺失' }} / {{ record.templateVersionId ?? '缺失' }}</el-descriptions-item>
          <el-descriptions-item label="生成时间">{{ record.generatedTime ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="任务">{{ record.taskId ?? '无任务' }} · {{ record.taskStatus == null ? '未登记' : taskNames[record.taskStatus] ?? '未知' }}</el-descriptions-item>
          <el-descriptions-item label="执行次数 / 租约截止">{{ record.attemptCount ?? 0 }} / {{ record.leaseUntil ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="失败分类">{{ record.lastError ?? '—' }}</el-descriptions-item>
        </el-descriptions>
        <div class="report-actions">
          <el-button :disabled="busy || record.reportStatus !== 1" @click="readContent" v-hasPermi="['acq:report:content']">查看正文</el-button>
          <el-button type="warning" :disabled="busy || record.payStatus !== 1 || record.reportStatus === 1" @click="retry" v-hasPermi="['acq:report:retry']">受控补生成</el-button>
        </div>
      </template>
    </el-card>
    <el-dialog v-model="bodyOpen" title="已保存报告正文" width="min(760px, 94vw)" destroy-on-close @closed="body = ''">
      <pre class="report-body">{{ body }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AcqReport">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReportStatus, getReportContent, retryReport, type ReportStatus } from '@/api/acq/report'
const orderNo = ref('')
const record = ref<ReportStatus>()
const busy = ref(false)
const bodyOpen = ref(false)
const body = ref('')
const paymentNames: Record<number, string> = { 0: '待支付', 1: '已支付', 2: '已退款', 3: '已关闭' }
const reportNames: Record<number, string> = { 0: '待生成', 1: '已生成', 2: '生成失败' }
const taskNames: Record<number, string> = { 0: '待执行', 1: '执行中', 2: '成功', 3: '失败待重试', 4: '需要人工核查' }
async function query() {
  if (busy.value) return
  const value = orderNo.value.trim()
  if (!/^[A-Za-z0-9_-]{1,32}$/.test(value)) return void ElMessage.warning('请输入有效的完整订单号')
  busy.value = true
  record.value = undefined
  body.value = ''
  try { record.value = (await getReportStatus(value)).data } catch { /* request interceptor displays the error */ }
  finally { busy.value = false }
}
async function readContent() {
  if (busy.value || !record.value) return
  busy.value = true
  try { body.value = (await getReportContent(record.value.orderNo)).data ?? ''; bodyOpen.value = true }
  catch { /* request interceptor displays the error */ }
  finally { busy.value = false }
}
async function retry() {
  if (busy.value || !record.value) return
  const value = record.value.orderNo
  busy.value = true
  try {
    await ElMessageBox.confirm('确认对订单 ' + value + ' 补生成？后台将重新核验支付、冻结版本与任务租约。', '受控补生成', { type: 'warning' })
    record.value = (await retryReport(value)).data
    ElMessage.success('处理完成，请核对报告状态')
  } catch { /* cancellation or error already displayed by request interceptor */ }
  finally { busy.value = false }
}
</script>

<style scoped>
.report-details { margin-top: 20px; }
.report-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 20px; }
.report-actions .el-button { margin-left: 0; }
.report-body { white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.7; }
</style>
