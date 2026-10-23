<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, message } from './api'
interface Order { orderNo:string; amountFen:number; payStatus:number; title:string; tradeType:number; oauthReady:boolean }
declare global { interface Window { WeixinJSBridge?:{invoke:(name:string,params:Record<string,string>,callback:(result:{err_msg:string})=>void)=>void} } }
const route=useRoute(),order=ref<Order>(),error=ref(''),busy=ref(false),content=ref(''),reportStatus=ref('PENDING')
const entry=ref<{status:string;type?:number;url?:string}>(),path='/orders/'+encodeURIComponent(String(route.params.no))
let active=true,timer:ReturnType<typeof setTimeout>|undefined
async function refresh(){
  try{
    order.value=await api<Order>(path)
    if(order.value.payStatus===1){
      const results=await Promise.allSettled([api<{status:string;content?:string}>(path+'/report'),api<{status:string;type?:number;url?:string}>(path+'/entry')])
      const report=results[0];if(report.status==='fulfilled'){reportStatus.value=report.value.status;content.value=report.value.content||''}else{reportStatus.value='REVIEW_REQUIRED';content.value=''}
      const teacher=results[1];entry.value=teacher.status==='fulfilled'?teacher.value:{status:'REVIEW_REQUIRED'}
    }else{content.value='';entry.value=undefined}
  }catch(e){error.value=message(e);content.value='';entry.value=undefined}
}
async function query(){busy.value=true;error.value='';try{await api(path+'/query',{});await refresh()}catch(e){error.value=message(e)}finally{busy.value=false}}
async function pay(){
  if(busy.value)return;busy.value=true;error.value=''
  try{
    if(order.value?.tradeType===2){
      if(!order.value.oauthReady){const result=await api<{url:string}>(path+'/oauth',{});const url=new URL(result.url);if(url.protocol!=='https:'||url.hostname!=='open.weixin.qq.com')throw new Error('授权链接不可用');location.assign(url.href);return}
      if(!window.WeixinJSBridge)throw new Error('请在微信中打开订单完成支付')
      const params=await api<Record<string,string>>(path+'/jsapi',{})
      await new Promise<void>((resolve,reject)=>window.WeixinJSBridge!.invoke('getBrandWCPayRequest',params,result=>result.err_msg==='get_brand_wcpay_request:ok'?resolve():reject(new Error('支付未完成，可稍后重试'))))
      await query();return
    }
    const result=await api<{url:string}>(path+'/payment',{}),url=new URL(result.url)
    if(url.protocol!=='https:'||url.hostname!=='wx.tenpay.com')throw new Error('支付链接不可用')
    url.searchParams.set('redirect_url',location.origin+location.pathname);location.assign(url.href)
  }catch(e){error.value=message(e)}finally{busy.value=false}
}
function entryUrl(){try{const url=new URL(entry.value?.url||'');return url.protocol==='https:'&&!url.username&&!url.password?url.href:undefined}catch{return undefined}}
async function poll(){await refresh();if(active)timer=setTimeout(poll,5000)}
onMounted(poll);onUnmounted(()=>{active=false;clearTimeout(timer)})
</script>
<template>
  <section v-if="order" class="order"><p class="eyebrow">我的订单</p><h1>{{order.title}}</h1><div class="order-summary"><span>服务金额</span><strong>¥ {{(order.amountFen/100).toFixed(2)}}</strong></div><p class="muted order-number">订单号 {{order.orderNo}}</p>
    <template v-if="order.payStatus===0"><h2>完成支付，解锁你的报告</h2><p class="muted">支付完成后返回此页，我们会确认支付结果。</p><button :disabled="busy" @click="pay">{{busy?'处理中…':'微信支付'}}</button><button class="secondary" :disabled="busy" @click="query">我已支付，查询结果</button></template>
    <template v-else-if="order.payStatus===1"><p class="success">支付成功</p><article v-if="content" class="report"><h2>你的专属报告</h2><div>{{content}}</div></article><p v-else role="status">{{reportStatus==='PENDING'?'报告正在生成，请稍候…':'报告需要处理，支付记录已保留，请联系服务方。'}}</p>
      <section class="teacher"><h2>需要进一步了解？</h2><p class="muted">可自愿添加老师，报告可直接阅读。</p><a v-if="entry?.status==='READY'&&entry.type===2&&entryUrl()" :href="entryUrl()" target="_blank" rel="noopener noreferrer">添加企业微信老师 →</a><img v-else-if="entry?.status==='READY'&&entry.type===1&&entryUrl()" :src="entryUrl()" alt="企业微信老师二维码"><p v-else class="muted">{{entry?.status==='REVIEW_REQUIRED'?'老师入口暂时不可用，不影响报告阅读。':'老师入口正在准备，不影响报告阅读。'}}</p></section>
    </template>
    <p v-else>订单已关闭或已进入退款流程。</p>
  </section>
  <section v-else class="empty"><h1>我的订单</h1><p>{{error?'暂时无法读取订单':'正在读取订单…'}}</p><button v-if="error" @click="refresh">重新读取</button></section>
  <p v-if="error" class="error" role="alert">{{error}}</p>
</template>
