(()=>{
  window.CHS_VERSION='0.5.0';
  window.CHS_CHANNEL='STABLE';
  const setIdentity=()=>{
    const sub=document.querySelector('.sub');
    if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.5.0 · STABLE';
    const top=document.getElementById('topStatus');
    if(top)top.textContent='SQLITE + MULTI-IA + VAULT + GUARDS';
  };
  const ensureNativeCsvButton=()=>{
    const candidates=[...document.querySelectorAll('button,label')].filter(x=>/Importar CSV/i.test(x.textContent||''));
    const old=candidates[0];
    if(!old)return;
    let b=old;
    if(old.tagName!=='BUTTON'){
      b=document.createElement('button');
      b.className='btn primary';
      old.replaceWith(b);
    }
    b.type='button';
    b.textContent='Importar CSV';
    b.dataset.nativeCsvStable='1';
    b.onclick=()=>{
      try{
        if(typeof CSVBridge==='undefined'||!CSVBridge.ready())throw new Error('ponte nativa indisponível');
        CSVBridge.pickCsv();
      }catch(e){flash('Importador CSV indisponível: '+(e?.message||e));}
    };
  };
  const oldRender=window.render;
  if(typeof oldRender==='function'){
    window.render=function(){
      const r=oldRender();
      setIdentity();
      ensureNativeCsvButton();
      return r;
    };
  }
  window.addEventListener('error',e=>{try{flash('Erro interno: '+(e.message||'JavaScript'));}catch(x){}});
  window.addEventListener('unhandledrejection',e=>{try{flash('Erro interno: '+((e.reason&&e.reason.message)||e.reason||'Promise'));}catch(x){}});
  setIdentity();
  ensureNativeCsvButton();
  try{if(typeof render==='function')render();}catch(e){console.error('CHS STABLE',e)}
})();
