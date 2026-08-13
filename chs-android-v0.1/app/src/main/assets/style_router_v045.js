(()=>{
  window.CHS_STYLE_ROUTER_VERSION='0.4.5';
  function styleOf(p){return p&&p.narrativeStyle&&typeof p.narrativeStyle==='object'?p.narrativeStyle:null}
  function label(s){if(!s)return 'Dramaturgia histórica sóbria';const a=s.primaryLabel||s.primary||'Dramaturgia histórica';const b=s.secondaryLabel||s.secondary||'';return b?`${a} + ${b}`:a}
  function styleBox(p){const s=styleOf(p);if(!s)return `<div class="setting"><b>🎭 Motor de Estilo</b><p class="small">Preparação antiga sem Style Router. Será usada dramaturgia histórica sóbria.</p></div>`;const traits=Array.isArray(s.traits)?s.traits.join(' · '):'';const why=s.whyItFits||'';const guards=Array.isArray(s.guardrails)?s.guardrails.join(' · '):'';return `<div class="setting" data-style-router="1"><b>🎭 Motor de Estilo · AUTO</b><p><b>${esc(label(s))}</b> · intensidade ${Number(s.intensity||3)}/5</p>${why?`<p class="small">${esc(why)}</p>`:''}${traits?`<p class="small"><b>Traços:</b> ${esc(traits)}</p>`:''}${guards?`<p class="small"><b>Limites:</b> ${esc(guards)}</p>`:''}<p class="ok">● Escolhido automaticamente para esta pauta.</p></div>`}

  const oldShow=window.showPreparation;
  if(typeof oldShow==='function')window.showPreparation=function(code){oldShow(code);setTimeout(()=>{try{const p=production.find(x=>x.code===code),m=document.querySelector('#overlay .modal');if(!p||!m||m.querySelector('[data-style-router]'))return;const d=document.createElement('div');d.innerHTML=styleBox(p);const box=d.firstElementChild;const actions=m.querySelector('.actions');actions?m.insertBefore(box,actions):m.appendChild(box)}catch(e){console.warn(e)}},0)};

  const oldWriter=window.openWriter;
  if(typeof oldWriter==='function')window.openWriter=function(code){oldWriter(code);setTimeout(()=>{try{const p=production.find(x=>x.code===code),m=document.querySelector('#overlay .modal');if(!p||!m||m.querySelector('[data-style-router]'))return;const d=document.createElement('div');d.innerHTML=styleBox(p);const box=d.firstElementChild;const stats=m.querySelector('.writer-stat');stats?stats.insertAdjacentElement('afterend',box):m.appendChild(box)}catch(e){console.warn(e)}},0)};

  const oldSettings=window.renderSettings;
  if(typeof oldSettings==='function')window.renderSettings=function(m){oldSettings(m);if(m.querySelector('[data-style-auto-settings]'))return;const d=document.createElement('div');d.className='setting';d.setAttribute('data-style-auto-settings','1');d.innerHTML='<h3>🎭 Style Router AUTO</h3><p class="ok">● Ativo</p><p>O Showrunner escolhe automaticamente o motor narrativo mais adequado a cada pauta e o Writer preserva a escolha em todos os atos.</p><p class="small">O estilo nunca substitui Canon, A/B/C/D, verificação ou Memória Anti-Repetição.</p>';m.appendChild(d)};

  const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.5 · STYLE ROUTER AUTO';
  const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + IA + STYLE AUTO + WRITER + TTS';
  try{render()}catch(e){}
})();