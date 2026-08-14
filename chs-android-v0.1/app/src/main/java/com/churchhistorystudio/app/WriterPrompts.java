package com.churchhistorystudio.app;

import org.json.JSONObject;

final class WriterPrompts {
  static String system() {
    return WriterPromptsV045.system() +
      " Preserve a voz narrativa escolhida pelo Showrunner quando narrativeVoice existir. " +
      "O CHS deve soar como DRAMA HISTÓRICO RECONSTRUÍDO, não como aula acadêmica. Evidência e debate historiográfico funcionam como guardrails, salvo quando a pauta for explicitamente historiográfica. " +
      "Priorize cenas, decisões públicas, conflitos, consequências, causalidade e tensão histórica sustentável. Não transforme lacunas documentais em longas enumerações de hipóteses. " +
      "Nunca transforme personagem histórico em narrador fictício de primeira pessoa.";
  }

  static String user(String payloadJson) throws Exception {
    String base = WriterPromptsV045.user(payloadJson);
    JSONObject root = new JSONObject(payloadJson);
    JSONObject prep = root.optJSONObject("preparation");
    JSONObject voice = prep == null ? null : prep.optJSONObject("narrativeVoice");
    JSONObject posture = prep == null ? null : prep.optJSONObject("researchPosture");
    StringBuilder out = new StringBuilder(base);
    if (voice == null) {
      out.append("\nVOZ NARRATIVA: terceira pessoa histórica como base; outras pessoas gramaticais podem aparecer com parcimônia quando natural.");
    } else {
      String perspective = voice.optString("perspective", "TERCEIRA_PESSOA_HISTORICA");
      String label = voice.optString("label", perspective);
      String rules = voice.optJSONArray("rules") == null ? "[]" : voice.optJSONArray("rules").toString();
      out.append("\nVOZ NARRATIVA ESCOLHIDA PELO SHOWRUNNER: ").append(label).append(" (").append(perspective).append("). Regras: ").append(rules)
        .append(". Preserve essa perspectiva no ato inteiro e nos atos seguintes. Primeira pessoa singular de personagem somente quando for citação ou documento sustentado pelo pacote.");
    }
    String mode = posture == null ? "BACKGROUND_GUARD" : posture.optString("mode", "BACKGROUND_GUARD");
    if ("EXPLICIT_INVESTIGATION".equals(mode)) {
      out.append("\nPOSTURA DE PESQUISA: investigação explícita é necessária nesta pauta, mas não substitui o drama. Integre evidências e hipóteses apenas nos pontos em que alteram a compreensão da cena, do conflito ou do desfecho.");
    } else {
      out.append("\nPOSTURA DE PESQUISA: BACKGROUND_GUARD. Não narre o processo historiográfico como eixo do ato. Use incertezas apenas para limitar afirmações e reconstruções. A superfície narrativa deve ser drama histórico reconstruído.");
    }
    out.append("\nREGRA DE RECONSTRUÇÃO: transforme fatos A e reconstruções B sustentáveis em cenas narradas com conflito e progressão. Conteúdo C só pode existir explicitamente marcado como ficção dramática e nunca como prova. Evite diálogos privados inventados e estados mentais não documentados.");
    return out.toString();
  }
}
