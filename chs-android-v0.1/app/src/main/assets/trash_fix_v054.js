(()=>{
window.CHS_TRASH_FIX_VERSION='0.5.4';
const K='editorial_trash_codes_v054';
function loadSet(){try{const r=NativeStore.get(K);return new Set(r?JSON.parse(r):[])}catch(e){return new Set()}}
let set=loadSet();
function save(){const r=JSON.stringify([...set]);try{NativeStore.set(K,r)}catch(e){}try{localStorage.setItem('chs_'+K,r)}catch(e){}}
function sync(){for(const x of catalog){if(x?.editorialTrashed)set.add(x.code);if(set.has(x.code))x.editorialTrashed=true}save()}
function trashed(x){return !!(x&&(x.editorialTrashed||set.has(x.code)))}
window.moveTopicToTrash=function(code){sync();const x=catalog.find(v=>v.code===code);if(!x)return flash('Pauta não encontrada.');if(production.some(p=>p.code===code))return flash('Esta pauta já entrou em Produção. Use “Voltar ao Banco” antes de excluí-la.');if(!confirm(`Mover ${code} para a Lixeira?\n\nEla sairá do Banco de Ideias e será ignorada pela sequência editorial.`))return;set.add(code);x.editorialTrashed=true;x.trashedAt=new Date().toISOString();save();persist();render();flash(`${code} movido para a Lixeira.`)};
window.restoreTopicFromTrash=function(code){const x=catalog.find(v=>v.code===code);if(!x)return;set.delete(code);x.editorialTrashed=false;delete x.trashedAt;save();persist();tab='catalog';render();flash(`${code} restaurado ao Banco.`)};
window.deleteTopicForever=function(code){const i=catalog.findIndex(v=>v.code===code);if(i<0)return;if(!confirm(`Excluir ${code} definitivamente deste aparelho?\n\nO código não será renumerado.`))return;catalog.splice(i,1);set.delete(code);save();persist();render();flash(`${code} excluído definitivamente.`)};
const oldCard=window.card;
if(typeof oldCard==='function')window.card=function(x){let h=oldCard(x);if(trashed(x)||production.some(p=>p.code===x.code))return h;const b=`<button type="button" class="btn danger" onclick="moveTopicToTrash('${x.code}')">🗑 Excluir pauta</button>`;return h.replace('</div></article>',b+'</div></article>')};
const oldCatalog=window.renderCatalog;
if(typeof oldCatalog==='function')window.renderCatalog=function(m){sync();const all=catalog;catalog=all.filter(x=>!trashed(x));try{return oldCatalog(m)}finally{catalog=all}};
const oldDetail=window.detail;
if(typeof oldDetail==='function')window.detail=function(code){oldDetail(code);setTimeout(()=>{const x=catalog.find(v=>v.code===code),m=document.querySelector('#overlay .modal');if(!x||!m||trashed(x)||production.some(p=>p.code===code)||m.querySelector('[data-trash-v054]'))return;const b=document.createElement('button');b.className='btn danger';b.dataset.trashV054='1';b.textContent='🗑 Excluir pauta do Banco';b.onclick=()=>moveTopicToTrash(code);(m.querySelector('.actions')||m).appendChild(b)},0)};
function nav(){const a=document.querySelector('aside');if(!a)return;let b=a.querySelector('[data-trash-v054-nav]')||a.querySelector('[data-trash-nav]');if(!b){b=document.createElement('button');b.className='nav';b.dataset.trashV054Nav='1';a.appendChild(b)}b.dataset.tab='trash';b.innerHTML=`🗑 Lixeira <span style="opacity:.65">(${catalog.filter(trashed).length})</span>`;b.onclick=()=>{tab='trash';render()}}
function page(){const m=document.getElementById('main'),a=catalog.filter(trashed);m.innerHTML=`<section class="hero"><div><h1>Lixeira Editorial</h1><p>Pautas removidas do Banco de Ideias.</p></div></section>${a.length?`<div class="grid" style="margin-top:18px">${a.map(x=>`<article class="card"><span class="code">${esc(x.code)}</span><h3>${esc(x.title||x.theme||'Pauta')}</h3><p class="syn">${esc(x.synopsis||'')}</p><div class="actions"><button class="btn primary" onclick="restoreTopicFromTrash('${x.code}')">Restaurar</button><button class="btn danger" onclick="deleteTopicForever('${x.code}')">Excluir definitivamente</button></div></article>`).join('')}</div>`:`<div class="empty" style="margin-top:18px">A Lixeira está vazia.</div>`}`}
const oldRender=window.render;
window.render=function(){sync();nav();if(tab==='trash'){document.querySelectorAll('.nav').forEach(x=>x.classList.toggle('active',x.dataset.tab==='trash'));page();nav();return}const r=oldRender.apply(this,arguments);nav();return r};
sync();try{render()}catch(e){console.warn('trash fix 0.5.4',e)}
})();
