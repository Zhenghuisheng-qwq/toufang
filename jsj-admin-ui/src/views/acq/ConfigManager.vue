<template>
  <div class="app-container config-page">
    <el-alert type="info" :closable="false" show-icon title="这里只登记配置。字段完整、已启用不代表平台授权或联调通过；凭据引用尚未解析或验证。" />
    <el-form inline class="filters" @submit.prevent="search">
      <el-form-item v-if="kind !== 'route'" label="名称"><el-input v-model="query.keyword" clearable @keyup.enter="search" /></el-form-item>
      <el-form-item v-if="kind === 'ad'" label="平台">
        <el-select v-model="query.platform" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="p in fields.ad[0].options" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部" style="width: 120px"><el-option label="启用" :value="1" /><el-option label="停用" :value="0" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <div class="toolbar">
      <el-button v-if="can('add')" type="primary" plain @click="edit(kind)">新增{{ titles[kind] }}</el-button>
      <template v-if="kind === 'route' && can('preview')">
        <el-input v-model="previewPage" placeholder="预览落地页ID；空为全局" style="width: 240px" />
        <el-button :loading="previewLoading" @click="previewRoute">解析预览</el-button>
      </template>
    </div>
    <el-alert v-if="routePreview" :title="'解析结果：企业 ' + routePreview.corpId + ' / 老师 ' + routePreview.teacherUserid + ' / ' + (routePreview.scopeType === 1 ? '全局' : '指定页')" type="success" :closable="false" />
    <el-table :data="rows" v-loading="loading">
      <el-table-column v-for="col in columns" :key="col.key" :prop="col.key" :label="col.label" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ display(col.key, row[col.key]) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用配置' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="{ row }">
          <el-button v-if="can('query')" link type="primary" @click="showDetail(row)">详情</el-button>
          <el-button v-if="can('edit')" link type="primary" @click="edit(kind, row)">编辑</el-button>
          <el-button v-if="can(kind === 'route' ? 'switch' : 'status')" link type="primary" :disabled="busy" @click="changeStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          <el-button v-if="can('remove')" link type="danger" :disabled="busy" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
      <template #empty><el-empty :description="'暂无' + titles[kind]" /></template>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load" />

    <el-dialog v-model="detailOpen" :title="titles[kind] + '详情'" width="min(1050px, 96vw)" destroy-on-close @closed="detail = null">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item v-for="field in fields[kind]" :key="field.key" :label="field.label">{{ display(field.key, detail[field.key]) }}</el-descriptions-item>
            <el-descriptions-item label="验证状态">未通过平台联调</el-descriptions-item>
          </el-descriptions>
          <div class="toolbar">
            <el-button v-if="(kind === 'wecom' || kind === 'ad') && can('secret')" @click="editSecrets">管理凭据引用</el-button>
            <template v-if="kind === 'merchant'">
              <el-button v-if="can('version') && can('secret')" @click="edit('version')">创建草稿版本</el-button>
              <el-button v-if="can('version') && detail.currentVersionId" :disabled="busy" @click="deactivateVersion">停用当前版本</el-button>
            </template>
          </div>
          <template v-if="kind === 'merchant'">
            <h3>配置版本</h3>
            <el-table :data="detail.versions">
              <el-table-column label="版本" prop="versionNo" width="80" />
              <el-table-column label="状态"><template #default="{ row }">{{ ['草稿', '启用', '历史停用'][row.status] || '未知' }}{{ row.current ? '（当前）' : '' }}</template></el-table-column>
              <el-table-column label="完整性"><template #default="{ row }">{{ row.complete ? '字段完整，未验证' : '缺少必要字段' }}</template></el-table-column>
              <el-table-column label="说明" prop="remark" show-overflow-tooltip />
              <el-table-column label="操作" width="200"><template #default="{ row }">
                <el-button v-if="can('secret')" link @click="viewVersion(row)">{{ row.status === 0 && can('version') ? '编辑引用' : '查看引用' }}</el-button>
                <el-button v-if="can('version') && !row.current" link type="primary" :disabled="busy || !row.complete" @click="activateVersion(row)">启用版本</el-button>
              </template></el-table-column>
              <template #empty><el-empty description="尚无配置版本" /></template>
            </el-table>
            <h3>关联域名</h3>
            <el-table :data="detail.domains"><el-table-column prop="domainName" label="域名" /><el-table-column label="核验状态"><template #default="{ row }">{{ row.verifyStatus === 1 ? '已有核验记录' : '未核验' }}</template></el-table-column></el-table>
          </template>
          <template v-if="kind === 'wecom'">
            <el-tabs v-model="childTab">
              <el-tab-pane v-for="child in ['teacher', 'tag']" :key="child" :label="titles[child]" :name="child">
                <div class="toolbar"><el-input v-model="childFilter" placeholder="按名称或ID筛选" clearable style="width: 240px" /><el-button v-if="can('add')" @click="edit(child)">新增{{ titles[child] }}</el-button></div>
                <el-table :data="children(child)">
                  <el-table-column v-for="field in fields[child]" :key="field.key" :prop="field.key" :label="field.label" min-width="130" show-overflow-tooltip />
                  <el-table-column label="状态" width="80"><template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template></el-table-column>
                  <el-table-column v-if="child === 'teacher'" label="当前路由引用" prop="routeReferences" width="115" />
                  <el-table-column label="操作" width="200"><template #default="{ row }">
                    <el-button v-if="can('edit')" link @click="edit(child, row)">编辑</el-button>
                    <el-button v-if="can('status')" link :disabled="busy" @click="changeStatus(row, child)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                    <el-button v-if="can('remove')" link type="danger" :disabled="busy" @click="remove(row, child)">删除</el-button>
                  </template></el-table-column>
                </el-table>
                <p class="hint">仅系统登记，未向企业微信查询或创建成员、标签。</p>
              </el-tab-pane>
              <el-tab-pane label="路由引用" name="routes"><el-table :data="detail.routes"><el-table-column prop="id" label="路由ID" /><el-table-column prop="teacherUserid" label="老师" /><el-table-column prop="landingPageId" label="落地页ID（空为全局）" /><el-table-column prop="status" label="状态（1=启用）" /></el-table></el-tab-pane>
            </el-tabs>
          </template>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="editOpen" :title="(readonly ? '查看' : editingId ? '编辑' : '新增') + titles[editKind]" width="min(680px, 96vw)" append-to-body destroy-on-close :close-on-click-modal="!saving" :show-close="!saving" @closed="clearEditor">
      <el-alert v-if="isSensitive" type="warning" :closable="false" title="仅填写 ref: 引用，不填写密钥、私钥、Token 原文。引用仅登记，未解析或验证。" />
      <el-alert v-if="editKind === 'route'" type="info" :closable="false" title="指定页路由优先；指定配置失效不会自动换企业。启用会原子替换同范围当前路由。不会创建二维码或订单。" />
      <el-form ref="editor" :model="form" label-width="150px" class="editor" :disabled="readonly || saving">
        <el-form-item v-for="field in visibleFields" :key="field.key" :label="field.label" :prop="field.key" :rules="field.required ? [{ required: true, message: '请填写' + field.label, trigger: 'blur' }] : []">
          <el-select v-if="field.key === 'corpRowId'" v-model="form.corpRowId" filterable remote :remote-method="searchCorps" :loading="optionsLoading" placeholder="输入企业名称查找" @change="corpChanged">
            <el-option v-for="c in corpOptions" :key="c.id" :value="c.id" :label="c.corpName + ' / ' + c.corpId" />
          </el-select>
          <el-select v-else-if="field.key === 'teacherRowId'" v-model="form.teacherRowId" filterable placeholder="先选择企业">
            <el-option v-for="t in teacherOptions" :key="t.id" :value="t.id" :label="t.teacherName + ' / ' + t.teacherUserid" />
          </el-select>
          <el-select v-else-if="field.options" v-model="form[field.key]" @change="fieldChanged(field.key)"><el-option v-for="o in options(field)" :key="o.value" :label="o.label" :value="o.value" /></el-select>
          <el-input-number v-else-if="field.numeric" v-model="form[field.key]" :min="0" :max="999999" />
          <el-input v-else v-model="form[field.key]" :maxlength="field.limit" :type="field.key === 'remark' ? 'textarea' : 'text'" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="editOpen = false">关闭</el-button><el-button v-if="!readonly" type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { configRequest, type ConfigKind } from '@/api/acq/config'
