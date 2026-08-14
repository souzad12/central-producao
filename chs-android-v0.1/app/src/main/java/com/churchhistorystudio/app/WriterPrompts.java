package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPrompts {
  static String system() {
    return WriterPromptsV045.system() +
      " CONTRATO v0.6.0: escreva LITERATURA HISTÓRICA DRAMATIZADA baseada em fatos reais. É PROIBIDO transformar o episódio em sermão, estudo bíblico, aula teológica, comentário exegético, verbete ou documentário expositivo, salvo ordem explícita. " +
      "A fonte não é modelo de prosa: não copie nem parafraseie longamente Bíblia, crônica ou documento. Use fontes como fatos, limites, falas atestadas e contexto. " +
      "A superfície falada deve ser majoritariamente CENA: personagens em situação, ação, subtexto, diálogo, obstáculo, pressão, decisão, virada e consequência. Contexto só entra quando aumenta a força da cena seguinte. " +
      "Meta de proporção: aproximadamente 60-75% cenas dramatizadas, 20-35% narração de ligação/contexto e no máximo 10% citação/paráfrase direta de fonte. Não force números artificialmente; use-os como disciplina editorial. " +
      "RECONSTRUÇÃO B É PERMITIDA: diálogo plausível, silêncios, gestos, pequenas ações, encontros e circunstâncias compatíveis com fatos, época, posição e relações. Nunca apresente fala reconstruída como citação autêntica. " +
      "LICENÇA C É PERMITIDA de modo controlado para literatura, inclusive personagens menores/compostos, desde que não altere fatos centrais, resultados históricos, cronologia, documentos ou doutrina e permaneça indicada nas notas técnicas. " +
      "Não declare pensamento privado como fato; manifeste psicologia por comportamento, contradição, subtexto, escolhas e consequências. " +
      "SURPRESA: busque reversões verdadeiras, revelação retardada, falsa resolução, ironia histórica, retorno de elemento e consequências inesperadas. Não invente twist factual. Cada ato deve terminar em estado diferente daquele em que começou. " +
      "ANTI-REDUNDÂNCIA OBRIGATÓRIA: não repetir informação, conflito, metáfora, explicação, pergunta ou revelação já utilizada no episódio. Não recontar episódios anteriores e não roubar episódios futuros. " +
      "Se writersStudio.roomDecision existir, trate writerInstruction como DIREÇÃO DE SHOWRUNNER obrigatória, subordinada apenas ao Historical Guard. A skill AUTO pode mudar por ato/cena. " +
      "HANDOFF FINAL: no último ato/epílogo, feche o conflito atual e termine com uma consequência, imagem ou pergunta histórica real que naturalmente abre o próximo episódio. Nunca diga 'no próximo episódio veremos' e não use CTA publicitário. " +
      "Notas de rigor ficam em {NOTA HISTÓRICA: ...} ou {RIGOR: ...} e são removidas do TTS. Evite na fala fórmulas como 'o texto não diz', 'não sabemos', 'a arqueologia sugere' salvo quando a incerteza for o próprio drama.";
  }

  static String user(String payloadJson) throws Exception {
    String base = WriterPromptsV045.user(payloadJson);
    JSONObject root = new JSONObject(payloadJson);
    JSONObject prep = root.optJSONObject("preparation");
    JSONObject studio = root.optJSONObject("writersStudio");
    JSONObject room = studio == null ? null : studio.optJSONObject("roomDecision");
    StringBuilder out = new StringBuilder(base);

    out.append("\nMODO EDITORIAL: LITERATURA HISTÓRICA DRAMATIZADA. NÃO escreva um resumo do material-fonte. Faça o leitor viver uma progressão de cenas.");
    out.append("\nREGRA DE CENA: toda cena precisa de agente, desejo/objetivo, obstáculo/pressão, virada e consequência. Se uma cena apenas explica, reestruture-a para que a informação apareça porque alguém precisa agir, escolher, confrontar, esconder, perder ou descobrir algo.");
    out.append("\nREGRA DE FONTE: não reproduza em sequência longos blocos bíblicos ou documentais. Falas atestadas podem ser citadas brevemente. Entre os marcos documentados, use reconstrução B controlada e licença C controlada quando dramaturgicamente necessária.");
    out.append("\nREGRA DE SURPRESA: preserve fairness. O twist deve nascer de informação verdadeira guardada, consequência histórica, mudança de perspectiva ou reversão causal; nunca de fato inventado.");
    out.append("\nREGRA DE REDUNDÂNCIA: antes de cada parágrafo, pergunte funcionalmente se ele acrescenta ação, nova informação, nova pressão, nova imagem ou nova consequência. Se apenas repete o que já foi dito, corte.");

    if (studio != null) {
      out.append("\nWRITERS' STUDIO: modo=").append(studio.optString("mode","AUTO_SURPRISE"))
        .append("; preferência=").append(studio.optString("preferredSkill","AUTO"))
        .append("; direção manual=").append(studio.optString("manualDirection",""))
        .append("; POV focal=").append(studio.optString("povFocus",""));
      if (room != null) {
        out.append("\nDECISÃO DA SALA (OBRIGATÓRIA): ").append(room.toString());
        String instruction = room.optString("writerInstruction", "");
        if (!instruction.isEmpty()) out.append("\nDIREÇÃO DO SHOWRUNNER: ").append(instruction);
      }
    }

    JSONObject handoff = prep == null ? null : prep.optJSONObject("seriesHandoff");
    int actIndex = root.optInt("actIndex", 0);
    int actCount = prep != null && prep.optJSONArray("acts") != null ? prep.optJSONArray("acts").length() : 6;
    if (actIndex >= Math.max(0, actCount - 1)) {
      out.append("\nEPÍLOGO/HANDOFF: feche o conflito principal deste episódio. Depois abra uma consequência real para a continuidade serial sem entregar o clímax do próximo.");
      if (handoff != null) out.append(" Diretriz de continuidade: ").append(handoff.toString());
    }

    out.append("\nSAÍDA: preserve o JSON de cenas exigido pelo Writer legado, mas productionText deve conter literatura + blocos técnicos entre chaves; ttsText deve conter somente a narração/dialogação falável. Não use markdown no texto narrável.");
    return out.toString();
  }
}
