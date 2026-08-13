package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPromptsV045 {
  static String system() {
    return WriterPromptsV044.system() +
      " A preparação pode conter narrativeStyle escolhido automaticamente pelo Showrunner. " +
      "Aplique esse motor como gramática narrativa do ato, não como imitação textual literal de um autor ou cineasta. " +
      "O estilo é subordinado ao rigor histórico, à memória anti-repetição e ao TTS ultraestrito.";
  }

  static String user(String payloadJson) throws Exception {
    JSONObject p = new JSONObject(payloadJson);
    String base = WriterPromptsV044.user(payloadJson);
    JSONObject prep = p.optJSONObject("preparation");
    JSONObject style = prep != null ? prep.optJSONObject("narrativeStyle") : null;
    if (style == null) {
      return base + "\nMOTOR DE ESTILO: se a preparação antiga não tiver narrativeStyle, use dramaturgia histórica sóbria e não invente uma estilização forte.";
    }
    String primary = style.optString("primaryLabel", style.optString("primary", "Dramaturgia histórica"));
    String secondary = style.optString("secondaryLabel", style.optString("secondary", ""));
    int intensity = style.optInt("intensity", 3);
    String traits = style.optJSONArray("traits") != null ? style.optJSONArray("traits").toString() : "[]";
    String guardrails = style.optJSONArray("guardrails") != null ? style.optJSONArray("guardrails").toString() : "[]";
    String blend = secondary == null || secondary.trim().isEmpty() ? primary : primary + " + " + secondary;
    return base +
      "\nMOTOR DE ESTILO ESCOLHIDO PELO SHOWRUNNER: " + blend + ". Intensidade " + intensity + "/5. " +
      "Características a aplicar: " + traits + ". Limites: " + guardrails + ". " +
      "Mantenha o mesmo motor em todas as cenas deste ato e preserve-o nos atos seguintes. " +
      "Não copie frases, sintaxe identificável, maneirismos ou passagens de obras; converta o estilo em ritmo, foco dramático, arquitetura de conflito e perspectiva narrativa abstrata.";
  }
}
