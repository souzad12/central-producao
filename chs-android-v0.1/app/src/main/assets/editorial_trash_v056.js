(()=>{
window.CHS_EDITORIAL_TRASH_VERSION='0.5.6';

function isTrash(e){return !!(e&&e.editorialTrashed)}

// Migração apenas de estado: versões 0.5.x chegaram a guardar códigos separados.
try{
  const keys=['editorial_trash_codes_v055','editorial_trash_codes_v054'];
  const legacy=new Set();
  for(const k of keys){const raw=NativeStore.get(k);if(raw){const a=JSON.parse(raw);if(Array.isArray(a))a.forEach(c=>legacy.add(c))}}
  let changed=false;
  for(const e of catalog){if(legacy.has(e.code)&&!e.editorialTrashed){e.editorialTrashed=true;e.trashedAt=e.trashedAt||new Date().toISOString();changed=true}}
  if(changed)persist();
}catch(e){console.warn('Migração Lixeira',e)}

window.activeEditorialCatalog=()=>catalog.filter(e=>!isTrash(e));

window.moveTopicToTrash=function(code){
  const e=catalog.find(x=>x.code===code);
  if(!e)return flash('Pauta não encontrada.');
  if(production.some(p=>p.code===code))return flash('Este episódio já está em Produção ou Finalizados. Use os controles dessa etapa.');
  if(!confirm(`Mover ${code} para a Lixeira?\n\nEle sairá do Banco e será ignorado pelo Continuity Guard. O código não será renumerado.`))return;
  e.editorialTrashed=true;
  e.trashedAt=new Date().toISOString();
  persist();
  render();
  flash(`${code} movido para a Lixeira.`);
};

window.restoreTopicFromTrash=function(code){
  const e=catalog.find(x=>x.code===code);if(!e)return;
  e.editorialTrashed=false;delete e.trashedAt;
  persist();render();flash(`${code} restaurado ao Banco.`);
};

window.deleteTopicForever=function(code){
  const e=catalog.find(x=>x.code===code);if(!e)return;
  if(!confirm(`Excluir ${code} definitivamente?\n\nA pauta será removida do catálogo deste aparelho. O código não será reaproveitado automaticamente.`))return;
  const i=catalog.findIndex(x=>x.code===code);if(i>=0)catalog.splice(i,1);
  persist();render();flash(`${code} excluído definitivamente.`);
};

if(typeof window.buildFutureGuard==='function'){
  const oldFuture=window.buildFutureGuard;
  window.buildFutureGuard=function(target){
    const original=catalog;
    try{catalog=original.filter(x=>!isTrash(x));return oldFuture(target)}finally{catalog=original}
  };
}

function ensureNav(){
  const aside=document.querySelector('aside');if(!aside)return;
  let b=aside.querySelector('[data-trash-nav]');
  if(!b){
    b=document.createElement('button');b.className='nav';b.dataset.trashNav='1';
    b.onclick=()=>{tab='trash';render()};
    const cfg=[...aside.querySelectorAll('.nav')].find(x=>/Configura/i.test(x.textContent||''));
    cfg?aside.insertBefore(b,cfg):aside.appendChild(b);
  }
  b.innerHTML=`🗑 Lixeira <span style="opacity:.65">(${catalog.filter(isTrash).length})</span>`;
  b.classList.toggle('active',tab==='trash');
}

function renderTrash(){
  const main=document.querySelector('main');if(!main)return;
  const items=catalog.filter(isTrash);
  main.innerHTML=`<section class="hero"><div><h1>Lixeira Editorial</h1><p>Pautas removidas do Banco e ignoradas pela sequência editorial.</p></div></section>${items.length?`<div class="grid" style="margin-top:18px">${items.map(e=>`<article class="card"><span class="code">${esc(e.code)}</span><h3>${esc(e.title||e.theme||'Pauta')}</h3><p class="syn">${esc(e.synopsis||'')}</p><p class="small">${e.trashedAt?'Movido em '+new Date(e.trashedAt).toLocaleString('pt-BR'):''}</p><div class="actions"><button class="btn primary" onclick="restoreTopicFromTrash('${e.code}')">Restaurar</button><button class="btn danger" onclick="deleteTopicForever('${e.code}')">Excluir definitivamente</button></div></article>`).join('')}</div>`:`<div class="empty" style="margin-top:18px">A Lixeira está vazia.</div>`}`;
}

function decorateBankTrashButtons(){
  const bankNow=/Banco de Episódios/i.test(document.querySelector('main h1')?.textContent||'');
  if(!bankNow)return;
  document.querySelectorAll('main .card').forEach(card=>{
    const code=card.querySelector('.code')?.textContent?.trim();
    if(!code||production.some(p=>p.code===code)||card.querySelector('[data-trash-topic]'))return;
    const actions=card.querySelector('.actions');if(!actions)return;
    const b=document.createElement('button');
    b.type='button';
    b.className='btn danger';
    b.dataset.trashTopic='1';
    b.textContent='🗑';
    b.title='Mover pauta para a Lixeira';
    b.setAttribute('aria-label','Mover '+code+' para a Lixeira');
    // MECANISMO COMPROVADO DA v0.4.8: handler direto no elemento real do DOM.
    b.onclick=()=>moveTopicToTrash(code);
    actions.appendChild(b);
  });
}

const oldRender=window.render;
window.render=function(){
  if(tab==='trash'){ensureNav();renderTrash();return}
  const original=catalog;
  const heading=document.querySelector('main h1')?.textContent||'';
  const bank=tab==='catalog'||tab==='bank'||/Banco de Episódios/i.test(heading);
  try{if(bank)catalog=original.filter(e=>!isTrash(e));oldRender.apply(this,arguments)}finally{catalog=original}
  ensureNav();
  decorateBankTrashButtons();
};

const oldPrep=window.prepare;
if(typeof oldPrep==='function')window.prepare=function(code){const e=catalog.find(x=>x.code===code);if(e?.editorialTrashed)return flash('Restaure esta pauta da Lixeira antes de prepará-la.');return oldPrep(code)};

try{render()}catch(e){console.warn('Lixeira 0.5.6',e)}
})();
