(()=>{
window.CHS_UI_VERSION='0.5.6';
window.CHS_VERSION='0.5.6';

const css=document.createElement('style');
css.textContent=`
html.chs-light{--bg:#f6f2e8;--panel:#fff;--panel2:#f0eadf;--line:#d7cec0;--gold:#9a6b16;--muted:#615d55;--text:#1d1b18;color-scheme:light}
html.chs-light body{background:var(--bg)!important;color:var(--text)!important}
html.chs-light .top{background:#fbf8f1!important;border-color:var(--line)!important;color:#1d1b18!important}
html.chs-light aside{background:#f7f2e8!important;border-color:var(--line)!important}
html.chs-light main,html.chs-light .layout{background:var(--bg)!important;color:#1d1b18!important}
html.chs-light .nav{color:#4c4841!important}html.chs-light .nav.active,html.chs-light .nav:hover{background:#e8e0d2!important;color:#171512!important}
html.chs-light .card,html.chs-light .metric,html.chs-light .setting,html.chs-light .modal,html.chs-light .field,html.chs-light .act,html.chs-light .scene,html.chs-light .claim{background:#fff!important;color:#211f1b!important;border-color:#d8d0c4!important}
html.chs-light .syn{color:#4d4942!important}html.chs-light .meta,html.chs-light .small,html.chs-light .footer-note{color:#5d584f!important}
html.chs-light .code,html.chs-light .label,html.chs-light .field b{color:#8a5e12!important}
html.chs-light .ok{color:#23733d!important}html.chs-light .warning{color:#805d00!important}
html.chs-light .btn{background:#eee8dc!important;color:#25211c!important;border-color:#d1c7b8!important}
html.chs-light .btn.primary,html.chs-light .filebtn{background:#ad7919!important;color:#fff!important;border-color:#ad7919!important}
html.chs-light .btn.danger{background:#fff5f3!important;color:#8f2e27!important;border-color:#d39d98!important}
html.chs-light input,html.chs-light select,html.chs-light textarea{background:#fff!important;color:#211f1b!important;border-color:#aaa196!important}
html.chs-light input:disabled,html.chs-light select:disabled,html.chs-light textarea:disabled{background:#eee9e0!important;color:#514c45!important;opacity:1!important}
html.chs-light input::placeholder,html.chs-light textarea::placeholder{color:#756e64!important;opacity:1!important}
html.chs-light .provider-grid{color:#211f1b!important}
html.chs-light .provider-card{background:#fff!important;color:#211f1b!important;border-color:#cfc5b8!important}
html.chs-light .provider-card h4,html.chs-light .provider-card p,html.chs-light .provider-card .small,html.chs-light .provider-card label{color:#3f3b35!important}
html.chs-light .cost-box{background:#e8f4eb!important;color:#153d22!important;border-color:#a8cdb0!important}
html.chs-light .cost-box b,html.chs-light .cost-box p,html.chs-light .cost-box .small{color:#1d4a2a!important}
html.chs-light .future-box{background:#edf3f9!important;color:#213849!important;border-color:#b9cad8!important}
html.chs-light .provider-pill{background:#e4edf7!important;color:#24486d!important}
html.chs-light .stage-final{background:#e7f3e9!important;color:#235f34!important}
html.chs-light .vault-ok{color:#216839!important}
html.chs-light .toast{background:#fff!important;color:#25211c!important;border-color:#aaa196!important;box-shadow:0 4px 18px #0002!important}
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
  m.innerHTML=m.innerHTML.replace(/v0\.5\.0 STABLE/g,'v0.5.6 STABLE').replace(/CHS v0\.5\.0/g,'CHS v0.5.6').replace(/v0\.5\.5/g,'v0.5.6');
  if(!m.querySelector('[data-theme-v056]')){
    const d=document.createElement('div');d.className='setting';d.dataset.themeV056='1';
    d.innerHTML='<h3>Aparência</h3><p class="small">Escolha o tema da interface.</p><button class="btn" data-theme="system">Seguir sistema</button> <button class="btn" data-theme="light">Claro</button> <button class="btn" data-theme="dark">Escuro</button>';
    m.appendChild(d);d.querySelectorAll('[data-theme]').forEach(b=>b.onclick=()=>{setChsTheme(b.dataset.theme);render()});
  }
};

const baseRender=window.render;
if(typeof baseRender==='function')window.render=function(){const r=baseRender.apply(this,arguments);setChsTheme(themePref());setIdentity();return r};
function setIdentity(){const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.5.6 · NOVELA HISTÓRICA';const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS'}
setIdentity();try{render()}catch(e){console.warn('CHS theme 0.5.6',e)}
})();
