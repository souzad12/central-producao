(()=>{
window.CHS_VERSION='0.5.8';
function id058(){
  const sub=document.querySelector('.sub');
  if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.5.8 · CSV ROBUSTO';
  const top=document.getElementById('topStatus');
  if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS';
}
const oldRender=window.render;
if(typeof oldRender==='function')window.render=function(){const r=oldRender.apply(this,arguments);id058();return r};
id058();
})();
