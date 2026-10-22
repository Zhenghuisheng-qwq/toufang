import 'vue'

declare module 'vue' {
  interface ComponentCustomProperties {
    parseTime: typeof import('@/utils/ruoyi').parseTime
    resetForm: typeof import('@/utils/ruoyi').resetForm
    handleTree: typeof import('@/utils/ruoyi').handleTree
    addDateRange: typeof import('@/utils/ruoyi').addDateRange
    selectDictLabel: typeof import('@/utils/ruoyi').selectDictLabel
    selectDictLabels: typeof import('@/utils/ruoyi').selectDictLabels
    $tab: typeof import('@/plugins/tab').default
    $auth: typeof import('@/plugins/auth').default
    $cache: typeof import('@/plugins/cache').default
    $modal: typeof import('@/plugins/modal').default
    $download: typeof import('@/plugins/download').default
    useDict: typeof import('@/utils/dict').useDict
    download: typeof import('@/utils/request').download
    getConfigKey: typeof import('@/api/system/config').getConfigKey
  }
}
export {}
