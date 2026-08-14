(()=>{
window.CHS_THEME_VERSION='0.5.3';
const css=document.createElement('style');
css.textContent=`html.chs-light{--bg:#f5f2eb;--panel:#fff;--panel2:#f0ece3;--line:#d8d1c4;--gold:#9f742c;--muted:#6e6960;--text:#1c1b18;color-scheme:light}html.chs-light body{background:#f5f2eb;color:#1c1b18}html.chs-light .top{background:#faf8f3}html.chs-light aside{background:#f7f4ed}html.chs-light .nav{color:#494641}html.chs-light .nav.active{background:#e8e2d7;color:#111}html.chs-light .btn{background:#eee9df;color:#222;border-color:#cfc7ba}html.chs-light .btn.primary{background:#9f742c;color:#fff}html.chs-light input,html.chs-light select,html.chs-light textarea{background:#fff!important;color:#222!important}html.chs-light .syn{color:#504d47}html.chs-light .modal{background:#fff;color:#222}html.chs-light .field,html.chs-light .act,html.chs-light .scene,html.chs-light .claim,html.chs-light .setting{background:#f2eee6;color:#222}`;
document.head.appendChild(css);
function pref(){try{return NativeStore.get('ui_theme')||'system'}catch(e){return'system'}}
function resolved(v){if(v==='light'||v==='dark')return v;return window.matchMedia&&window.matchMedia('(prefers-color-scheme: light)').matches?'light':'dark'}
window.setChsTheme=function(v){if(!['system','light','dark'].includes(v))v='system';try{NativeStore.set('ui_theme',v)}catch(e){}document.documentElement.classList.toggle('chs-light',resolved(v)==='light');window.__chsThemePref=v};
setChsTheme(pref());
try{window.matchMedia('(prefers-color-scheme: light)').addEventListener('change',()=>{if(pref()==='system')setChsTheme('system')})}catch(e){}
const oldSettings=window.renderSettings;
if(typeof oldSettings==='function')window.renderSettings=function(m){oldSettings(m);if(m.querySelector('[data-theme-v053]'))return;const d=document.createElement('div');d.className='setting';d.dataset.themeV053='1';d.innerHTML='<h3>Aparência</h3><p class="small">Escolha como o CHS deve exibir a interface.</p><button class="btn" data-theme="system">Seguir sistema</button> <button class="btn" data-theme="light">Claro</button> <button class="btn" data-theme="dark">Escuro</button>';m.appendChild(d);d.querySelectorAll('[data-theme]').forEach(b=>b.onclick=()=>{setChsTheme(b.dataset.theme);try{render()}catch(e){}})};
const baseRender=window.render;
if(typeof baseRender==='function')window.render=function(){const r=baseRender();setChsTheme(pref());return r};
})();
