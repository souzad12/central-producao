(()=>{
window.CHS_UI_VERSION='0.5.5';
window.CHS_VERSION='0.5.5';

const TRASH_KEY='editorial_trash_codes_v055';
const LEGACY_TRASH_KEY='editorial_trash_codes_v054';
let trashCodes=new Set();

function readJsonKey(key){try{const raw=NativeStore.get(key);return raw?JSON.parse(raw):null}catch(e){return null}}
function loadTrash(){
  const current=readJsonKey(TRASH_KEY),legacy=readJsonKey(LEGACY_TRASH_KEY);
  trashCodes=new Set(Array.isArray(current)?current:(Array.isArray(legacy)?legacy:[]));
  for(const x of catalog){if(x&&x.editorialTrashed)trashCodes.add(x.code)}
  syncTrashFlags();
}
function saveTrash(){
  const raw=JSON.stringify([...trashCodes]);
  try{NativeStore.set(TRASH_KEY,raw)}catch(e){}
  try{localStorage.setItem('chs_'+TRASH_KEY,raw)}catch(e){}
}
function syncTrashFlags(){
  for(const x of catalog){if(!x)continue;x.editorialTrashed=trashCodes.has(x.code);if(!x.editorialTrashed)delete x.trashedAt}
  saveTrash();
}
function isTrashed(x){return !!(x&&trashCodes.has(x.code))}

window.moveTopicToTrash=function(code){
  const x=catalog.find(v=>v.code===code);
  if(!x)return flash('Pauta não encontrada.');
  if(production.some(p=>p.code===code))return flash('Esta pauta já entrou em Produção. Use “Voltar ao Banco” antes de movê-la para a Lixeira.');
  if(!confirm(`Mover ${code} para a Lixeira?\n\nEla sairá do Banco de Ideias e será ignorada pela sequência editorial.`))return;
  trashCodes.add(code);x.editorialTrashed=true;x.trashedAt=new Date().toISOString();
  saveTrash();persist();render();flash(`${code} movido para a Lixeira.`);
};
window.restoreTopicFromTrash=function(code){
  const x=catalog.find(v=>v.code===code);if(!x)return;
  trashCodes.delete(code);x.editorialTrashed=false;delete x.trashedAt;
  saveTrash();persist();render();flash(`${code} restaurado ao Banco.`);
};
window.deleteTopicForever=function(code){
  const i=catalog.findIndex(v=>v.code===code);if(i<0)return;
  if(!confirm(`Excluir ${code} definitivamente deste aparelho?\n\nO código não será renumerado.`))return;
  catalog.splice(i,1);trashCodes.delete(code);saveTrash();persist();render();flash(`${code} excluído definitivamente.`);
};

const baseCard=window.card;
if(typeof baseCard==='function')window.card=function(x){
  if(isTrashed(x))return '';
  let html=baseCard(x);
  if(production.some(p=>p.code===x.code))return html;
  const button=`<button type="button" class="btn danger chs-trash-only" title="Mover para a Lixeira" aria-label="Mover ${esc(x.code)} para a Lixeira" onclick="moveTopicToTrash('${x.code}')">🗑</button>`;
  return html.replace('</div></article>',button+'</div></article>');
};

const baseRenderCatalog=window.renderCatalog;
if(typeof baseRenderCatalog==='function')window.renderCatalog=function(m){
  syncTrashFlags();
  const full=catalog;catalog=full.filter(x=>!isTrashed(x));
  try{return baseRenderCatalog(m)}finally{catalog=full}
};

function ensureTrashNav(){
  const aside=document.querySelector('aside');if(!aside)return;
  let b=aside.querySelector('[data-v055-trash-nav]');
  if(!b){b=document.createElement('button');b.className='nav';b.dataset.v055TrashNav='1';b.dataset.tab='trash';const cfg=[...aside.querySelectorAll('.nav')].find(x=>/Configura/i.test(x.textContent||''));cfg?aside.insertBefore(b,cfg):aside.appendChild(b)}
  b.innerHTML=`🗑 Lixeira <span style="opacity:.68">(${catalog.filter(isTrashed).length})</span>`;
  b.classList.toggle('active',tab==='trash');
  b.onclick=()=>{tab='trash';render()};
}
function renderTrash(){
  const m=document.getElementById('main'),items=catalog.filter(isTrashed);
  m.innerHTML=`<section class="hero"><div><h1>Lixeira Editorial</h1><p>Pautas removidas do Banco de Ideias e ignoradas pela sequência editorial.</p></div></section>${items.length?`<section class="grid" style="margin-top:18px">${items.map(x=>`<article class="card"><span class="code">${esc(x.code)}</span><h3>${esc(x.title||x.theme||'Pauta')}</h3><p class="syn">${esc(x.synopsis||'')}</p><div class="actions"><button class="btn primary" onclick="restoreTopicFromTrash('${x.code}')">Restaurar</button><button class="btn danger" onclick="deleteTopicForever('${x.code}')">Excluir definitivamente</button></div></article>`).join('')}</section>`:`<div class="empty" style="margin-top:18px">A Lixeira está vazia.</div>`}`;
}

const css=document.createElement('style');
css.textContent=`
.chs-trash-only{flex:0 0 54px!important;min-width:54px!important;font-size:18px!important;padding-left:10px!important;padding-right:10px!important}
html.chs-light{--bg:#f6f2e8;--panel:#fff;--panel2:#f0eadf;--line:#d7cec0;--gold:#9a6b16;--muted:#615d55;--text:#1d1b18;color-scheme:light}
html.chs-light body{background:var(--bg)!important;color:var(--text)!important}
html.chs-light .top{background:#fbf8f1!important;border-color:var(--line)!important;color:#1d1b18!important}
html.chs-light aside{background:#f7f2e8!important;border-color:var(--line)!important}
html.chs-light main,html.chs-light .layout{background:var(--bg)!important;color:#1d1b18!important}
html.chs-light .nav{color:#4c4841!important}html.chs-light .nav.active,html.chs-light .nav:hover{background:#e8e0d2!important;color:#171512!important}
html.chs-light .card,html.chs-light .metric,html.chs-light .setting,html.chs-light .modal,html.chs-light .field,html.chs-light .act,html.chs-light .scene,html.chs-light .claim{background:#fff!important;color:#211f1b!important;border-color:#d8d0c4!important}
html.chs-light .syn{color:#4d4942!important}html.chs-light .meta,html.chs-light .small,html.chs-light .footer-note{color:#666057!important}
html.chs-light .code,html.chs-light .label,html.chs-light .field b{color:#8a5e12!important}
html.chs-light .ok{color:#23733d!important}html.chs-light .warning{color:#8b6500!important}
html.chs-light .btn{background:#eee8dc!important;color:#25211c!important;border-color:#d1c7b8!important}
html.chs-light .btn.primary,html.chs-light .filebtn{background:#ad7919!important;color:#fff!important;border-color:#ad7919!important}
html.chs-light .btn.danger{background:#fff5f3!important;color:#9b322b!important;border-color:#d9aaa6!important}
html.chs-light input,html.chs-light select,html.chs-light textarea{background:#fff!important;color:#211f1b!important;border-color:#bdb4a8!important}
html.chs-light input:disabled,html.chs-light select:disabled,html.chs-light textarea:disabled{background:#f0ece4!important;color:#5d574f!important;opacity:1!important}
html.chs-light input::placeholder,html.chs-light textarea::placeholder{color:#817a70!important;opacity:1!important}
html.chs-light .provider-grid{color:#211f1b!important}
html.chs-light .provider-card{background:#fff!important;color:#211f1b!important;border-color:#d6cdc0!important}
html.chs-light .provider-card h4,html.chs-light .provider-card p,html.chs-light .provider-card .small,html.chs-light .provider-card label{color:#4d4942!important}
html.chs-light .cost-box{background:#edf7ef!important;color:#1e4c2a!important;border-color:#b8d7be!important}
html.chs-light .cost-box b,html.chs-light .cost-box p,html.chs-light .cost-box .small{color:#285635!important}
html.chs-light .future-box{background:#eef4fa!important;color:#24394c!important;border-color:#bdcedd!important}
html.chs-light .provider-pill{background:#e6eef8!important;color:#274a72!important}
html.chs-light .stage-final{background:#e9f5eb!important;color:#27683a!important}
html.chs-light .vault-ok{color:#23733d!important}
html.chs-light .toast{background:#fff!important;color:#25211c!important;border-color:#bdb4a8!important;box-shadow:0 4px 18px #0002!important}
html.chs-light .overlay{background:#0007!important}
`;
document.head.appendChild(css);

function themePref(){try{return NativeStore.get('ui_theme')||'system'}catch(e){return 'system'}}
function resolvedTheme(v){if(v==='light'||v==='dark')return v;return window.matchMedia&&window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark'}
window.setChsTheme=function(v){if(!['system','light','dark'].includes(v))v='system';try{NativeStore.set('ui_theme',v)}catch(e){}document.documentElement.classList.toggle('chs-light',resolvedTheme(v)==='light');window.__chsThemePref=v};
setChsTheme(themePref());
try{window.matchMedia('(prefers-color-scheme: light)').addEventListener('change',()=>{if(themePref()==='system')setChsTheme('system')})}catch(e){}

const baseSettings=window.renderSettings;
if(typeof baseSettings==='function')window.renderSettings=function(m){
  baseSettings(m);
  m.innerHTML=m.innerHTML.replace(/v0\.5\.0 STABLE/g,'v0.5.5 STABLE').replace(/CHS v0\.5\.0/g,'CHS v0.5.5');
  if(!m.querySelector('[data-theme-v055]')){const d=document.createElement('div');d.className='setting';d.dataset.themeV055='1';d.innerHTML='<h3>Aparência</h3><p class="small">Escolha o tema da interface.</p><button class="btn" data-theme="system">Seguir sistema</button> <button class="btn" data-theme="light">Claro</button> <button class="btn" data-theme="dark">Escuro</button>';m.appendChild(d);d.querySelectorAll('[data-theme]').forEach(b=>b.onclick=()=>{setChsTheme(b.dataset.theme);render()})}
};

const basePrepare=window.prepare;
if(typeof basePrepare==='function')window.prepare=function(code){if(trashCodes.has(code))return flash('Restaure esta pauta da Lixeira antes de prepará-la.');return basePrepare(code)};

const baseRender=window.render;
window.render=function(){
  syncTrashFlags();setChsTheme(themePref());ensureTrashNav();
  if(tab==='trash'){document.querySelectorAll('.nav').forEach(b=>b.classList.toggle('active',b.dataset.tab==='trash'));renderTrash();ensureTrashNav();setIdentity();return}
  const r=baseRender.apply(this,arguments);ensureTrashNav();setChsTheme(themePref());setIdentity();return r;
};
function setIdentity(){const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.5.5 · NOVELA HISTÓRICA';const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS'}

loadTrash();setIdentity();try{render()}catch(e){console.warn('CHS UI 0.5.5',e)}
})();