import { checkPermi } from '@/utils/permission'
import { fields, titles, type Field } from './config-fields'
const props = defineProps<{ kind: ConfigKind }>()
const kind = props.kind
type Row = Record<string, any>
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined, platform: undefined as number | undefined })
const rows = ref<Row[]>([]), total = ref(0), loading = ref(false), busy = ref(false)
const detail = ref<Row | null>(null), detailOpen = ref(false), detailLoading = ref(false)
const editOpen = ref(false), editKind = ref<string>(kind), editingId = ref<string>(), readonly = ref(false), saving = ref(false), editor = ref<any>()
const form = reactive<Row>({})
const childTab = ref('teacher'), childFilter = ref('')
const previewPage = ref(''), routePreview = ref<Row | null>(null), previewLoading = ref(false)
const corpOptions = ref<Row[]>([]), teacherOptions = ref<Row[]>([]), optionsLoading = ref(false)
const can = (action: string) => checkPermi(['acq:' + kind + ':' + action])
const columns = computed(() => fields[kind].filter(f => kind === 'route' ? ['scopeType', 'landingPageId', 'corpRowId', 'teacherRowId', 'entryType'].includes(f.key) : kind === 'ad' ? ['platform', 'accountName', 'externalAccountId', 'interfaceMode'].includes(f.key) : kind === 'merchant' ? ['mchName', 'mchId', 'appId'].includes(f.key) : ['corpName', 'corpId', 'sid', 'authMode'].includes(f.key)))
const isSensitive = computed(() => editKind.value === 'version' || editKind.value.endsWith('Secret'))
const visibleFields = computed(() => (fields[editKind.value] || []).filter(f => {
  if (editKind.value === 'route') return !(f.key === 'landingPageId' && form.scopeType === 1) && !(['linkId', 'linkUrl'].includes(f.key) && form.entryType === 1)
  if (editKind.value === 'version') return !(f.key === 'platformCertRef' && form.verifyMode !== 'CERTIFICATE') && !(['platformKeyId', 'platformPublicKeyRef'].includes(f.key) && form.verifyMode !== 'PUBLIC_KEY')
  return true
}))
function options(field: Field) {
  if (field.key !== 'interfaceMode') return field.options
  const prefix = form.platform === 1 ? 'TENCENT_' : form.platform === 2 ? 'BAIDU_' : 'KUAISHOU_'
  return field.options?.filter(o => o.value === 'UNCONFIRMED' || String(o.value).startsWith(prefix))
}
function display(key: string, value: unknown) {
  const f = Object.values(fields).flat().find(f => f.key === key && f.options)
  return f?.options?.find(o => o.value === value)?.label || value || '—'
}
async function load() {
  loading.value = true
  try { const r: any = await configRequest(kind, '/list', 'get', query); rows.value = r.data.rows; total.value = r.data.total }
  finally { loading.value = false }
}
function search() { query.pageNum = 1; void load() }
function reset() { Object.assign(query, { pageNum: 1, keyword: '', status: undefined, platform: undefined }); void load() }
async function showDetail(row: Row) {
  detailOpen.value = true; detailLoading.value = true; detail.value = null; childFilter.value = ''
  try { const r: any = await configRequest(kind, '/' + row.id); detail.value = r.data }
  finally { detailLoading.value = false }
}
async function refresh() { await load(); if (detailOpen.value && detail.value) await showDetail(detail.value) }
function clearEditor() { Object.keys(form).forEach(k => delete form[k]) }
function edit(type: string, row?: Row, read = false) {
  clearEditor(); editKind.value = type; editingId.value = row?.id; readonly.value = read
  for (const field of fields[type]) form[field.key] = row?.[field.key] ?? field.options?.[0].value ?? (field.numeric ? 0 : '')
  editOpen.value = true
  if (type === 'route') { corpOptions.value = []; teacherOptions.value = []; void searchCorps(''); if (form.corpRowId) void loadTeachers() }
}
async function searchCorps(keyword: string) {
  optionsLoading.value = true
  try { const r: any = await configRequest('route', '/options', 'get', { keyword }); corpOptions.value = r.data.rows }
  finally { optionsLoading.value = false }
}
async function loadTeachers() {
  const r: any = await configRequest('route', '/options/' + form.corpRowId)
  teacherOptions.value = r.data.teachers
  if (!corpOptions.value.some(c => c.id === r.data.id)) corpOptions.value.push(r.data)
}
function corpChanged() { form.teacherRowId = ''; teacherOptions.value = []; if (form.corpRowId) void loadTeachers() }
function fieldChanged(key: string) {
  if (key === 'platform') form.interfaceMode = 'UNCONFIRMED'
  if (key === 'scopeType' && form.scopeType === 1) form.landingPageId = ''
  if (key === 'entryType' && form.entryType === 1) { form.linkId = ''; form.linkUrl = '' }
  if (key === 'verifyMode') { form.platformCertRef = ''; form.platformKeyId = ''; form.platformPublicKeyRef = '' }
}
async function save() {
  if (saving.value || readonly.value || !(await editor.value.validate().catch(() => false))) return
  saving.value = true
  try {
    const data: Row = Object.fromEntries(fields[editKind.value].map(f => [f.key, form[f.key] === '' ? null : form[f.key]]))
    let path = editingId.value ? '/' + editingId.value : '', method = editingId.value ? 'put' : 'post'
    if (editKind.value === 'teacher' || editKind.value === 'tag') { path = '/' + editKind.value + path; data.corpRowId = detail.value!.id }
    if (editKind.value === 'version') path = '/' + detail.value!.id + '/version' + path
    if (editKind.value.endsWith('Secret')) { path = '/' + detail.value!.id + '/secrets'; method = 'put' }
    await configRequest(kind, path, method, data)
    editOpen.value = false; ElMessage.success('已保存配置，未进行平台验证'); await refresh()
  } finally { saving.value = false }
}
async function confirmed(message: string, action: () => Promise<unknown>) {
  if (busy.value) return
  try { await ElMessageBox.confirm(message, '确认操作', { type: 'warning' }) } catch { return }
  busy.value = true
  try { await action(); ElMessage.success('配置已更新'); await refresh() } finally { busy.value = false }
}
function changeStatus(row: Row, child?: string) {
  return confirmed('确认' + (row.status === 1 ? '停用' : '启用') + '此配置？历史订单不随之变更。', () => configRequest(kind, (child ? '/' + child : '') + '/' + row.id + '/status', 'put', { status: row.status === 1 ? 0 : 1 }))
}
function remove(row: Row, child?: string) { return confirmed('确认删除？存在历史引用时服务端会拒绝，请改为停用。', () => configRequest(kind, (child ? '/' + child : '') + '/' + row.id, 'delete')) }
function activateVersion(row: Row) { return confirmed('确认启用该配置版本并停用原版本？这不代表真实支付可用。', () => configRequest(kind, '/' + detail.value!.id + '/version/' + row.id + '/activate', 'put')) }
function deactivateVersion() { return confirmed('确认停用当前版本？', () => configRequest(kind, '/' + detail.value!.id + '/version/deactivate', 'put')) }
async function editSecrets() { const r: any = await configRequest(kind, '/' + detail.value!.id + '/secrets'); edit(kind + 'Secret', r.data) }
async function viewVersion(row: Row) {
  const r: any = await configRequest(kind, '/' + detail.value!.id + '/secrets')
  const version = r.data.versions.find((v: Row) => v.id === row.id)
  if (version) edit('version', version, row.status !== 0 || !can('version'))
}
function children(type: string) {
  const source: Row[] = detail.value?.[type === 'teacher' ? 'teachers' : 'tags'] || []
  const filter = childFilter.value.trim().toLowerCase()
  return source.filter(row => fields[type].some(f => String(row[f.key] || '').toLowerCase().includes(filter)))
}
async function previewRoute() {
  if (previewLoading.value) return
  previewLoading.value = true; routePreview.value = null
  try { const r: any = await configRequest('route', '/preview', 'get', { landingPageId: previewPage.value || undefined }); routePreview.value = r.data }
  finally { previewLoading.value = false }
}
onMounted(load)
</script>

<style scoped>
.filters, .toolbar, .editor { margin-top: 18px; }
.toolbar { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; margin-bottom: 18px; }
.toolbar .el-button + .el-button { margin-left: 0; }
.editor :deep(.el-select) { width: 100%; }
.hint { color: var(--el-text-color-secondary); line-height: 1.6; }
@media (max-width: 600px) {
  .editor :deep(.el-form-item) { display: block; }
  .editor :deep(.el-form-item__label) { justify-content: flex-start; }
  .editor :deep(.el-form-item__content) { margin-left: 0 !important; }
}
</style>
