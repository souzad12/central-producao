package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPromptsV044 {
  static String system() {
    return WriterPromptsV041.system() + " A duração solicitada é obrigatória: não entregue um ato muito abaixo da meta de palavras.";
  }
  static String user(String payloadJson) throws Exception {
    JSONObject p = new JSONObject(payloadJson);
    int target = p.optInt("targetWords", 1300);
    int minimum = p.optInt("minimumWords", Math.round(target * 0.85f));
    boolean expansion = p.optBoolean("expansionMode", false);
    if (!expansion) return WriterPromptsV041.user(payloadJson) + "\nREGRA DE DURAÇÃO: entregue no mínimo " + minimum + " palavras narradas e mire " + target + ".";
    return "CORREÇÃO DE DURAÇÃO. Reescreva o mesmo ato completo usando currentDraft como base. Preserve fatos, recorte, cenas, continuidade, anti-repetição e TTS ultraestrito. Aprofunde contexto, causalidade e consequências sem inventar fatos e sem avançar para o ato seguinte. Entregue entre " + minimum + " e " + target + " palavras narradas. Retorne o mesmo JSON estrito com actTitle, continuityNote, repetitionCheck e scenes com productionText, ttsText e endState. PACOTE:\n" + payloadJson;
  }
}
