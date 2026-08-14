package com.churchhistorystudio.app;

final class LiteraryEnginePromptsV062 {
  static String system(String operation) {
    String op = operation == null ? "ARCHITECTURE" : operation.trim().toUpperCase();
    String base = LiteraryEnginePrompts.system(op);
    if ("WRITE_ACT".equals(op)) {
      return base +
        "\nCONTRATO DE CONVERGENCIA v0.6.2: targetWords e minimumWords sao requisitos reais de producao. " +
        "Se revisionBrief.currentDraft existir, NAO comece do zero: use o rascunho como materia-prima, preserve o que funciona e reestruture/expanda exatamente os pontos indicados pelo Editor ou Showrunner. " +
        "Distribua a extensao entre cenas e beats; cada cena deve ter funcao propria e cada beat deve alterar acao, informacao, poder, decisao ou consequencia. " +
        "Nunca complete palavras com descricao sensorial decorativa, monologo interior inventado, repeticao de fatos, repeticao de pressagios ou nova parafrase da fonte. " +
        "Para ganhar densidade, use acao dramatica, dialogo reconstruido plausivel, resistencia, estrategia, custo da escolha, consequencia, relacoes entre personagens e contexto historico incorporado a uma necessidade concreta da cena. " +
        "Se sceneWordTargets existir, use como orientacao de equilibrio, sem encher cenas artificialmente. A saida deve chegar ao menos a minimumWords salvo impossibilidade factual explicita.";
    }
    if ("EDIT_ACT".equals(op)) {
      return base +
        "\nCONTRATO DE EDICAO v0.6.2: voce e editor E cirurgiao, nao apenas fiscal. Corrija dentro de revisedAct tudo que puder corrigir antes de reprovar. " +
        "Problema de word_count/length isolado NAO e motivo para bloqueio permanente: expanda o ato no proprio revisedAct usando progressao dramatica, nao enchimento. " +
        "Repeticao, skill pouco distinta, exposicao e progressao fraca devem ser reescritas concretamente quando possivel, nao apenas apontadas. " +
        "Nao recomende genericamente 'mais detalhes sensoriais' nem 'mais conflito interno'; isso pode violar o perfil do CHS. Prefira acao, relacao, subtexto, oposicao, estrategia, decisao e consequencia. " +
        "Use BLOCKER somente para falha que permaneceu apos sua tentativa de correcao e que compromete fonte/parafrase, sermonizacao, rigor historico, continuidade futura, repeticao estrutural grave, vazamento tecnico TTS ou ausencia real de progressao. " +
        "MAJOR e MINOR sao corrigiveis e nao devem, sozinhos, impedir approved=true se o revisedAct ja resolveu o problema. " +
        "Se o texto estiver abaixo de minimumWords, tente corrigir a extensao no revisedAct antes de decidir approved.";
    }
    if ("ROOM".equals(op)) {
      return base +
        "\nRECUPERACAO v0.6.2: se showrunnerFeedback/editorFeedback estiver presente, a Sala esta revisitando um ato que falhou na pratica. Nao repita as mesmas propostas. " +
        "Use os erros observados no rascunho como evidencias para produzir alternativas estruturalmente diferentes. Discordancias devem produzir solucoes, nao comentarios.";
    }
    if ("SHOWRUNNER".equals(op)) {
      return base +
        "\nRECUPERACAO v0.6.2: quando a Sala veio de um fracasso do Editor, escolha um plano que resolva especificamente esses defeitos. " +
        "Nao devolva ao Writer a mesma arquitetura de cena com nomes diferentes. Se o problema foi repeticao funcional, altere funcoes; se foi tensao estacionaria, altere estados; se foi future theft, remova o material reservado.";
    }
    return base;
  }

  static String user(String operation, String payloadJson) {
    String op = operation == null ? "ARCHITECTURE" : operation.trim().toUpperCase();
    String base = LiteraryEnginePrompts.user(op, payloadJson);
    if ("WRITE_ACT".equals(op)) {
      return base +
        "\nIMPORTANTE: o pacote pode conter minimumWords, sceneWordTargets e revisionBrief.currentDraft. Quando houver currentDraft, entregue a VERSAO COMPLETA revisada do ato, nao um complemento separado. " +
        "Nao reduza o numero de palavras ao corrigir repeticao: substitua material redundante por nova progressao causal.";
    }
    if ("EDIT_ACT".equals(op)) {
      return base +
        "\nIMPORTANTE: revisedAct deve ser uma tentativa real de correcao completa. Se approved=false, issues deve explicar apenas o que AINDA permaneceu depois da sua propria revisao. " +
        "Nao marque BLOCKER por mera preferencia estetica. Informe audit.hardBlockers como lista de tipos que realmente impedem uso do texto.";
    }
    return base;
  }
}
