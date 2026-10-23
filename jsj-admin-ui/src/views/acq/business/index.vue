<template><div class="app-container"><el-card shadow="never"><template #header>投放业务查询</template>
<el-tabs v-model="tab" @tab-change="reset"><el-tab-pane label="订单" name="orders"/><el-tab-pane label="线索" name="leads"/><el-tab-pane label="广告转化" name="events"/><el-tab-pane label="统计" name="stats"/></el-tabs>
<el-alert v-if="error" :title="error" type="error" :closable="false"/>
<el-descriptions v-if="tab==='stats'" border :column="1"><el-descriptions-item label="订单数">{{stats.orders??'—'}}</el-descriptions-item><el-descriptions-item label="已支付订单">{{stats.paidOrders??'—'}}</el-descriptions-item><el-descriptions-item label="已支付金额（分）">{{stats.paidAmountFen??'—'}}</el-descriptions-item></el-descriptions>
<el-table v-else :data="rows" v-loading="busy" empty-text="暂无记录" stripe><el-table-column v-for="column in columns[tab]" :key="column.key" :prop="column.key" :label="column.title" min-width="150" show-overflow-tooltip/><el-table-column v-if="tab==='orders'" label="处理" width="160"><template #default="scope"><el-button v-hasPermi="['acq:order:refund']" link type="danger" :disabled="actionBusy || ![1,2].includes(Number(scope.row.payStatus))" @click="refund(scope.row)">全额退款 / 查单</el-button></template></el-table-column></el-table>
<div class="actions"><el-button @click="load" :loading="busy">刷新</el-button><template v-if="tab!=='stats'"><el-button :disabled="page===1||busy" @click="page--;load()">上一页</el-button><span>第 {{page}} 页</span><el-button :disabled="rows.length<30||busy" @click="page++;load()">下一页</el-button></template></div>
</el-card></div></template>
<script setup lang="ts" name="AcqBusiness">
import {ref,onMounted} from 'vue'
import request from '@/utils/request'
import {ElMessage,ElMessageBox} from 'element-plus'
type Tab='orders'|'leads'|'events'|'stats'
const tab=ref<Tab>('orders'),page=ref(1),busy=ref(false),error=ref(''),rows=ref<Record<string,unknown>[]>([]),stats=ref<Record<string,unknown>>({})
const columns:Record<Tab,{key:string;title:string}[]>={orders:[{key:'orderNo',title:'订单号'},{key:'amountFen',title:'金额（分）'},{key:'payStatus',title:'支付状态 0待付/1已付/2退款/3关闭'},{key:'domainName',title:'域名'},{key:'mchId',title:'商户'},{key:'corpId',title:'企业'},{key:'teacherUserid',title:'老师'}],leads:[{key:'id',title:'线索ID'},{key:'pageId',title:'落地页ID'},{key:'domainName',title:'域名'},{key:'platform',title:'平台 1腾讯/2百度/3快手'},{key:'submitTime',title:'提交时间'}],events:[{key:'eventKey',title:'事件键'},{key:'orderNo',title:'订单号'},{key:'eventType',title:'事件 1支付/2提交/3加老师'},{key:'status',title:'状态 0待发/1成功/2无点击/3重试/4未知/5阻断/6执行'},{key:'retryCount',title:'尝试次数'},{key:'lastError',title:'结果分类'}],stats:[]}
async function load(){if(busy.value)return;busy.value=true;error.value='';rows.value=[];try{const response=await request({url:'/acq/business/'+tab.value,params:{page:page.value}});if(tab.value==='stats')stats.value=response.data;else rows.value=response.data}catch{error.value='查询失败，请确认权限或稍后重试'}finally{busy.value=false}}
const actionBusy=ref(false)
async function refund(row:Record<string,unknown>){
 if(actionBusy.value)return
 try{
  const {value}=await ElMessageBox.prompt(`订单 ${String(row.orderNo)}，全额 ${Number(row.amountFen)/100} 元。成功后报告权限将撤销；已有退款单仅查单。请输入原因：`,'全额退款确认',{confirmButtonText:'确认申请 / 查单',cancelButtonText:'取消',inputValidator:(v:string)=>!!v?.trim()&&v.length<=80||'请填写1至80字原因'})
  actionBusy.value=true
  const result=await request({url:'/acq/business/orders/'+encodeURIComponent(String(row.orderNo))+'/refund',method:'post',data:{reason:value.trim()}})
  ElMessage.info(Number(result.data.status)===1?'退款已确认，报告权限已撤销':'退款单已记录，需继续查单确认结果');await load()
 }catch(e){if(e!=='cancel'&&e!=='close')error.value='退款未完成，请查看服务端提示；再次操作将按原退款单查单'}finally{actionBusy.value=false}
}
function reset(){page.value=1;void load()}onMounted(load)
</script>
<style scoped>.actions{display:flex;gap:12px;align-items:center;margin-top:20px;flex-wrap:wrap}</style>
