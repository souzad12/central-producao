(()=>{
window.CHS_VERSION='0.5.7';
function id057(){
  const sub=document.querySelector('.sub');
  if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.5.7 · JS DIALOGS FIX';
  const top=document.getElementById('topStatus');
  if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS';
}
const oldRender=window.render;
if(typeof oldRender==='function')window.render=function(){const r=oldRender.apply(this,arguments);id057();return r};
id057();
})();
