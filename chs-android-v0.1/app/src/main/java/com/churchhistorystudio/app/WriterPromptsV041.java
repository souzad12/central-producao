package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPromptsV041 {
  static String system() {
    return "Você é o Motor de Dramaturgia Histórica do Church History Studio. Escreva em PT-BR para narrador único. " +
      "Use estilo Dostoievskiano histórico 3/5, sensorialidade baixa e rigor histórico. Não invente diálogos, fontes, citações, fatos ou estados mentais privados. " +
      "Respeite A/B/C/D e VERIFIED/PENDING/CONFLICTING. " +
      "Quando houver editorialMemory ou noveltyAudit, compare o ato com os episódios anteriores. Não reutilize a mesma tese, abertura, sequência dramática, clímax ou payoff. " +
      "Se um fato já usado for indispensável, resuma-o brevemente como contexto e avance para o recorte exclusivo atual. Personagem recorrente não significa arco recorrente. " +
      "Cada cena deve terminar no ponto previsto pelo Scene Stop Engine. Retorne somente JSON válido.";
  }

  static String user(String payloadJson) throws Exception {
    JSONObject p = new JSONObject(payloadJson);
    int target = p.optInt("targetWords", 1300);
    return "Escreva APENAS o ato solicitado. Meta aproximada de " + target + " palavras narradas, tolerância de 15%.\n" +
      "Leia editorialMemory e noveltyAudit antes de escrever. Evite forbiddenRepeats e overlapRisks. Não reconte a espinha narrativa de episódios anteriores.\n" +
      "Use previousEnding só para continuidade, sem repetir o que já foi narrado. Se for o primeiro ato, productionText pode conter [DIREÇÃO VISUAL — NÃO NARRAR], mas ttsText não.\n" +
      "Retorne JSON estrito: {\"actTitle\":\"...\",\"continuityNote\":\"...\",\"repetitionCheck\":\"como o ato preservou o recorte exclusivo\",\"scenes\":[{\"number\":1,\"title\":\"...\",\"productionText\":\"narração completa\",\"ttsText\":\"texto falado\",\"endState\":\"ponto de parada\"}]}.\n" +
      "Não devolva resumo. Escreva a narração completa.\nPACOTE:\n" + payloadJson;
  }
}