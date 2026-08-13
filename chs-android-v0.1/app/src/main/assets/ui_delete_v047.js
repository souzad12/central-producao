(()=>{
window.CHS_UI_DELETE_VERSION='0.4.7';
const css=document.createElement('style');
css.textContent=`html,body{min-height:100%;}body{padding-bottom:26px!important}.top{margin-top:4px!important}.layout{padding-bottom:18px}.delete-row{display:flex;gap:8px;margin-top:10px;flex-wrap:wrap}.delete-row .btn{flex:1;min-width:150px}.danger-strong{border-color:#7a3d3d!important;color:#f1b0b0!important;background:#2b1e1e!important}`;
document.head.appendChild(css);

function findProject(code){return production.find(x=>x.code===code)}
function saveAll(){try{persist()}catch(e){console.warn(e)}try{render()}catch(e){console.warn(e)}}

window.clearWrittenScript=function(code){
 const p=findProject(code);if(!p)return flash('Episódio não encontrado em Produção/Finalizados.');
 const done=(p.scriptActs||[]).filter(Boolean).length;
 if(!done)return flash('Este episódio ainda não possui roteiro escrito.');
 const msg=`Apagar o roteiro escrito de ${code}?\n\nSerão apagados os atos gerados deste episódio no aplicativo. A preparação histórica será mantida e o episódio voltará para Em Produção.\n\nArquivos já salvos no Cofre NÃO serão apagados.`;
 if(!confirm(msg))return;
 p.scriptActs=[];
 delete p.scriptUpdatedAt;
 delete p.finalizedAt;
 p.stage='production';
 p.productionStartedAt=p.productionStartedAt||new Date().toISOString();
 p.scriptDeletedAt=new Date().toISOString();
 saveAll();
 try{closeModal()}catch{}
 flash('Roteiro apagado. A preparação foi mantida em Em Produção.');
};

window.returnEpisodeToBank=function(code){
 const i=production.findIndex(x=>x.code===code);if(i<0)return flash('Episódio não encontrado em Produção/Finalizados.');
 const msg=`Voltar ${code} ao Banco de Episódios?\n\nA preparação, roteiro e estado de produção serão removidos do aplicativo e a pauta voltará ao Banco para começar novamente.\n\nArquivos já salvos no Cofre NÃO serão apagados.`;
 if(!confirm(msg))return;
 production.splice(i,1);
 saveAll();
 try{closeModal()}catch{}
 flash('Episódio devolvido ao Banco de Episódios. O Cofre foi preservado.');
};

function decorateCards(){
 try{
  if(typeof tab==='undefined'||!['production','finalized'].includes(tab))return;
  document.querySelectorAll('article.card').forEach(card=>{
   if(card.querySelector('[data-v047-delete]'))return;
   const code=(card.querySelector('.code')?.textContent||'').trim();
   if(!code||!findProject(code))return;
   const row=document.createElement('div');row.className='delete-row';row.setAttribute('data-v047-delete','1');
   row.innerHTML=`<button class="btn danger-strong" onclick="clearWrittenScript('${code}')">🗑 Apagar roteiro</button><button class="btn" onclick="returnEpisodeToBank('${code}')">↩ Voltar ao Banco</button>`;
   card.appendChild(row);
  });
 }catch(e){console.warn('decorate delete',e)}
}

const oldRender=window.render;
if(typeof oldRender==='function')window.render=function(){const r=oldRender.apply(this,arguments);setTimeout(decorateCards,0);return r};

const oldWriter=window.openWriter;
if(typeof oldWriter==='function')window.openWriter=function(code){oldWriter(code);setTimeout(()=>{
 try{
  const m=document.querySelector('#overlay .modal');if(!m||m.querySelector('[data-v047-writer-delete]'))return;
  const row=document.createElement('div');row.className='delete-row';row.setAttribute('data-v047-writer-delete','1');
  row.innerHTML=`<button class="btn danger-strong" onclick="clearWrittenScript('${code}')">🗑 Apagar roteiro</button><button class="btn" onclick="returnEpisodeToBank('${code}')">↩ Remover produção e voltar ao Banco</button>`;
  m.appendChild(row);
 }catch(e){console.warn(e)}
},0)};

const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.7 · SAFE AREA + DELETE';
const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS';
try{render()}catch(e){}
})();
