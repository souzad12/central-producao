(()=>{
  window.CHS_TTS_ULTRASTRICT_VERSION='0.4.2';
  const TECH_WORDS='ATO|CENA|SEQU[EÊ]NCIA|BEAT|POV|TEMPO|TIMECODE|VISUAL|DIRE[CÇ][AÃ]O VISUAL|PLANO|C[AÂ]MERA|ENQUADRAMENTO|TRANSI[CÇ][AÃ]O|SFX|FX|SOM|SOM DIEG[EÉ]TICO|TRILHA|M[UÚ]SICA|SUBT[IÍ]TULO|CARTELA|TEXTO NA TELA|NOTA|NOTA HIST[OÓ]RICA|NOTA EDITORIAL|COMENT[AÁ]RIO|CLAIM|HISTORICIDADE|VERIFICA[CÇ][AÃ]O|STATUS|DURA[CÇ][AÃ]O';
  const technicalLine=new RegExp('^\\s*(?:'+TECH_WORDS+')\\s*(?:[:#\\-–—]|\\d|$)','i');
  const technicalBracket=new RegExp('\\[[^\\]]*(?:'+TECH_WORDS+'|N[AÃ]O NARRAR)[^\\]]*\\]','gi');

  function sanitizeTts(input){
    let text=String(input||'').replace(/\r\n/g,'\n');
    const audit={braceBlocks:0,technicalBrackets:0,technicalLines:0,headings:0,totalRemoved:0};
    text=text.replace(/\{[\s\S]*?\}/g,m=>{audit.braceBlocks++;audit.totalRemoved++;return ' ';});
    text=text.replace(technicalBracket,m=>{audit.technicalBrackets++;audit.totalRemoved++;return ' ';});
    const kept=[];
    for(const raw of text.split('\n')){
      const line=raw.trim();
      if(!line){kept.push('');continue;}
      if(technicalLine.test(line)){audit.technicalLines++;audit.totalRemoved++;continue;}
      if(/^\s*(?:ATO|CENA|SEQU[EÊ]NCIA|EP[IÍ]LOGO)\s+(?:[IVXLCDM]+|\d+)(?:\s*[-–—:].*)?$/i.test(line)){
        audit.headings++;audit.totalRemoved++;continue;
      }
      if(/^\s*(?:\[?\s*)?(?:FADE\s+(?:IN|OUT)|CUT\s+TO|CORTE\s+PARA|DISSOLVE|FIM\s+DA\s+CENA)(?:\s*\]?)\s*$/i.test(line)){
        audit.technicalLines++;audit.totalRemoved++;continue;
      }
      kept.push(raw);
    }
    let clean=kept.join('\n')
      .replace(/[ \t]+\n/g,'\n')
      .replace(/\n[ \t]+/g,'\n')
      .replace(/\n{3,}/g,'\n\n')
      .replace(/[ \t]{2,}/g,' ')
      .trim();
    return {text:clean,audit};
  }

  function mergeAudit(a,b){
    const r={braceBlocks:0,technicalBrackets:0,technicalLines:0,headings:0,totalRemoved:0};
    for(const k of Object.keys(r))r[k]=(a?.[k]||0)+(b?.[k]||0);
    return r;
  }

  function sanitizeOutputObject(out){
    let total={braceBlocks:0,technicalBrackets:0,technicalLines:0,headings:0,totalRemoved:0};
    if(Array.isArray(out.scenes)){
      for(const sc of out.scenes){
        const source=(sc.ttsText&&String(sc.ttsText).trim())?sc.ttsText:(sc.productionText||'');
        const result=sanitizeTts(source);
        sc.ttsText=result.text;
        sc.ttsAudit=result.audit;
        total=mergeAudit(total,result.audit);
      }
    }
    out.ttsAudit=total;
    out.ttsSanitizer='ULTRASTRICT_v0.4.2';
    return out;
  }

  function scriptTts(p){
    let audit={braceBlocks:0,technicalBrackets:0,technicalLines:0,headings:0,totalRemoved:0};
    const parts=[];
    const acts=(p?.scriptActs||[]).filter(Boolean).sort((a,b)=>(a.actIndex??0)-(b.actIndex??0));
    for(const act of acts){
      for(const sc of (act.scenes||[])){
        const r=sanitizeTts(sc.ttsText||sc.productionText||'');
        if(r.text)parts.push(r.text);
        audit=mergeAudit(audit,r.audit);
      }
    }
    return {text:parts.join('\n\n').replace(/\n{3,}/g,'\n\n').trim(),audit};
  }

  function wordCount(t){return String(t||'').trim()?String(t).trim().split(/\s+/).length:0;}
  function auditHtml(a){
    const x=a||{};
    return `<div class="setting"><b>🧹 Auditoria TTS Ultraestrito</b><p class="small">Removidos nesta saída: <b>${x.totalRemoved||0}</b> elementos não faláveis · ${x.braceBlocks||0} blocos {…} · ${x.technicalBrackets||0} marcações técnicas entre colchetes · ${x.technicalLines||0} linhas técnicas · ${x.headings||0} cabeçalhos.</p><p class="ok">● Saída TTS gerada somente com conteúdo falável.</p></div>`;
  }

  const previousWriting=window.onAiWritingResult;
  window.onAiWritingResult=function(rid,ok,data){
    if(ok){
      try{data=JSON.stringify(sanitizeOutputObject(JSON.parse(data)));}
      catch(e){ok=false;data='Falha na sanitização TTS: '+e.message;}
    }
    return previousWriting(rid,ok,data);
  };

  const previousViewFull=window.viewFullScript;
  window.viewFullScript=function(code,mode){
    if(mode!=='tts')return previousViewFull(code,mode);
    const p=production.find(x=>x.code===code);if(!p)return;
    const result=scriptTts(p),txt=result.text,wc=wordCount(txt),mins=wc?Math.round((wc/130)*10)/10:0;
    overlay.innerHTML=`<div class="overlay"><div class="modal"><button class="close" onclick="openWriter('${code}')">×</button><span class="code">${esc(p.code)}</span><h2>🧹 TTS Limpo · Ultraestrito</h2><p class="small">${wc.toLocaleString('pt-BR')} palavras · estimativa ~${mins} min</p>${auditHtml(result.audit)}<div class="script-view">${esc(txt)}</div><div class="actions"><button class="btn" onclick="openWriter('${code}')">Voltar</button><button class="btn primary" onclick="exportScript('${code}','tts')">Salvar TTS .txt</button></div></div></div>`;
  };

  const previousExport=window.exportScript;
  window.exportScript=function(code,mode){
    if(mode!=='tts')return previousExport(code,mode);
    const p=production.find(x=>x.code===code);if(!p)return;
    const result=scriptTts(p);
    if(!result.text.trim())return flash('Não há texto falável para exportar.');
    try{NativeStore.exportText(`${p.code}_TTS_LIMPO.txt`,result.text);flash(`TTS limpo: ${result.audit.totalRemoved||0} elementos técnicos removidos.`)}catch(e){flash('Falha ao exportar TTS: '+e.message)}
  };

  const previousOpenWriter=window.openWriter;
  window.openWriter=function(code){
    previousOpenWriter(code);
    setTimeout(()=>{
      document.querySelectorAll('#overlay button').forEach(b=>{if(/^Ver TTS$|^TTS limpo$/i.test(b.textContent.trim()))b.textContent='🧹 TTS Limpo';});
    },0);
  };

  const sub=document.querySelector('.sub');if(sub)sub.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.2 · TTS ULTRAESTRITO';
  const top=document.getElementById('topStatus');if(top)top.textContent='SQLITE + IA + WRITER + TTS';
  window.sanitizeChsTts=sanitizeTts;
})();