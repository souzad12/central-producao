package com.churchhistorystudio.app;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class DeepSeekClient {
  private static final String ENDPOINT = "https://api.deepseek.com/chat/completions";

  static String safeModel(String model) {
    return "deepseek-v4-pro".equals(model) ? "deepseek-v4-pro" : "deepseek-v4-flash";
  }

  static String chat(String apiKey, String model, String system, String user, int maxTokens, boolean jsonMode) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
    conn.setRequestMethod("POST");
    conn.setConnectTimeout(30000);
    conn.setReadTimeout(240000);
    conn.setDoOutput(true);
    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("User-Agent", "ChurchHistoryStudio-Android/0.3");

    JSONObject body = new JSONObject();
    body.put("model", safeModel(model));
    JSONArray messages = new JSONArray();
    messages.put(new JSONObject().put("role", "system").put("content", system));
    messages.put(new JSONObject().put("role", "user").put("content", user));
    body.put("messages", messages);
    body.put("temperature", jsonMode ? 0.25 : 0.0);
    body.put("max_tokens", maxTokens);
    body.put("thinking", new JSONObject().put("type", "disabled"));
    if (jsonMode) body.put("response_format", new JSONObject().put("type", "json_object"));

    try (OutputStream out = conn.getOutputStream()) {
      out.write(body.toString().getBytes(StandardCharsets.UTF_8));
    }

    int status = conn.getResponseCode();
    InputStream stream = status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream();
    String response = readStream(stream, 8 * 1024 * 1024);
    if (status < 200 || status >= 300) throw new Exception("DeepSeek HTTP " + status + ": " + extractError(response));

    JSONObject root = new JSONObject(response);
    JSONArray choices = root.optJSONArray("choices");
    if (choices == null || choices.length() == 0) throw new Exception("DeepSeek não retornou choices.");
    JSONObject choice = choices.getJSONObject(0);
    if ("length".equals(choice.optString("finish_reason", ""))) {
      throw new Exception("Resposta da IA foi truncada pelo limite de saída. Tente novamente.");
    }
    JSONObject message = choice.optJSONObject("message");
    String content = message == null ? null : message.optString("content", null);
    if (content == null || content.trim().isEmpty()) throw new Exception("DeepSeek retornou conteúdo vazio.");
    return content;
  }

  static String normalizeJsonObject(String content) {
    String t = content == null ? "" : content.trim();
    if (t.startsWith("```")) {
      int firstNl = t.indexOf('\n');
      int lastFence = t.lastIndexOf("```");
      if (firstNl >= 0 && lastFence > firstNl) t = t.substring(firstNl + 1, lastFence).trim();
    }
    int a = t.indexOf('{'), b = t.lastIndexOf('}');
    return a >= 0 && b > a ? t.substring(a, b + 1) : t;
  }

  private static String readStream(InputStream in, int maxBytes) throws Exception {
    if (in == null) return "";
    try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192]; int read, total = 0;
      while ((read = input.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) throw new Exception("Resposta maior que o limite de segurança.");
        out.write(buffer, 0, read);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private static String extractError(String raw) {
    try {
      JSONObject r = new JSONObject(raw);
      JSONObject e = r.optJSONObject("error");
      if (e != null) return e.optString("message", raw);
    } catch (Exception ignored) {}
    return raw == null || raw.isEmpty() ? "erro sem mensagem" : raw.substring(0, Math.min(raw.length(), 500));
  }
}
