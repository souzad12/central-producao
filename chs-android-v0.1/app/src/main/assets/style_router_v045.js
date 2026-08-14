(()=>{
  window.CHS_STYLE_ROUTER_VERSION='0.5.3';
  function styleOf(p){return p&&p.narrativeStyle&&typeof p.narrativeStyle==='object'?p.narrativeStyle:null}
  function pretty(x){return String(x||'').replaceAll('_',' ').toLowerCase().replace(/(^|\s)\S/g,m=>m.toUpperCase())}
  function styleBox(p){
    const s=styleOf(p), posture=p?.researchPosture;
    if(!s)return `<div class="setting"><b>🎭 Novela Histórica Reconstruída</b><p class="small">Preparação antiga: o Writer usará drama histórico reconstruído como base.</p></div>`;
    const primary=s.primaryLabel||pretty(s.primary||'DRAMA_HISTORICO_RECONSTRUIDO');
    const secondary=s.secondaryLabel||pretty(s.secondary||'');
    const traits=Array.isArray(s.traits)?s.traits.join(' · '):'';
    const guards=Array.isArray(s.guardrails)?s.guardrails.join(' · '):'';
    return `<div class="setting" data-style-router="1"><b>🎭 Motor Dramático · AUTO</b><p><b>${esc(primary)}</b> · intensidade ${Number(s.intensity||3)}/5</p>${secondary?`<p class="small"><b>Lente literária:</b> ${esc(secondary)}</p>`:''}${s.whyItFits?`<p class="small">${esc(s.whyItFits)}</p>`:''}${traits?`<p class="small"><b>Traços:</b> ${esc(traits)}</p>`:''}${guards?`<p class="small"><b>Limites:</b> ${esc(guards)}</p>`:''}${posture?`<p class="small"><b>Rigor historiográfico:</b> ${esc(posture.mode||'BACKGROUND_GUARD')} — ${esc(posture.reason||'atua em segundo plano')}</p>`:''}<p class="ok">● A superfície narrativa deve permanecer uma novela histórica reconstruída.</p></div>`
  }
  const oldShow=window.showPreparation;
  if(typeof oldShow==='function')window.showPreparation=function(code){oldShow(code);setTimeout(()=>{try{const p=production.find(x=>x.code===code),m=document.querySelector('#overlay .modal');if(!p||!m||m.querySelector('[data-style-router]'))return;const d=document.createElement('div');d.innerHTML=styleBox(p);const actions=m.querySelector('.actions');actions?m.insertBefore(d.firstElementChild,actions):m.appendChild(d.firstElementChild)}catch(e){console.warn(e)}},0)};
  const oldWriter=window.openWriter;
  if(typeof oldWriter==='function')window.openWriter=function(code){oldWriter(code);setTimeout(()=>{try{const p=production.find(x=>x.code===code),m=document.querySelector('#overlay .modal');if(!p||!m||m.querySelector('[data-style-router]'))return;const d=document.createElement('div');d.innerHTML=styleBox(p);const stats=m.querySelector('.writer-stat');stats?stats.insertAdjacentElement('afterend',d.firstElementChild):m.appendChild(d.firstElementChild)}catch(e){console.warn(e)}},0)};
  const oldSettings=window.renderSettings;
  if(typeof oldSettings==='function')window.renderSettings=function(m){oldSettings(m);if(m.querySelector('[data-style-auto-settings]'))return;const d=document.createElement('div');d.className='setting';d.dataset.styleAutoSettings='1';d.innerHTML='<h3>🎭 Dramaturgia AUTO</h3><p class="ok">● Novela Histórica Reconstruída como base</p><p>O Showrunner escolhe o motor dramático e, quando ajuda, uma lente literária. A lente Dostoievskiana volta a ser preferida para conflitos humanos, morais e psicológicos sustentáveis.</p><p class="small">Arqueologia, crítica de fontes e debates acadêmicos atuam como guardrails; só dominam a superfície quando a pauta for explicitamente historiográfica.</p>';m.appendChild(d)};
})();