<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="域名" prop="domainName">
        <el-input v-model="queryParams.domainName" placeholder="例如 a.example.com" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd()" v-hasPermi="['acq:domain:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert class="mb8" type="warning" :closable="false" show-icon title="审核、备案、HTTPS 和支付授权都只是人工登记。页面不会把未核验配置显示成已经可以收款。" />

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="域名" prop="domainName" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">{{ scope.row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核登记" width="120" align="center">
        <template #default="scope">{{ registerLabel(scope.row.reviewStatus, '通过', '未通过') }}</template>
      </el-table-column>
      <el-table-column label="备案登记" width="120" align="center">
        <template #default="scope">{{ registerLabel(scope.row.icpStatus, '已备案', '未备案') }}</template>
      </el-table-column>
      <el-table-column label="HTTPS登记" width="120" align="center">
        <template #default="scope">{{ registerLabel(scope.row.httpsStatus, '已启用', '未启用') }}</template>
      </el-table-column>
      <el-table-column label="实际核验" width="110" align="center">
        <template #default="scope">{{ scope.row.verifyStatus === 1 ? '已验证' : '未验证' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" align="center" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['acq:domain:query']">关联</el-button>
          <el-button link type="primary" @click="handleUpdate(scope.row)" v-hasPermi="['acq:domain:edit']">编辑</el-button>
          <el-button link type="primary" v-if="scope.row.status !== 1" @click="handleStatus(scope.row, 1)" v-hasPermi="['acq:domain:status']">启用</el-button>
          <el-button link type="primary" v-if="scope.row.status === 1" @click="handleStatus(scope.row, 0)" v-hasPermi="['acq:domain:status']">停用</el-button>
          <el-button link type="primary" @click="handleDelete(scope.row)" v-hasPermi="['acq:domain:remove']">删除</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有域名" />
      </template>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="min(640px, 96vw)" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="域名" prop="domainName">
          <el-input v-model="form.domainName" maxlength="253" placeholder="只填主机名，不要带协议和路径" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status" disabled>
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核登记" prop="reviewStatus">
          <el-select v-model="form.reviewStatus" style="width: 100%">
            <el-option label="未登记" :value="0" />
            <el-option label="已登记通过" :value="1" />
            <el-option label="已登记未通过" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="备案登记" prop="icpStatus">
          <el-select v-model="form.icpStatus" style="width: 100%">
            <el-option label="未登记" :value="0" />
            <el-option label="已登记备案" :value="1" />
            <el-option label="已登记未备案" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="HTTPS登记" prop="httpsStatus">
          <el-select v-model="form.httpsStatus" style="width: 100%">
            <el-option label="未登记" :value="0" />
            <el-option label="已登记启用" :value="1" />
            <el-option label="已登记未启用" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="实际核验">
          <el-input model-value="未验证" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" maxlength="255" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog title="域名关联" v-model="detailOpen" width="min(860px, 96vw)" append-to-body>
      <template v-if="detail">
        <p><strong>{{ detail.domainName }}</strong></p>
        <p class="muted">{{ detail.verifyNote }}</p>
        <h4>落地页</h4>
        <div class="link-bar" v-hasPermi="['acq:domain:link']">
          <el-select v-model="selectedLanding" filterable clearable placeholder="选择落地页" style="width: 280px">
            <el-option v-for="item in landingChoices" :key="item.id" :label="item.title" :value="String(item.id)" />
          </el-select>
          <el-button type="primary" plain @click="addLandingLink">关联</el-button>
        </div>
        <el-table :data="detail.landings">
          <el-table-column label="落地页" prop="title" min-width="180" />
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="removeLandingLink(scope.row)" v-hasPermi="['acq:domain:link']">取消关联</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="还没有关联落地页" /></template>
        </el-table>

        <h4>支付商户</h4>
        <el-empty v-if="merchantChoices.length === 0" description="隔离环境里还没有可关联的商户配置" />
        <div v-else class="link-bar" v-hasPermi="['acq:domain:link']">
          <el-select v-model="selectedMerchant" filterable clearable placeholder="选择已有商户" style="width: 360px">
            <el-option v-for="item in merchantChoices" :key="item.id" :label="merchantLabel(item)" :value="String(item.id)" />
          </el-select>
          <el-button type="primary" plain @click="addMerchantLink">关联</el-button>
        </div>
        <el-table :data="detail.merchants">
          <el-table-column label="商户" prop="mchName" min-width="180" />
          <el-table-column label="商户号" prop="mchId" min-width="140" />
          <el-table-column label="核验" width="120" align="center">
            <template #default="scope">{{ scope.row.verifyStatus === 1 ? '已核验' : '未核验' }}</template>
          </el-table-column>
          <el-table-column label="收款能力" min-width="220">
            <template #default="scope">{{ scope.row.paymentNote }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="scope">
              <el-button link type="primary" @click="removeMerchantLink(scope.row)" v-hasPermi="['acq:domain:link']">取消关联</el-button>
            </template>
          </el-table-column>
          <template #empty><el-empty description="还没有关联商户" /></template>
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="AcqDomain">
import { useTemplateRef } from 'vue'
import type { FormInstance } from 'element-plus'
const formRefTyped = useTemplateRef<FormInstance>('formRef')

import { useComponentProxy } from '@/utils/component-proxy'
import { listDomain, getDomain, addDomain, updateDomain, changeDomainStatus, delDomain, linkLanding, unlinkLanding, linkMerchant, unlinkMerchant, merchantOptions } from '@/api/acq/domain'
import { landingOptions } from '@/api/acq/landing'

const proxy = useComponentProxy() as any
const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const rows = ref<any[]>([])
const total = ref(0)
const open = ref(false)
const detailOpen = ref(false)
const title = ref('')
const detail = ref<any>(null)
const landingChoices = ref<any[]>([])
const merchantChoices = ref<any[]>([])
const selectedLanding = ref('')
const selectedMerchant = ref('')
const queryParams = reactive({ pageNum: 1, pageSize: 10, domainName: undefined as string | undefined, status: undefined as number | undefined })
const form = reactive({
  id: undefined as string | undefined,
  domainName: '',
  status: 0,
  reviewStatus: 0,
  icpStatus: 0,
  httpsStatus: 0,
  remark: ''
})
const rules = {
  domainName: [{ required: true, message: '请填写域名', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function registerLabel(value: number, yes: string, no: string) {
  if (value === 1) return yes
  if (value === 2) return no
  return '未登记'
}
function merchantLabel(item: any) {
  const state = item.status === 1 ? '启用' : '停用'
  return `${item.mchName}（${item.mchId}，${state}，未核验收款）`
}
function getList() {
  loading.value = true
  listDomain(queryParams).then((response: any) => {
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
function blankForm() {
  form.id = undefined
  form.domainName = ''
  form.status = 0
  form.reviewStatus = 0
  form.icpStatus = 0
  form.httpsStatus = 0
  form.remark = ''
}
function handleAdd() {
  blankForm()
  title.value = '新增域名'
  open.value = true
}
function handleUpdate(row: any = {}) {
  form.id = String(row.id)
  form.domainName = row.domainName
  form.status = row.status
  form.reviewStatus = row.reviewStatus
  form.icpStatus = row.icpStatus
  form.httpsStatus = row.httpsStatus
  form.remark = row.remark || ''
  title.value = '编辑域名'
  open.value = true
}
function submitForm() {
  formRefTyped.value?.validate((valid: boolean) => {
    if (!valid) return
    submitting.value = true
    const data = {
      domainName: form.domainName,
      status: form.status,
      reviewStatus: form.reviewStatus,
      icpStatus: form.icpStatus,
      httpsStatus: form.httpsStatus,
      remark: form.remark
    }
    const request = form.id ? updateDomain(form.id, data) : addDomain(data)
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    }).finally(() => { submitting.value = false })
  })
}
function handleStatus(row: any, status: number) {
  changeDomainStatus(String(row.id), status).then(() => {
    proxy.$modal.msgSuccess('状态已更新')
    getList()
  })
}
function handleDelete(row: any = {}) {
  proxy.$modal.confirm('确认删除该域名吗？已有访问或订单引用时会被拒绝。').then(() => delDomain(String(row.id))).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}
function openDetail(row: any) {
  detailOpen.value = true
  selectedLanding.value = ''
  selectedMerchant.value = ''
  loadDetail(String(row.id))
  landingOptions().then((response: any) => { landingChoices.value = response.data || [] }).catch(() => { landingChoices.value = [] })
  merchantOptions().then((response: any) => { merchantChoices.value = response.data || [] }).catch(() => { merchantChoices.value = [] })
}
function loadDetail(id: string) {
  getDomain(id).then((response: any) => { detail.value = response.data })
}
function addLandingLink() {
  if (!selectedLanding.value) {
    proxy.$modal.msgError('请选择落地页')
    return
  }
  linkLanding(String(detail.value.id), selectedLanding.value).then(() => {
    proxy.$modal.msgSuccess('已关联')
    loadDetail(String(detail.value.id))
  })
}
function removeLandingLink(row: any) {
  unlinkLanding(String(detail.value.id), String(row.landingPageId)).then(() => {
    proxy.$modal.msgSuccess('已取消关联，历史订单快照不会被改写')
    loadDetail(String(detail.value.id))
  })
}
function addMerchantLink() {
  if (!selectedMerchant.value) {
    proxy.$modal.msgError('请选择商户')
    return
  }
  linkMerchant(String(detail.value.id), selectedMerchant.value).then(() => {
    proxy.$modal.msgSuccess('已登记关联。这不代表具备真实收款能力')
    loadDetail(String(detail.value.id))
  })
}
function removeMerchantLink(row: any) {
  unlinkMerchant(String(detail.value.id), String(row.payMerchantId)).then(() => {
    proxy.$modal.msgSuccess('已取消关联，历史订单中的商户快照不会被改写')
    loadDetail(String(detail.value.id))
  })
}

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.muted { color: var(--el-text-color-secondary); }
.link-bar { display: flex; gap: 8px; margin: 8px 0 12px; flex-wrap: wrap; }
h4 { margin: 16px 0 8px; }
</style>
