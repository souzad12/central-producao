package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

public class CHSStableActivityV060 extends CHSStableActivityV058 {
  private WebView studioWeb;
  private ProviderKeyStore studioKeys;
  private SecureApiKeyStore legacyDeepSeek;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    studioWeb = findWebView(getWindow().getDecorView());
    studioKeys = new ProviderKeyStore(this);
    legacyDeepSeek = new SecureApiKeyStore(this);
    if (studioWeb != null) studioWeb.addJavascriptInterface(new WritersStudioBridge(), "WritersStudio");
  }

  private class WritersStudioBridge {
    @JavascriptInterface public boolean ready() { return true; }

    @JavascriptInterface public void run(String operation, String payloadJson, String provider, String model, String requestId) {
      final String op = operation == null ? "ROOM" : operation.trim().toUpperCase();
      final String p = MultiAiClient.safeProvider(provider);
      final String m = MultiAiClient.safeModel(p, model);
      new Thread(() -> {
        try {
          String key = readKey(p);
          MultiAiClient.Result r = MultiAiClient.chat(
            p, key, m,
            WritersStudioPrompts.system(op),
            WritersStudioPrompts.user(op, payloadJson),
            "ROOM".equals(op) ? 5500 : 7500,
            true
          );
          String normalized = MultiAiClient.normalizeJsonObject(r.content);
          JSONObject out = new JSONObject(normalized);
          out.put("_provider", p);
          out.put("_model", m);
          out.put("_usage", r.usageJson());
          studioCallback(requestId, true, out.toString());
        } catch (Exception e) {
          studioCallback(requestId, false, safeMessage(e));
        }
      }).start();
    }
  }

  private String readKey(String provider) throws Exception {
    String key = null;
    try { key = studioKeys.read(provider); } catch (Exception ignored) {}
    if ((key == null || key.isEmpty()) && "deepseek".equals(provider)) {
      try { key = legacyDeepSeek.read(); } catch (Exception ignored) {}
    }
    if (key == null || key.isEmpty()) throw new Exception("API Key " + provider + " não configurada.");
    return key;
  }

  private void studioCallback(String requestId, boolean ok, String data) {
    WebView w = studioWeb != null ? studioWeb : findWebView(getWindow().getDecorView());
    if (w == null) return;
    String js = "try{if(window.onWritersStudioResult)window.onWritersStudioResult(" +
      JSONObject.quote(requestId == null ? "" : requestId) + "," +
      (ok ? "true" : "false") + "," + JSONObject.quote(data == null ? "" : data) +
      ")}catch(e){console.error(e)}";
    w.post(() -> w.evaluateJavascript(js, null));
  }

  private String safeMessage(Exception e) {
    String m = e.getMessage();
    return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
  }

  private WebView findWebView(View view) {
    if (view instanceof WebView) return (WebView) view;
    if (!(view instanceof ViewGroup)) return null;
    ViewGroup g = (ViewGroup) view;
    for (int i=0;i<g.getChildCount();i++) {
      WebView w = findWebView(g.getChildAt(i));
      if (w != null) return w;
    }
    return null;
  }
}
