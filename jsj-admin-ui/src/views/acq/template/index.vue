<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="queryParams.templateName" placeholder="请输入名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd()" v-hasPermi="['acq:template:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="模板名称" prop="templateName" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag>{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="说明" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="当前发布版本" prop="currentVersionId" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['acq:template:query']">版本</el-button>
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['acq:template:edit']">编辑</el-button>
          <el-button link type="primary" v-if="scope.row.status !== 1" @click="handleStatus(scope.row, 1)" v-hasPermi="['acq:template:status']">启用</el-button>
          <el-button link type="primary" v-if="scope.row.status === 1" @click="handleStatus(scope.row, 2)" v-hasPermi="['acq:template:status']">停用</el-button>
          <el-button link type="primary" @click="handleDelete(scope.row)" v-hasPermi="['acq:template:remove']">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有报告模板" />
      </template>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="min(560px, 96vw)" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" maxlength="128" />
        </el-form-item>
        <el-form-item label="说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" maxlength="255" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="模板版本" v-model="detailOpen" width="min(920px, 96vw)" append-to-body>
      <div class="detail-head" v-if="detail">
        <div>
          <strong>{{ detail.templateName }}</strong>
          <span class="muted">当前发布版本：{{ detail.currentVersionId || '尚未发布' }}</span>
        </div>
        <el-button type="primary" plain @click="openDraft()" v-hasPermi="['acq:template:edit']">创建草稿</el-button>
      </div>
      <el-table :data="detail?.versions || []" v-loading="detailLoading">
        <el-table-column label="版本" prop="versionNo" width="80" align="center" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ versionLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="说明" prop="changeNote" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前" width="80" align="center">
          <template #default="scope">{{ scope.row.current ? '是' : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openVersion(scope.row, false)" v-hasPermi="['acq:template:preview']">预览</el-button>
            <el-button link type="primary" v-if="scope.row.status === 0" @click="openVersion(scope.row, true)" v-hasPermi="['acq:template:edit']">编辑</el-button>
            <el-button link type="primary" v-if="scope.row.status === 0" @click="handlePublish(scope.row)" v-hasPermi="['acq:template:publish']">发布</el-button>
            <el-button link type="primary" v-if="scope.row.status === 0" @click="handleDeleteDraft(scope.row)" v-hasPermi="['acq:template:remove']">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="还没有版本" />
        </template>
      </el-table>
    </el-dialog>

    <el-dialog :title="versionTitle" v-model="versionOpen" width="min(760px, 96vw)" append-to-body>
      <el-alert v-if="versionForm.status === 1" type="warning" :closable="false" show-icon title="已发布正文不能直接覆盖。保存会被拒绝，请创建新版本。" />
      <el-alert v-else type="info" :closable="false" show-icon title="支持纯文本与白名单变量替换；不提供测算算法。" />
      <el-form label-width="90px" class="version-form">
        <el-form-item label="版本说明">
          <el-input v-model="versionForm.changeNote" maxlength="255" :disabled="versionReadonly" />
        </el-form-item>
        <el-form-item label="模板正文">
          <el-input v-model="versionForm.contentText" type="textarea" :rows="12" maxlength="20000" show-word-limit :disabled="versionReadonly" />
        </el-form-item>
        <el-form-item label="变量说明">
          <span v-pre>{{name}}、{{gender}}、{{birthDate}}、{{birthTime}}、{{birthTimeProvided}}。未知变量禁止发布；空值显示“未提供”，出生时间不会自动补零点。</span>
        </el-form-item>
      </el-form>
      <el-divider>开发预览，不代表正式测算结果</el-divider>
      <el-form label-width="100px">
        <el-form-item label="姓名"><el-input v-model="previewInput.name" maxlength="64" /></el-form-item>
        <el-form-item label="性别"><el-select v-model="previewInput.gender" clearable><el-option label="未提供" :value="0" /><el-option label="男" :value="1" /><el-option label="女" :value="2" /></el-select></el-form-item>
        <el-form-item label="出生日期"><el-date-picker v-model="previewInput.birthDate" value-format="YYYY-MM-DD" clearable /></el-form-item>
        <el-form-item label="出生时间"><el-time-picker v-model="previewInput.birthTime" value-format="HH:mm:ss" clearable placeholder="可留空" /></el-form-item>
      </el-form>
      <el-button :loading="previewLoading" @click="renderPreview" v-hasPermi="['acq:template:preview']">渲染预览（不保存报告）</el-button>
      <pre v-if="renderedText" class="report-preview">{{ renderedText }}</pre>
      <template #footer>
        <el-button @click="versionOpen = false">关闭</el-button>
        <el-button v-if="!versionReadonly" type="primary" :loading="submitting" @click="saveVersion">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AcqTemplate">
import { useTemplateRef } from 'vue'
import type { FormInstance } from 'element-plus'
const formRefTyped = useTemplateRef<FormInstance>('formRef')

import { useComponentProxy } from '@/utils/component-proxy'
import { listTemplate, getTemplate, addTemplate, updateTemplate, changeTemplateStatus, delTemplate, createDraft, updateDraft, publishVersion, delDraft, previewTemplate } from '@/api/acq/template'

