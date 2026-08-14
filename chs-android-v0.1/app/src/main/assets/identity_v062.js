(()=>{
window.CHS_VERSION='0.6.2';
function applyIdentity062(){
  const sub=document.querySelector('.sub');
  if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.6.2 · WRITER LITERÁRIO v2.1';
  const top=document.getElementById('topStatus');
  if(top)top.textContent='WRITER v2.1 · SALA + SHOWRUNNER + RECUPERAÇÃO AUTOMÁTICA';
}
applyIdentity062();
const baseRender=window.render;
if(typeof baseRender==='function')window.render=function(){const r=baseRender.apply(this,arguments);setTimeout(applyIdentity062,0);return r};
setInterval(applyIdentity062,1500);
})();
