<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="页面标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in publishOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd()" v-hasPermi="['acq:landing:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['acq:landing:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="rows" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="标题" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="价格（元）" prop="priceYuan" width="120" align="center" />
      <el-table-column label="按钮" prop="buttonText" width="120" show-overflow-tooltip />
      <el-table-column label="顺序" prop="sortNo" width="80" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.publishStatus)">{{ statusLabel(scope.row.publishStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="采集" min-width="180">
        <template #default="scope">
          {{ switchText(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openPreview(scope.row)" v-hasPermi="['acq:landing:preview']">预览</el-button>
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['acq:landing:edit']">修改</el-button>
          <el-button link type="primary" @click="handleStatus(scope.row, 1)" v-hasPermi="['acq:landing:status']" v-if="scope.row.publishStatus !== 1">启用</el-button>
          <el-button link type="primary" @click="handleStatus(scope.row, 2)" v-hasPermi="['acq:landing:status']" v-if="scope.row.publishStatus === 1">停用</el-button>
          <el-button link type="primary" @click="handleDelete(scope.row)" v-hasPermi="['acq:landing:remove']">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有落地页" />
      </template>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="min(860px, 96vw)" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12" :xs="24">
            <el-form-item label="页面标题" prop="title">
              <el-input v-model="form.title" maxlength="128" placeholder="页面名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="价格（元）" prop="priceYuan">
              <el-input v-model="form.priceYuan" maxlength="12" placeholder="例如 9.90" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主宣传文案" prop="headline">
              <el-input v-model="form.headline" maxlength="255" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="补充文案" prop="sellingText">
              <el-input v-model="form.sellingText" type="textarea" maxlength="500" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="按钮文案" prop="buttonText">
              <el-input v-model="form.buttonText" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="展示顺序" prop="sortNo">
              <el-input-number v-model="form.sortNo" :min="0" :max="999999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12" :xs="24">
            <el-form-item label="发布状态" prop="publishStatus">
              <el-select v-model="form.publishStatus" disabled style="width: 100%">
                <el-option v-for="item in publishOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="支付商户ID"><el-input v-model="form.payMerchantId" placeholder="域名关联多个商户时必须指定" /></el-form-item>
            <el-form-item label="广告账户ID"><el-input v-model="form.adAccountId" placeholder="选填，填写广告账户配置中的 ID" /></el-form-item>
            <el-form-item label="报告模板">
              <el-select v-model="form.reportTemplateId" clearable filterable remote :remote-method="loadTemplates" placeholder="草稿可暂不绑定" style="width: 100%">
                <el-option v-for="t in templates" :key="t.id" :value="t.id" :label="t.templateName + ' / ' + (t.status === 1 ? '启用' : '未启用') + ' / ' + (t.complete ? '已发布 v' + t.versionNo : '配置不完整')" />
              </el-select>
            </el-form-item>
            <el-form-item label="采集字段">
              <el-checkbox v-model="form.nameEnabled" :true-value="1" :false-value="0">姓名</el-checkbox>
              <el-checkbox v-model="form.genderEnabled" :true-value="1" :false-value="0">性别</el-checkbox>
              <el-checkbox v-model="form.birthDateEnabled" :true-value="1" :false-value="0">出生日期</el-checkbox>
              <el-checkbox v-model="form.birthTimeEnabled" :true-value="1" :false-value="0">出生时间</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="图片">
              <div class="asset-panel">
                <el-upload
                  :action="uploadUrl"
                  :headers="headers"
                  :show-file-list="false"
                  accept=".png,.jpg,.jpeg,.gif,.bmp"
                  :before-upload="beforeUpload"
                  :on-success="onUploaded"
                  :on-error="onUploadError"
                >
                  <el-button type="primary" plain :disabled="form.assets.length >= 8">上传图片</el-button>
                </el-upload>
                <span class="asset-tip">最多 8 张，单张不超过 5MB。删除只解除和本页的关联，不删除存储文件。</span>
                <el-empty v-if="form.assets.length === 0" description="还没有图片" :image-size="72" />
                <div v-for="(asset, index) in form.assets" :key="asset.assetUrl + index" class="asset-row">
                  <img :src="imageSrc(asset.assetUrl)" alt="" />
                  <span class="asset-order">第 {{ index + 1 }} 张</span>
                  <el-button link type="primary" :disabled="index === 0" @click="moveAsset(index, -1)">上移</el-button>
                  <el-button link type="primary" :disabled="index === form.assets.length - 1" @click="moveAsset(index, 1)">下移</el-button>
                  <el-button link type="danger" @click="form.assets.splice(index, 1)">移除</el-button>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="配置预览" v-model="previewOpen" width="min(420px, 96vw)" append-to-body>
      <el-alert type="info" :closable="false" show-icon title="这是后台配置预览，不会生成订单、付费报告，也不会调用企业微信。" />
      <div class="preview-card" v-if="preview">
        <img v-for="asset in preview.assets" :key="asset.id || asset.assetUrl" :src="imageSrc(asset.assetUrl)" alt="" />
        <h3>{{ preview.title }}</h3>
        <p>{{ preview.headline }}</p>
        <p class="muted">{{ preview.sellingText }}</p>
        <p class="price">{{ preview.priceYuan }} 元</p>
        <el-button type="primary" disabled>{{ preview.buttonText || '立即测试' }}</el-button>
        <p class="muted">采集：{{ switchText(preview) }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AcqLanding">
import { useTemplateRef } from 'vue'
import type { FormInstance } from 'element-plus'
const formRefTyped = useTemplateRef<FormInstance>('formRef')

import { useComponentProxy } from '@/utils/component-proxy'
import { getToken } from '@/utils/auth'
import { templateOptions } from '@/api/acq/template'
import { listLanding, getLanding, addLanding, updateLanding, changeLandingStatus, delLanding } from '@/api/acq/landing'

interface AssetItem {
  id?: string
  assetUrl: string
}

const proxy = useComponentProxy() as any
const publishOptions = [
  { label: '草稿', value: 0 },
  { label: '已启用', value: 1 },
  { label: '已停用', value: 2 }
]
const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const rows = ref<any[]>([])
const total = ref(0)
const ids = ref<string[]>([])
const multiple = ref(true)
const open = ref(false)
const previewOpen = ref(false)
const preview = ref<any>(null)
const title = ref('')
const uploadUrl = import.meta.env.VITE_APP_BASE_API + '/common/upload'
const headers = ref({ Authorization: 'Bearer ' + getToken() })

const queryParams = reactive({ pageNum: 1, pageSize: 10, title: undefined as string | undefined, publishStatus: undefined as number | undefined })
const form = reactive({
  adAccountId: null as string | null,
  payMerchantId: null as string | null,
  reportTemplateId: null as string | null,
  id: undefined as string | undefined,
  title: '',
  headline: '',
  sellingText: '',
  buttonText: '立即测试',
  priceYuan: '',
  publishStatus: 0,
  sortNo: 0,
  nameEnabled: 1,
  genderEnabled: 1,
  birthDateEnabled: 1,
  birthTimeEnabled: 0,
  assets: [] as AssetItem[]
})
const rules = {
  title: [{ required: true, message: '页面标题不能为空', trigger: 'blur' }],
  buttonText: [{ required: true, message: '按钮文案不能为空', trigger: 'blur' }],
  priceYuan: [{ required: true, pattern: /^(?:[1-9]\d{0,5}(?:\.\d{1,2})?|0\.(?:0[1-9]|[1-9]\d?))$/, message: '请输入0.01到999999.99之间的金额，最多两位小数', trigger: 'blur' }],
  publishStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function statusLabel(value: number) {
  return publishOptions.find(item => item.value === value)?.label || '未知'
}
function statusTag(value: number) {
  if (value === 1) return 'success'
  if (value === 2) return 'info'
  return 'warning'
}
function switchText(row: any) {
  const parts = []
  if (row.nameEnabled === 1) parts.push('姓名')
  if (row.genderEnabled === 1) parts.push('性别')
  if (row.birthDateEnabled === 1) parts.push('出生日期')
  if (row.birthTimeEnabled === 1) parts.push('出生时间')
  return parts.length ? parts.join('、') : '不采集'
}
function imageSrc(url: string) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return import.meta.env.VITE_APP_BASE_API + url
}
function getList() {
  loading.value = true
  listLanding(queryParams).then((response: any) => {
    rows.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}
function handleQuery() {
  queryParams.pageNum = 1
  getList()
}
function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}
function handleSelectionChange(selection: any[]) {
  ids.value = selection.map(item => String(item.id))
  multiple.value = selection.length === 0
}
function resetFormModel() {
  form.payMerchantId = null
  form.adAccountId = null
  form.reportTemplateId = null
  form.id = undefined
  form.title = ''
  form.headline = ''
  form.sellingText = ''
  form.buttonText = '立即测试'
  form.priceYuan = ''
  form.publishStatus = 0
  form.sortNo = 0
  form.nameEnabled = 1
  form.genderEnabled = 1
  form.birthDateEnabled = 1
  form.birthTimeEnabled = 0
  form.assets = []
}
function handleAdd() {
  loadTemplates('')
  resetFormModel()
  title.value = '新增落地页'
  open.value = true
}
function fillForm(data: any) {
  form.payMerchantId = data.payMerchantId == null ? null : String(data.payMerchantId)
  form.adAccountId = data.adAccountId == null ? null : String(data.adAccountId)
  form.reportTemplateId = data.reportTemplateId == null ? null : String(data.reportTemplateId)
  form.id = data.id == null ? undefined : String(data.id)
  form.title = data.title || ''
  form.headline = data.headline || ''
  form.sellingText = data.sellingText || ''
  form.buttonText = data.buttonText || '立即测试'
  form.priceYuan = data.priceYuan || ''
  form.publishStatus = data.publishStatus ?? 0
  form.sortNo = data.sortNo ?? 0
  form.nameEnabled = data.nameEnabled ?? 0
  form.genderEnabled = data.genderEnabled ?? 0
  form.birthDateEnabled = data.birthDateEnabled ?? 0
  form.birthTimeEnabled = data.birthTimeEnabled ?? 0
  form.assets = (data.assets || []).map((item: any) => ({ id: item.id == null ? undefined : String(item.id), assetUrl: item.assetUrl }))
}
function handleUpdate(row: any = {}) {
  loadTemplates('')
  getLanding(String(row.id)).then((response: any) => {
    fillForm(response.data)
    title.value = '修改落地页'
    open.value = true
  })
}
function payload() {
  return {
    reportTemplateId: form.reportTemplateId || null,
    adAccountId: form.adAccountId || null,
    payMerchantId: form.payMerchantId || null,
    title: form.title,
    headline: form.headline,
    sellingText: form.sellingText,
    buttonText: form.buttonText,
    priceYuan: form.priceYuan,
    publishStatus: form.publishStatus,
    sortNo: form.sortNo,
    nameEnabled: form.nameEnabled,
    genderEnabled: form.genderEnabled,
    birthDateEnabled: form.birthDateEnabled,
    birthTimeEnabled: form.birthTimeEnabled,
    assets: form.assets.map(item => ({ id: item.id, assetUrl: item.assetUrl }))
  }
}
function submitForm() {
  formRefTyped.value?.validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const request = form.id ? updateLanding(form.id, payload()) : addLanding(payload())
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    }).finally(() => {
      submitting.value = false
    })
  })
}
function handleStatus(row: any, status: number) {
  const text = status === 1 ? '启用' : '停用'
  proxy.$modal.confirm(`确认${text}“${row.title}”吗？`).then(() => changeLandingStatus(String(row.id), status)).then(() => {
    proxy.$modal.msgSuccess('状态已更新')
    getList()
  }).catch(() => {})
}
function handleDelete(row?: any) {
  const target = row?.id ? [String(row.id)] : ids.value
  proxy.$modal.confirm('确认删除选中的落地页吗？有历史引用的记录会被拒绝。').then(() => delLanding(target)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}
function openPreview(row: any) {
  getLanding(String(row.id)).then((response: any) => {
    preview.value = response.data
    previewOpen.value = true
  })
}
function beforeUpload(file: File) {
  const extension = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase() : ''
  if (!['png', 'jpg', 'jpeg', 'gif', 'bmp'].includes(extension)) {
    proxy.$modal.msgError('只支持 png、jpg、jpeg、gif、bmp')
    return false
  }
  if (file.size / 1024 / 1024 >= 5) {
    proxy.$modal.msgError('图片不能超过 5MB')
    return false
  }
  if (form.assets.length >= 8) {
    proxy.$modal.msgError('最多 8 张图片')
    return false
  }
  return true
}
function onUploaded(response: any) {
  if (response.code !== 200 || !response.fileName) {
    proxy.$modal.msgError(response.msg || '上传失败')
    return
  }
  form.assets.push({ assetUrl: response.fileName })
}
function onUploadError() {
  proxy.$modal.msgError('上传失败')
}
function moveAsset(index: number, offset: number) {
  const target = index + offset
  if (target < 0 || target >= form.assets.length) return
  const list = form.assets
  const current = list[index]
  list[index] = list[target]
  list[target] = current
}

const templates = ref<any[]>([])
async function loadTemplates(keyword: string) {
  const response: any = await templateOptions(keyword)
  templates.value = response.data
}
getList()
</script>

<style scoped>
.asset-panel {
  width: 100%;
}
.asset-tip {
  display: block;
  margin: 8px 0 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.asset-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.asset-row img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
.asset-order {
  min-width: 64px;
}
.preview-card {
  margin-top: 16px;
}
.preview-card img {
  width: 100%;
  border-radius: 4px;
  margin-bottom: 8px;
}
.preview-card h3 {
  margin: 8px 0;
}
.price {
  font-size: 20px;
  font-weight: 600;
}
.muted {
  color: var(--el-text-color-secondary);
}
</style>
