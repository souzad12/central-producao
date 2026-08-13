(()=>{
window.CHS_DURATION_GUARD_VERSION='0.4.4';
function wc(t){return String(t||'').trim()?String(t).trim().split(/\s+/).length:0}
function actW(o){return (o&&o.scenes||[]).reduce((n,s)=>n+wc(s.ttsText||s.productionText||''),0)}
const old=window.onAiWritingResult;
window.onAiWritingResult=function(rid,ok,data){
 const req=window.__writerRequest;
 if(ok&&req&&req.rid===rid&&req.targetWords&&req.payload){
  try{
   const out=JSON.parse(data),n=actW(out),min=Math.max(350,Math.round(req.targetWords*0.85)),a=req.lengthAttempt||0;
   if(n<min&&a<2){
    const nr='expand_'+Date.now();
    const p=Object.assign({},req.payload,{expansionMode:true,currentDraft:out,minimumWords:min,targetWords:req.targetWords,lengthAttempt:a+1});
    window.__writerRequest=Object.assign({},req,{rid:nr,payload:p,lengthAttempt:a+1});
    overlay.innerHTML='<div class="overlay"><div class="modal loading"><div class="spinner"></div><h2>Expandindo ato curto</h2><p>'+n+' palavras; mínimo '+min+'.</p><p class="small">Correção automática '+(a+1)+'/2.</p></div></div>';
    NativeStore.writeAct(JSON.stringify(p),req.model||'deepseek-v4-flash',nr);return;
   }
   out.lengthAudit={actual:n,minimum:min,target:req.targetWords,attempts:a,status:n>=min?'OK':'SHORT'};data=JSON.stringify(out);
  }catch(e){console.error(e)}
 }
 return old(rid,ok,data);
};
const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.4 · DURAÇÃO CONTROLADA';
const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + IA + WRITER + TTS + LENGTH GUARD';
})();
