<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, message, requestKey } from './api'

interface Page { id:string; title:string; headline:string; sellingText:string; buttonText:string; priceFen:number; nameEnabled:boolean; genderEnabled:boolean; birthDateEnabled:boolean; birthTimeEnabled:boolean; images:string[] }
interface Checkout { visitKey:string; leadKey:string; orderKey:string; visitId?:string; leadId?:string; orderNo?:string; tradeType:1|2 }
const route=useRoute(), router=useRouter(), page=ref<Page>(), error=ref(''), busy=ref(false), loading=ref(false)
const name=ref(''), gender=ref(''), birthDate=ref(''), birthTime=ref(''), consent=ref(false)
const id=String(route.params.id || route.query.page || ''), storageKey='checkout:'+id
const now=new Date(), today=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`
let checkout:Checkout={visitKey:requestKey(),leadKey:requestKey(),orderKey:requestKey(),tradeType:/MicroMessenger/i.test(navigator.userAgent)?2:1}, submitted=''
function remember(){ sessionStorage.setItem(storageKey,JSON.stringify(checkout)) }
function restore(){
  const raw=sessionStorage.getItem(storageKey);if(!raw)return
  const saved:unknown=JSON.parse(raw)
  if(!saved||typeof saved!=='object')throw new Error('本地流程记录异常，请重新打开页面')
  const value=saved as Partial<Checkout>
  if(![value.visitKey,value.leadKey,value.orderKey].every(key=>typeof key==='string'&&/^[a-f0-9]{32}$/.test(key)) || ![1,2].includes(Number(value.tradeType)))throw new Error('本地流程记录异常，请重新打开页面')
  checkout=value as Checkout
}
async function createOrder(){
  const order=await api<{orderNo:string}>('/orders',{leadId:checkout.leadId,requestKey:checkout.orderKey,tradeType:checkout.tradeType})
  checkout.orderNo=order.orderNo;remember();await router.replace('/orders/'+encodeURIComponent(order.orderNo))
}
async function load(){
  if(loading.value)return;loading.value=true;error.value=''
  try{
    if(!/^\d+$/.test(id))throw new Error('请从有效的推广链接打开报告页面')
    restore()
    // An existing order remains readable even if its landing page is later unpublished.
    if(checkout.orderNo){await router.replace('/orders/'+encodeURIComponent(checkout.orderNo));return}
    page.value=await api<Page>('/pages/'+id);await api('/session',{})
    if(checkout.leadId){await createOrder();return}
    const search=new URLSearchParams(location.search)
    const baidu=search.has('bd_vid')?location.origin+location.pathname+'?bd_vid='+encodeURIComponent(search.get('bd_vid')!):search.get('logidUrl')
    const clickId=search.get('gdt_vid')||baidu||search.get('callback'), platform=search.has('gdt_vid')?1:baidu?2:search.has('callback')?3:0
    if(!checkout.visitId)checkout.visitId=(await api<{id:string}>('/visits',{pageId:id,requestKey:checkout.visitKey,platform,clickId})).id
    remember();history.replaceState(history.state,'','/pages/'+id)
  }catch(e){error.value=message(e)}finally{loading.value=false}
}
async function submit(){
  if(busy.value||!page.value||!checkout.visitId)return;busy.value=true;error.value=''
  try{
    if(!consent.value)throw new Error('请先确认资料使用说明')
    const fields={visitId:checkout.visitId,name:name.value||null,gender:gender.value?Number(gender.value):null,birthDate:birthDate.value||null,birthTime:birthTime.value?birthTime.value+':00':null}
    const fingerprint=JSON.stringify(fields)
    if(submitted&&submitted!==fingerprint){checkout.leadKey=requestKey();checkout.orderKey=requestKey();checkout.leadId=undefined}
    submitted=fingerprint
    if(!checkout.leadId)checkout.leadId=(await api<{id:string}>('/leads',{...fields,requestKey:checkout.leadKey})).id
    remember();await createOrder()
  }catch(e){error.value=message(e)}finally{busy.value=false}
}
onMounted(load)
</script>

<template>
  <template v-if="page">
    <section class="hero"><p class="eyebrow">为你准备的专属解读</p><h1>{{page.headline||page.title}}</h1><p class="intro">{{page.sellingText}}</p><img v-for="(image,index) in page.images" :key="image" :src="image" :alt="`${page.title}介绍 ${index+1}`" loading="lazy" referrerpolicy="no-referrer"></section>
    <section class="form-section"><div class="section-heading"><h2>填写你的资料</h2><span>约 1 分钟</span></div><p v-if="page.birthTimeEnabled" class="muted">出生时间不知道也可以继续。</p>
      <form @submit.prevent="submit">
        <label v-if="page.nameEnabled">姓名<input v-model="name" required maxlength="64" autocomplete="off" placeholder="请输入姓名"></label>
        <label v-if="page.genderEnabled">性别<select v-model="gender" required><option disabled value="">请选择</option><option value="1">男</option><option value="2">女</option></select></label>
        <label v-if="page.birthDateEnabled">出生日期<input v-model="birthDate" type="date" min="1900-01-01" :max="today" required></label>
        <label v-if="page.birthTimeEnabled">出生时间 <span class="muted">选填</span><input v-model="birthTime" type="time"><button v-if="birthTime" class="text-button" type="button" @click="birthTime=''">不知道，暂不填写</button></label>
        <label class="consent"><input v-model="consent" type="checkbox" required><span>我确认以上资料，并同意将其用于生成本次报告。</span></label>
        <p v-if="error" class="error" role="alert">{{error}}</p><button v-if="error&&!checkout.visitId" type="button" class="secondary" :disabled="loading" @click="load">重新加载</button>
        <div class="purchase"><div><small>报告服务</small><strong>¥ {{(page.priceFen/100).toFixed(2)}}</strong></div><button :disabled="busy||loading||!checkout.visitId" type="submit">{{busy?'正在创建订单…':page.buttonText||'生成我的报告'}}</button></div>
        <p class="muted note">支付后即可查看报告，无需先添加老师。</p>
      </form>
    </section>
  </template>
  <section v-else class="empty"><p class="eyebrow">个人专属报告</p><h1>从了解自己开始</h1><p role="status">{{error||'正在加载页面…'}}</p><button v-if="error" :disabled="loading" @click="load">重新加载</button></section>
</template>
