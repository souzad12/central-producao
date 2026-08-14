(()=>{
window.CHS_LITERARY_GUARDRAILS='0.6.1';

// Preserva instruções livres da preparação para todas as etapas futuras do Writer v2.
if(typeof window.startAiPreparation==='function'){
  const baseStartPrep=window.startAiPreparation;
  window.startAiPreparation=function(code){
    try{
      const notes=document.getElementById('ai_notes')?.value?.trim()||'';
      window.__litV2PendingPrepNotes={code,notes};
    }catch{}
    return baseStartPrep.apply(this,arguments);
  };
}
if(typeof window.onAiPreparationResult==='function'){
  const basePrepResult=window.onAiPreparationResult;
  window.onAiPreparationResult=function(rid,ok,data){
    const pending=window.__litV2PendingPrepNotes?{...window.__litV2PendingPrepNotes}:null;
    const ret=basePrepResult.apply(this,arguments);
    if(ok&&pending?.code){
      setTimeout(()=>{
        try{
          const p=production.find(x=>x.code===pending.code);
          if(!p)return;
          if(pending.notes){
            p.userNotes=pending.notes;
            p.editorialConstraints=Array.isArray(p.editorialConstraints)?p.editorialConstraints:[];
            if(!p.editorialConstraints.includes(pending.notes))p.editorialConstraints.push(pending.notes);
          }
          persist();
        }catch(e){console.warn('CHS v0.6.1 constraints',e)}
      },50);
    }
    return ret;
  };
}

function cleanupOldStudio(code){
  const m=document.querySelector('#overlay .modal');
  if(!m)return;
  const old=m.querySelector('[data-writers-studio]');
  if(!old)return;
  const h=old.querySelector('h3');if(h)h.textContent='🎬 Ferramentas de Cena';
  const p=old.querySelector('p.small');if(p)p.textContent='Ferramentas pontuais para continuar, redirigir ou mudar o foco de uma cena já escrita. A arquitetura e a Sala automática agora pertencem ao Writer Literário v2.';
  old.querySelectorAll('label').forEach(l=>{if(/Sala automática/i.test(l.textContent||''))l.style.display='none'});
  old.querySelectorAll('button').forEach(b=>{if(/Sala de Roteiristas/i.test(b.textContent||''))b.textContent='🎭 Debater ato (v2)'});
}
if(typeof window.openWriter==='function'){
  const baseOpen=window.openWriter;
  window.openWriter=function(code){const r=baseOpen.apply(this,arguments);setTimeout(()=>cleanupOldStudio(code),1);return r};
}

// Diagnóstico silencioso de regressão: não altera os módulos funcionais.
setTimeout(()=>{
  try{
    window.CHS_REGRESSION_GUARD={
      trash:typeof window.moveTopicToTrash==='function',
      csv:!!window.CSVBridge,
      literary:!!window.LiteraryEngine
    };
    if(!window.CHS_REGRESSION_GUARD.trash)console.warn('CHS: módulo de Lixeira não detectado.');
    if(!window.CHS_REGRESSION_GUARD.csv)console.warn('CHS: ponte CSV não detectada.');
  }catch{}
},500);
})();
