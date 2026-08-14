package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPrompts {
  static String system() {
    return WriterPromptsV045.system() +
      " Preserve a voz narrativa escolhida pelo Showrunner quando narrativeVoice existir. " +
      "Nunca transforme personagem histórico em narrador fictício de primeira pessoa.";
  }

  static String user(String payloadJson) throws Exception {
    String base = WriterPromptsV045.user(payloadJson);
    JSONObject root = new JSONObject(payloadJson);
    JSONObject prep = root.optJSONObject("preparation");
    JSONObject voice = prep == null ? null : prep.optJSONObject("narrativeVoice");
    if (voice == null) {
      return base + "\nVOZ NARRATIVA: terceira pessoa histórica como base; primeira pessoa plural investigativa pode aparecer com parcimônia quando natural.";
    }
    String perspective = voice.optString("perspective", "TERCEIRA_PESSOA_HISTORICA");
    String label = voice.optString("label", perspective);
    String rules = voice.optJSONArray("rules") == null ? "[]" : voice.optJSONArray("rules").toString();
    return base + "\nVOZ NARRATIVA ESCOLHIDA PELO SHOWRUNNER: " + label + " (" + perspective + "). Regras: " + rules + ". Preserve essa perspectiva no ato inteiro e nos atos seguintes. Primeira pessoa singular de personagem somente quando for citação ou documento sustentado pelo pacote.";
  }
}
