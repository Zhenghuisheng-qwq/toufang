// Browser-only contract fixtures. No mock endpoints or mock payment code enter src/dist.
// Start a dedicated Chrome with remote debugging and the built Vite preview first.
import fs from 'node:fs'
const base=process.env.H5_TEST_URL||'http://127.0.0.1:4174'
const cdp=process.env.H5_TEST_CDP||'http://127.0.0.1:9222'
const out=process.env.H5_TEST_OUTPUT||'../.local/h5-module-verification'
fs.mkdirSync(out,{recursive:true})
const target=await(await fetch(cdp+'/json/new?about:blank',{method:'PUT'})).json()
const ws=new WebSocket(target.webSocketDebuggerUrl)
await new Promise(resolve=>ws.addEventListener('open',resolve,{once:true}))
let seq=0,payStatus=0,denied=false,unavailable=false
const pending=new Map(),errors=[],requests=[],checks=[]
function send(method,params={}){return new Promise((resolve,reject)=>{const id=++seq;pending.set(id,{resolve,reject});ws.send(JSON.stringify({id,method,params}))})}
function assert(value,text){if(!value)throw new Error(text);checks.push(text)}
function fixture(request){
  const path=new URL(request.url).pathname.replace('/api/h5','')
  const body=request.postData?JSON.parse(request.postData):undefined
  requests.push({path,body})
  if(path.startsWith('/pages/'))return unavailable?{code:500,msg:'页面未发布或配置不足'}:{code:200,data:{id:'7',title:'个人专属报告',headline:'读懂自己，开启新的可能',sellingText:'一份属于你的个人报告。填写资料后，即可继续查看订单。',buttonText:'查看我的报告',priceFen:1990,nameEnabled:true,genderEnabled:true,birthDateEnabled:true,birthTimeEnabled:true,images:[]}}
  if(path==='/session')return {code:200}
  if(path==='/visits')return {code:200,data:{id:'7001'}}
  if(path==='/leads')return {code:200,data:{id:'7002'}}
  if(path==='/orders')return {code:200,data:{orderNo:'fixture-order'}}
  if(denied)return {code:500,msg:'订单不存在或访问凭证不匹配'}
  if(path.endsWith('/payment'))return {code:500,msg:'真实支付尚未开放'}
  if(path.endsWith('/report'))return {code:200,data:{status:'READY',content:'浏览器契约测试报告。出生时间：未提供。'}}
  if(path.endsWith('/entry'))return {code:200,data:{status:'REVIEW_REQUIRED'}}
  return {code:200,data:{orderNo:'fixture-order',title:'个人专属报告',amountFen:1990,payStatus,tradeType:1,oauthReady:false}}
}
ws.onmessage=event=>{
  const msg=JSON.parse(event.data)
  if(msg.id){const callback=pending.get(msg.id);pending.delete(msg.id);msg.error?callback.reject(new Error(msg.error.message)):callback.resolve(msg.result)}
  else if(msg.method==='Runtime.exceptionThrown')errors.push(msg.params.exceptionDetails.text)
  else if(msg.method==='Fetch.requestPaused'){
    const data=fixture(msg.params.request)
    send('Fetch.fulfillRequest',{requestId:msg.params.requestId,responseCode:200,responseHeaders:[{name:'Content-Type',value:'application/json'},{name:'Cache-Control',value:'no-store'}],body:Buffer.from(JSON.stringify(data)).toString('base64')}).catch(error=>errors.push(error.message))
  }
}
async function evaluate(expression){const result=await send('Runtime.evaluate',{expression,awaitPromise:true,returnByValue:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.text);return result.result.value}
async function until(expression){for(let i=0;i<100;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,100))}throw new Error('Condition failed: '+expression)}
async function navigate(path){await send('Page.navigate',{url:base+path})}
async function screenshot(name){fs.writeFileSync(out+'/'+name+'.png',Buffer.from((await send('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})).data,'base64'))}
try{
  await send('Page.enable');await send('Runtime.enable')
  await navigate('/');await until('document.querySelector("main")')
  // Before interception, this traverses the actual Vite proxy and existing Mini controller/database.
  const real=await evaluate("fetch('/api/h5/pages/999999999').then(async r=>({http:r.status,body:await r.json()}))")
  assert(real.http===200&&real.body.code===500&&real.body.msg==='页面不存在','real Mini read-only missing-page response through same-origin proxy')
  await send('Fetch.enable',{patterns:[{urlPattern:'*/api/h5/*',requestStage:'Request'}]})
  await navigate('/pages/7?bd_vid=fixture-click');await until("document.querySelector('button[type=submit]')&&!document.querySelector('button[type=submit]').disabled")
  for(const width of [390,768,1024,1440]){
    await send('Emulation.setDeviceMetricsOverride',{width,height:1000,deviceScaleFactor:1,mobile:width===390})
    assert(!await evaluate('document.documentElement.scrollWidth>window.innerWidth'),'no horizontal overflow at '+width)
    if(width===390||width===1440)await screenshot('landing-'+width)
  }
  await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true})
  await evaluate("document.querySelector('form').requestSubmit()")
  assert(!requests.some(r=>r.path==='/leads'),'empty form blocked before submission')
  await evaluate(`(()=>{const input=document.querySelector('input[autocomplete=off]');input.value='前端验收';input.dispatchEvent(new Event('input',{bubbles:true}));const gender=document.querySelector('select');gender.value='1';gender.dispatchEvent(new Event('change',{bubbles:true}));const date=document.querySelector('input[type=date]');date.value='1990-01-01';date.dispatchEvent(new Event('input',{bubbles:true}));document.querySelector('input[type=checkbox]').click()})()`)
  await evaluate("document.querySelector('form').requestSubmit();document.querySelector('form').requestSubmit()")
  await until("location.pathname==='/orders/fixture-order'&&document.querySelector('.order-summary')")
  assert(requests.filter(r=>r.path==='/orders').length===1,'double submit creates one frontend order request')
  assert(requests.find(r=>r.path==='/leads').body.birthTime===null,'optional birth time remains absent')
  assert(requests.find(r=>r.path==='/visits').body.clickId.includes('bd_vid=fixture-click'),'Baidu click parameter preserved')
  assert(!('amountFen' in requests.find(r=>r.path==='/orders').body),'order request contains no client price')
  await send('Page.reload');await until("document.querySelector('.order-summary')")
  await navigate('/pages/7');await until("location.pathname==='/orders/fixture-order'&&document.querySelector('.order-summary')")
  assert(requests.filter(r=>r.path==='/orders').length===1,'refresh and return restore existing order')
  await evaluate("Array.from(document.querySelectorAll('button')).find(e=>e.textContent.includes('微信支付')).click()")
  await until("document.body.innerText.includes('真实支付尚未开放')")
  assert(await evaluate("location.pathname==='/orders/fixture-order'"),'disabled payment preserves order')
  payStatus=1;await send('Page.reload');await until("document.querySelector('.report')&&document.body.innerText.includes('老师入口暂时不可用')")
  assert(await evaluate("document.querySelector('.report').innerText.includes('未提供')"),'report visible independently of unavailable teacher')
  await screenshot('report-390')
  payStatus=2;await send('Page.reload');await until("document.body.innerText.includes('退款流程')")
  assert(!await evaluate("document.querySelector('.report')"),'refund state hides report')
  denied=true;await send('Page.reload');await until("document.body.innerText.includes('访问凭证不匹配')")
  assert(!await evaluate("document.querySelector('.report')"),'credential error exposes no report')
  unavailable=true;await navigate('/pages/8');await until("document.body.innerText.includes('页面未发布或配置不足')")
  assert(!await evaluate("document.querySelector('form')"),'unavailable page has no submission form')
  assert(errors.length===0,'no browser runtime exceptions')
  fs.writeFileSync(out+'/result.json',JSON.stringify({source:'jsj-mini-app/dist',realApi:'read-only missing-page response',browser:'Chrome',mockedBusinessResponses:true,checks},null,2))
  console.log(JSON.stringify({result:'PASS',checks:checks.length,realApi:'read-only',businessFlows:'browser contract fixtures'}))
}finally{await send('Page.close').catch(()=>{});ws.close()}
