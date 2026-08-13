(()=>{
  const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.1 · ANTI-REPETIÇÃO';
  const oldShow=window.showPreparation;
  window.showPreparation=function(code){
    oldShow(code);
    const p=production.find(x=>x.code===code),modal=document.querySelector('#overlay .modal');if(!p||!modal)return;
    const a=p.noveltyAudit,box=document.createElement('div');box.className='setting';
    if(a){
      const risks=Array.isArray(a.overlapRisks)?a.overlapRisks:[],comp=Array.isArray(a.comparedEpisodes)?a.comparedEpisodes:[];
      box.innerHTML=`<h3>🛡 Auditoria Anti-Repetição</h3><p class="ok">● Recorte diferenciado pela memória editorial</p>${comp.length?`<p class="small"><b>Comparados:</b> ${comp.map(esc).join(', ')}</p>`:''}${a.uniqueThesis?`<p><b>Tese exclusiva:</b> ${esc(a.uniqueThesis)}</p>`:''}${a.uniqueConflict?`<p><b>Conflito exclusivo:</b> ${esc(a.uniqueConflict)}</p>`:''}${a.uniqueOpening?`<p><b>Abertura exclusiva:</b> ${esc(a.uniqueOpening)}</p>`:''}${a.uniquePayoff?`<p><b>Payoff exclusivo:</b> ${esc(a.uniquePayoff)}</p>`:''}${risks.length?`<p class="small"><b>Sobreposições controladas:</b><br>${risks.map(r=>`${esc(r.code||'episódio')} · ${esc(r.severity||'')} — evitar: ${esc(r.avoid||r.overlap||'')}`).join('<br>')}</p>`:''}`;
    } else {
      const n=window.buildEditorialMemory?buildEditorialMemory(p).length:0;
      box.innerHTML=`<h3>🛡 Auditoria Anti-Repetição</h3><p class="warning">Preparação antiga, sem noveltyAudit.</p><p class="small">O Writer ainda comparará automaticamente este episódio com ${n} episódios anteriores antes de escrever cada ato. Para máxima proteção, gere novamente a preparação nesta versão.</p>`;
    }
    const actions=modal.querySelector('.actions');actions?modal.insertBefore(box,actions):modal.appendChild(box);
  };
  render();
})();