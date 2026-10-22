export interface Field {
  key: string
  label: string
  required?: boolean
  limit?: number
  options?: { label: string; value: string | number }[]
  numeric?: boolean
}
const text = (key: string, label: string, limit = 128, required = false): Field => ({ key, label, limit, required })
const select = (key: string, label: string, values: [string | number, string][]): Field =>
  ({ key, label, required: true, options: values.map(([value, label]) => ({ value, label })) })
export const fields: Record<string, Field[]> = {
  merchant: [text('mchName', '商户名称', 64, true), text('mchId', '商户号', 32, true), text('appId', 'AppID', 32, true), text('remark', '备注', 255)],
  wecom: [text('corpName', '企业名称', 128, true), text('corpId', 'CorpID', 64, true), text('sid', '回调识别标识', 64, true),
    select('authMode', '授权模式', [['UNCONFIRMED', '待确认'], ['SELF_BUILT', '自建应用'], ['THIRD_PARTY', '第三方授权']]), text('remark', '备注', 255)],
  ad: [select('platform', '平台', [[1, '腾讯广告'], [2, '百度'], [3, '快手']]), text('accountName', '账户名称', 128, true),
    text('externalAccountId', '平台账户ID', 64, true), select('interfaceMode', '接口模式', [['UNCONFIRMED', '待确认'], ['TENCENT_MARKETING', '腾讯 Marketing'], ['TENCENT_CONVERSION', '腾讯转化'], ['BAIDU_CONVERSION', '百度转化'], ['KUAISHOU_CONVERSION', '快手转化']]),
    text('dataSourceId', '数据源标识'), text('payEvent', '支付事件映射'),
    select('amountUnit', '金额单位', [['UNCONFIRMED', '待确认'], ['FEN', '分'], ['YUAN', '元']]), text('remark', '备注', 255)],
  route: [select('scopeType', '承接范围', [[1, '全局'], [2, '指定落地页']]), text('landingPageId', '落地页ID', 20),
    text('corpRowId', '企业'), text('teacherRowId', '老师'),
    select('entryType', '获客类型', [[1, '联系我二维码'], [2, '获客助手链接']]),
    text('linkId', '已提供的链接ID'), text('linkUrl', '已提供的HTTPS链接', 512), text('capabilityNote', '权限及额度登记', 255),
    { key: 'sortNo', label: '排序', numeric: true }, text('remark', '备注', 255)],
  version: [text('apiV3KeyRef', 'APIv3密钥引用'), text('privateKeyRef', '商户私钥引用'), text('merchantSerialNo', '商户证书序列号'),
    select('verifyMode', '微信验签模式', [['UNCONFIRMED', '待确认'], ['CERTIFICATE', '平台证书'], ['PUBLIC_KEY', '微信支付公钥']]),
    text('platformCertRef', '平台证书引用'), text('platformKeyId', '平台公钥ID'), text('platformPublicKeyRef', '平台公钥引用'), text('remark', '版本说明', 255)],
  teacher: [text('teacherName', '老师名称', 64, true), text('teacherUserid', '企业内UserID', 64, true)],
  tag: [text('tagName', '标签名称', 64, true), text('tagId', '官方标签ID', 64, true), text('groupId', '标签组ID', 64), text('groupName', '标签组名称', 64)],
  wecomSecret: [text('contactSecretRef', '客户联系Secret引用'), text('tokenRef', '回调Token引用'), text('encodingAesKeyRef', 'EncodingAESKey引用')],
  adSecret: [text('credentialRef', '授权引用')]
}
export const titles: Record<string, string> = { merchant: '支付商户', wecom: '企业微信配置', route: '承接路由', ad: '广告账户', version: '商户配置版本', teacher: '老师', tag: '企业标签', wecomSecret: '企业凭据引用', adSecret: '广告授权引用' }
