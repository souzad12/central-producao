(()=>{
window.CHS_AUTO_WRITER_VERSION='0.5.3';
let autoState=null;
let autoTimer=null;

function firstMissing(p){
  const total=(p?.acts||[]).length||6;
  for(let i=0;i<total;i++) if(!(p.scriptActs||[])[i]) return i;
  return -1;
}
function actStamp(p,i){
  const a=p?.scriptActs?.[i];
  return a?.generatedAt||a?.updatedAt||'';
}
function stopWatch(){
  if(autoTimer){ clearInterval(autoTimer); autoTimer=null; }
}
function finishAuto(){
  const code=autoState?.code;
  stopWatch(); autoState=null;
  window.__writerAuto=false; window.__autoWriterSession=null;
  if(code){ try{ openWriter(code); }catch(e){} }
  try{ flash('Escrita automática concluída.'); }catch(e){}
}
function watchSaved(index,baseline){
  stopWatch();
  autoTimer=setInterval(()=>{
    if(!autoState?.active){ stopWatch(); return; }
    const p=production.find(x=>x.code===autoState.code);
    if(!p){ stopAutoWriter(); return; }
    const current=actStamp(p,index);
    if(current && current!==baseline){
      stopWatch();
      try{ flash(`Ato ${index+1} salvo. Continuando automaticamente...`); }catch(e){}
      setTimeout(runAutoStep,700);
    }
  },1000);
}
function runAutoStep(){
  if(!autoState?.active) return;
  const p=production.find(x=>x.code===autoState.code);
  if(!p){ stopAutoWriter(); return; }
  const index=firstMissing(p);
  if(index<0){ finishAuto(); return; }
  autoState.index=index;
  const baseline=actStamp(p,index);
  window.__writerAuto=false;
  window.__autoWriterSession=null;
  watchSaved(index,baseline);
  try{ writeSpecificAct(autoState.code,index,false); }
  catch(e){ stopAutoWriter(); try{ flash('Automação interrompida: '+(e?.message||e)); }catch(x){} }
}
window.startAutoWriter=function(code){
  const p=production.find(x=>x.code===code);
  if(!p) return flash('Episódio não encontrado.');
  if(!confirm('Escrever automaticamente todos os atos restantes? O próximo ato só começará quando o atual estiver salvo.')) return;
  autoState={code,active:true,index:firstMissing(p)};
  window.__writerAuto=false; window.__autoWriterSession=null;
  try{ flash('Escrita automática iniciada.'); }catch(e){}
  runAutoStep();
};
window.stopAutoWriter=function(){
  if(autoState) autoState.active=false;
  stopWatch(); autoState=null;
  window.__writerAuto=false; window.__autoWriterSession=null;
  try{ flash('Escrita automática interrompida.'); }catch(e){}
};
})();