const proxy = useComponentProxy() as any
const statusOptions = [
  { label: '草稿', value: 0 },
  { label: '启用', value: 1 },
  { label: '停用', value: 2 }
]
const loading = ref(false)
const detailLoading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const rows = ref<any[]>([])
const total = ref(0)
const open = ref(false)
const detailOpen = ref(false)
const versionOpen = ref(false)
const versionReadonly = ref(false)
const title = ref('')
const versionTitle = ref('')
const detail = ref<any>(null)
const queryParams = reactive({ pageNum: 1, pageSize: 10, templateName: undefined as string | undefined, status: undefined as number | undefined })
const form = reactive({ id: undefined as string | undefined, templateName: '', remark: '' })
const versionForm = reactive({ id: undefined as string | undefined, templateId: '', status: 0, changeNote: '', contentText: '' })
const previewInput = reactive({ name: '', gender: null as number | null, birthDate: null as string | null, birthTime: null as string | null })
const renderedText = ref('')
const previewLoading = ref(false)
watch([versionOpen, () => versionForm.contentText, () => JSON.stringify(previewInput)], () => { renderedText.value = '' })
async function renderPreview() {
  if (previewLoading.value) return
  previewLoading.value = true
  renderedText.value = ''
  try {
    const result: any = await previewTemplate({ contentText: versionForm.contentText, input: { ...previewInput, name: previewInput.name || null } })
    renderedText.value = result.data.contentText
  } finally { previewLoading.value = false }
}
const rules = { templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }] }

function statusLabel(value: number) {
  return statusOptions.find(item => item.value === value)?.label || '未知'
}
function versionLabel(value: number) {
  if (value === 1) return '已发布'
  if (value === 2) return '停用'
  return '草稿'
}
function getList() {
  loading.value = true
  listTemplate(queryParams).then((response: any) => {
    rows.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => { loading.value = false })
}
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}
function handleAdd() {
  form.id = undefined
  form.templateName = ''
  form.remark = ''
  title.value = '新增报告模板'
  open.value = true
}
function handleUpdate(row: any = {}) {
  form.id = String(row.id)
  form.templateName = row.templateName
  form.remark = row.remark || ''
  title.value = '编辑报告模板'
  open.value = true
}
function submitForm() {
  formRefTyped.value?.validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const data = { templateName: form.templateName, remark: form.remark }
    const request = form.id ? updateTemplate(form.id, data) : addTemplate(data)
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}
function handleStatus(row: any, status: number) {
  changeTemplateStatus(String(row.id), status).then(() => {
    proxy.$modal.msgSuccess('状态已更新')
    getList()
  })
}
function handleDelete(row: any = {}) {
  proxy.$modal.confirm('确认删除该模板吗？已发布或已被使用的模板会被拒绝。').then(() => delTemplate(String(row.id))).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}
function openDetail(row: any) {
  detailOpen.value = true
  loadDetail(String(row.id))
}
function loadDetail(id: string) {
  detailLoading.value = true
  getTemplate(id).then((response: any) => {
    detail.value = response.data
  }).finally(() => { detailLoading.value = false })
}
function openDraft() {
  versionForm.id = undefined
  versionForm.templateId = String(detail.value.id)
  versionForm.status = 0
  versionForm.changeNote = ''
  versionForm.contentText = ''
  versionReadonly.value = false
  versionTitle.value = '新建草稿'
  versionOpen.value = true
}
function openVersion(row: any, editable: boolean) {
  versionForm.id = String(row.id)
  versionForm.templateId = String(row.templateId)
  versionForm.status = row.status
  versionForm.changeNote = row.changeNote || ''
  versionForm.contentText = row.contentText || ''
  versionReadonly.value = !editable || row.status !== 0
  versionTitle.value = row.status === 1 ? '已发布版本' : (editable ? '编辑草稿' : '预览草稿')
  versionOpen.value = true
}
function saveVersion() {
  submitting.value = true
  const data = { changeNote: versionForm.changeNote, contentText: versionForm.contentText }
  const request = versionForm.id
    ? updateDraft(versionForm.id, data)
    : createDraft(versionForm.templateId, data)
  request.then(() => {
    proxy.$modal.msgSuccess('保存成功')
    versionOpen.value = false
    loadDetail(versionForm.templateId)
    getList()
  }).finally(() => { submitting.value = false })
}
function handlePublish(row: any) {
  proxy.$modal.confirm('确认发布该草稿吗？发布后正文不能直接覆盖。').then(() => publishVersion(String(row.id))).then(() => {
    proxy.$modal.msgSuccess('已发布')
    loadDetail(String(detail.value.id))
    getList()
  }).catch(() => {})
}
function handleDeleteDraft(row: any) {
  delDraft(String(row.id)).then(() => {
    proxy.$modal.msgSuccess('草稿已删除')
    loadDetail(String(detail.value.id))
  })
}

getList()
</script>

<style scoped>
.report-preview { white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.8; padding: 16px; background: var(--el-fill-color-light); }
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.muted {
  display: block;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
}
.version-form {
  margin-top: 16px;
}
</style>
