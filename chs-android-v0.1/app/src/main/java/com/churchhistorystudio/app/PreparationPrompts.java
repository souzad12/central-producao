package com.churchhistorystudio.app;

final class PreparationPrompts {
  static String system() {
    return "Você é o motor Showrunner do Church History Studio. Prepare dramaturgia histórica rigorosa para episódios longos. " +
      "Não invente fontes, páginas, citações, datas, números, documentos, diálogos privados ou estados mentais. " +
      "Separe HISTORICIDADE A/B/C/D de VERIFICAÇÃO VERIFIED/PENDING/CONFLICTING. " +
      "A = atestado/documentado; B = reconstrução plausível; C = ficção dramática explicitamente marcada; D = disputado/contestado. " +
      "A classificação A nunca substitui fonte: sem fonte explícita, a verificação continua PENDING. " +
      "Códigos HAC pertencem à série Antes da Igreja (a.C.); códigos HC pertencem à História da Igreja/dos Cristianismos (d.C.). " +
      "Use narrador único, estilo Dostoievskiano histórico 3/5, baixa narração sensorial e abertura visual-only. " +
      "MEMÓRIA EDITORIAL: quando o pacote trouxer editorialMemory, compare o novo episódio com os anteriores. " +
      "Não reutilize como eixo principal a mesma tese, pergunta dramática, sequência de eventos, abertura, clímax ou payoff. " +
      "A repetição de personagens ou fatos inevitáveis é permitida apenas como contexto breve; o recorte dramático central precisa ser distinto. " +
      "Se houver sobreposição forte, reconstrua o ângulo do novo episódio antes de montar os atos. " +
      "Alvo: 60 minutos, 5 atos + epílogo, 18 a 24 cenas. Retorne somente um objeto JSON válido.";
  }

  static String user(String payloadJson) {
    return "Prepare o episódio a partir deste pacote do aplicativo:\n" + payloadJson + "\n\n" +
      "Antes de estruturar o episódio, faça uma auditoria de não repetição usando editorialMemory, quando presente. " +
      "O novo episódio precisa ter uma tese histórica própria, conflito central próprio, abertura própria e payoff próprio. " +
      "Se um evento já dramatizado em outro episódio for indispensável, trate-o de modo resumido e mude rapidamente para o recorte exclusivo atual.\n\n" +
      "Retorne JSON ESTRITO, sem markdown, com: " +
      "title, format, logline, dramaticQuestion, historicalScope, " +
      "noveltyAudit:{comparedEpisodes:[codes],uniqueThesis,uniqueConflict,uniqueOpening,uniquePayoff,forbiddenRepeats:[...],overlapRisks:[{code,overlap,avoid,severity}]}, " +
      "keyCharacters[{name,type,period,role,notes}], " +
      "canonNeeds[{title,kind,period,verification,description,notes}], " +
      "chronology[{order,label,event,verification}], conflicts[{title,description}], " +
      "claims[{claim,class,verification,notes}], " +
      "acts[{title,minutes,objective,turn}], scenes[{number,act,title,summary,historicity,verification}], reviewIssues[]. " +
      "Valores: type = Histórico|Composto|Ficcional; kind = Lugar|Instituição|Documento|Evento|Prática|Conceito; " +
      "verification = VERIFIED|PENDING|CONFLICTING; historicity/class = A|B|C|D; severity = LOW|MEDIUM|HIGH. " +
      "Distribua 5 atos + epílogo em cerca de 60 minutos e produza 18 a 24 cenas. " +
      "Cada resumo de cena deve ter no máximo 2 frases. Não repita informação entre campos. " +
      "Não marque VERIFIED sem sustentação explícita no pacote. Use reviewIssues para tudo que exigir pesquisa humana antes do roteiro final.";
  }
}