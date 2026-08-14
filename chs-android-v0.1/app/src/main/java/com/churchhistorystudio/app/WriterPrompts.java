package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPrompts {
  static String system() {
    return WriterPromptsV045.system() +
      " Preserve a voz narrativa escolhida pelo Showrunner quando narrativeVoice existir. " +
      "O produto falado do CHS deve soar como NOVELA HISTÓRICA RECONSTRUÍDA: cena, conflito, ação, decisão, pressão, virada e consequência. Não deve soar como aula, verbete, documentário arqueológico ou relatório de hipóteses, salvo quando a própria pauta for explicitamente historiográfica. " +
      "Para pautas históricas comuns, abra dentro do momento histórico; não abra em escavações modernas, museus ou com o narrador explicando sua investigação. " +
      "Priorize agentes históricos em situação, decisões públicas, confrontos, alianças, rupturas, deslocamentos e efeitos humanos. Evite longos parágrafos de contexto antes que algo aconteça. " +
      "Evidência, verificação e debate historiográfico são guardrails. Quando uma ressalva de rigor precisar ser preservada mas não for parte da experiência dramática, coloque-a em {NOTA HISTÓRICA: ...}; essa nota é técnica e não deve integrar a narração falada. " +
      "Não repita continuamente fórmulas como 'não sabemos', 'a arqueologia sugere', 'o historiador pode inferir', 'há várias hipóteses'. Use-as na fala apenas quando a incerteza for o próprio conflito do episódio. " +
      "Nunca invente diálogo privado, pensamento, memória, confissão ou estado mental. Reconstrução B permite encadear ações e circunstâncias plausíveis sustentadas pelo pacote, não fabricar consciência privada. " +
      "Nunca transforme personagem histórico em narrador fictício de primeira pessoa.";
  }

  static String user(String payloadJson) throws Exception {
    String base = WriterPromptsV045.user(payloadJson);
    JSONObject root = new JSONObject(payloadJson);
    JSONObject prep = root.optJSONObject("preparation");
    JSONObject voice = prep == null ? null : prep.optJSONObject("narrativeVoice");
    JSONObject posture = prep == null ? null : prep.optJSONObject("researchPosture");
    JSONObject style = prep == null ? null : prep.optJSONObject("narrativeStyle");
    StringBuilder out = new StringBuilder(base);

    out.append("\nMODO EDITORIAL OBRIGATÓRIO: NOVELA HISTÓRICA RECONSTRUÍDA. Cada ato deve avançar por cenas. Em cada cena identifique na própria prosa um agente, algo que está em jogo, uma pressão/obstáculo e uma mudança ao final. Não escreva uma sucessão de parágrafos enciclopédicos sobre o tema.");
    out.append("\nSUPERFÍCIE TTS: mantenha a experiência falada dramática. Informações de bastidor, classificação, incerteza metodológica, cautela arqueológica ou observação de fonte que não precisem ser ouvidas devem ir em {NOTA HISTÓRICA: ...}, {RIGOR: ...} ou outra chave técnica, para serem removidas pelo TTS Ultraestrito.");
    out.append("\nABERTURA: salvo quando a pauta for explicitamente arqueológica/historiográfica, comece dentro do passado em movimento, não no presente olhando ruínas e não com uma pergunta de pesquisador.");
    out.append("\nRITMO: contexto só entra quando modifica a cena. Evite explicar primeiro tudo sobre cidade, povo, geografia, arqueologia e depois começar o drama. Distribua contexto dentro da ação.");

    if (voice == null) {
      out.append("\nVOZ NARRATIVA: terceira pessoa histórica como base. O narrador acompanha o drama sem se transformar em professor ou personagem fictício.");
    } else {
      String perspective = voice.optString("perspective", "TERCEIRA_PESSOA_HISTORICA");
      String label = voice.optString("label", perspective);
      String rules = voice.optJSONArray("rules") == null ? "[]" : voice.optJSONArray("rules").toString();
      out.append("\nVOZ NARRATIVA ESCOLHIDA PELO SHOWRUNNER: ").append(label).append(" (").append(perspective).append("). Regras: ").append(rules)
        .append(". Preserve-a sem deixar a pessoa gramatical dominar o gênero. O gênero continua sendo novela histórica. Primeira pessoa singular de personagem somente quando for citação ou documento sustentado pelo pacote.");
    }

    String primary = style == null ? "DRAMA_HISTORICO_RECONSTRUIDO" : style.optString("primary", "DRAMA_HISTORICO_RECONSTRUIDO");
    String secondary = style == null ? "" : style.optString("secondary", "");
    out.append("\nMOTOR DRAMÁTICO: ").append(primary).append(".");
    if (!secondary.isEmpty()) {
      out.append(" LENTE LITERÁRIA: ").append(secondary).append(". A lente só modula ritmo, foco psicológico, escala e tensão; ela não muda o episódio para ensaio ou documentário.");
      if ("DOSTOIEVSKIANA_HISTORICA".equals(secondary)) {
        out.append(" Na lente dostoievskiana, concentre-se em pressão moral, lealdade, poder, culpa, ambição, medo, contradições públicas e consequências entre pessoas, sempre sem inventar interioridade privada.");
      }
    }

    String mode = posture == null ? "BACKGROUND_GUARD" : posture.optString("mode", "BACKGROUND_GUARD");
    if ("EXPLICIT_INVESTIGATION".equals(mode)) {
      out.append("\nPOSTURA DE PESQUISA: EXPLICIT_INVESTIGATION. A investigação pode entrar na fala apenas quando muda o sentido de uma cena ou é a própria pauta. Não faça inventário de hipóteses; dramatize o problema e concentre a discussão de evidências em passagens curtas.");
    } else {
      out.append("\nPOSTURA DE PESQUISA: BACKGROUND_GUARD. Não narre o processo historiográfico. Use as incertezas para limitar o que você afirma e para gerar notas técnicas, não como eixo falado.");
    }

    out.append("\nREGRA DE RECONSTRUÇÃO: transforme fatos A e reconstruções B sustentáveis em cenas narradas com progressão. Conteúdo C só pode existir explicitamente marcado como ficção dramática e nunca como prova. D deve ser tratado como disputado. Não invente diálogo privado nem estado mental.");
    out.append("\nCONTINUIDADE: não consuma material reservado aos próximos episódios. Se um tema futuro for necessário, faça apenas menção mínima ou prenúncio, sem dramatizar sua sequência central, clímax ou payoff.");
    return out.toString();
  }
}
