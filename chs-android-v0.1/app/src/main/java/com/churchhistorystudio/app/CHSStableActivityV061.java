package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

public class CHSStableActivityV061 extends CHSStableActivityV060 {
  private WebView literaryWeb;
  private ProviderKeyStore literaryKeys;
  private SecureApiKeyStore legacyDeepSeek;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    literaryWeb = findWebView(getWindow().getDecorView());
    literaryKeys = new ProviderKeyStore(this);
    legacyDeepSeek = new SecureApiKeyStore(this);
    if (literaryWeb != null) literaryWeb.addJavascriptInterface(new LiteraryEngineBridge(), "LiteraryEngine");
  }

  private class LiteraryEngineBridge {
    @JavascriptInterface public boolean ready() { return true; }

    @JavascriptInterface public void run(String operation, String payloadJson, String provider, String model, String requestId) {
      final String op = operation == null ? "ARCHITECTURE" : operation.trim().toUpperCase();
      final String p = MultiAiClient.safeProvider(provider);
      final String m = MultiAiClient.safeModel(p, model);
      new Thread(() -> {
        try {
          String key = readKey(p);
          int maxTokens = maxTokens(op);
          MultiAiClient.Result r = MultiAiClient.chat(
            p, key, m,
            LiteraryEnginePrompts.system(op),
            LiteraryEnginePrompts.user(op, payloadJson),
            maxTokens,
            true
          );
          String normalized = MultiAiClient.normalizeJsonObject(r.content);
          JSONObject out = new JSONObject(normalized);
          out.put("_provider", p);
          out.put("_model", m);
          out.put("_usage", r.usageJson());
          callback(requestId, true, out.toString());
        } catch (Exception e) {
          callback(requestId, false, safeMessage(e));
        }
      }).start();
    }
  }

  private int maxTokens(String op) {
    if ("ARCHITECTURE".equals(op) || "ARCHITECTURE_REVISE".equals(op)) return 12000;
    if ("ROOM".equals(op) || "SHOWRUNNER".equals(op)) return 7500;
    if ("WRITE_ACT".equals(op)) return 12000;
    if ("EDIT_ACT".equals(op)) return 14000;
    return 8000;
  }

  private String readKey(String provider) throws Exception {
    String key = null;
    try { key = literaryKeys.read(provider); } catch (Exception ignored) {}
    if ((key == null || key.isEmpty()) && "deepseek".equals(provider)) {
      try { key = legacyDeepSeek.read(); } catch (Exception ignored) {}
    }
    if (key == null || key.isEmpty()) throw new Exception("API Key " + provider + " não configurada.");
    return key;
  }

  private void callback(String requestId, boolean ok, String data) {
    WebView w = literaryWeb != null ? literaryWeb : findWebView(getWindow().getDecorView());
    if (w == null) return;
    String js = "try{if(window.onLiteraryEngineResult)window.onLiteraryEngineResult(" +
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
    for (int i = 0; i < g.getChildCount(); i++) {
      WebView w = findWebView(g.getChildAt(i));
      if (w != null) return w;
    }
    return null;
  }
}
