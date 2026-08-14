package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONObject;

public class CHSStableActivityV062 extends CHSStableActivityV061 {
  private WebView literaryWeb062;
  private ProviderKeyStore literaryKeys062;
  private SecureApiKeyStore legacyDeepSeek062;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    literaryWeb062 = findWebView062(getWindow().getDecorView());
    literaryKeys062 = new ProviderKeyStore(this);
    legacyDeepSeek062 = new SecureApiKeyStore(this);
    if (literaryWeb062 != null) {
      try { literaryWeb062.removeJavascriptInterface("LiteraryEngine"); } catch (Exception ignored) {}
      literaryWeb062.addJavascriptInterface(new LiteraryEngineBridge062(), "LiteraryEngine");
    }
  }

  private class LiteraryEngineBridge062 {
    @JavascriptInterface public boolean ready() { return true; }

    @JavascriptInterface public void run(String operation, String payloadJson, String provider, String model, String requestId) {
      final String op = operation == null ? "ARCHITECTURE" : operation.trim().toUpperCase();
      final String p = MultiAiClient.safeProvider(provider);
      final String m = MultiAiClient.safeModel(p, model);
      new Thread(() -> {
        try {
          String key = readKey062(p);
          MultiAiClient.Result r = MultiAiClient.chat(
            p, key, m,
            LiteraryEnginePromptsV062.system(op),
            LiteraryEnginePromptsV062.user(op, payloadJson),
            maxTokens062(op),
            true
          );
          String normalized = MultiAiClient.normalizeJsonObject(r.content);
          JSONObject out = new JSONObject(normalized);
          out.put("_provider", p);
          out.put("_model", m);
          out.put("_usage", r.usageJson());
          callback062(requestId, true, out.toString());
        } catch (Exception e) {
          callback062(requestId, false, safeMessage062(e));
        }
      }).start();
    }
  }

  private int maxTokens062(String op) {
    if ("ARCHITECTURE".equals(op) || "ARCHITECTURE_REVISE".equals(op)) return 14000;
    if ("ROOM".equals(op) || "SHOWRUNNER".equals(op)) return 9000;
    if ("WRITE_ACT".equals(op)) return 18000;
    if ("EDIT_ACT".equals(op)) return 18000;
    return 10000;
  }

  private String readKey062(String provider) throws Exception {
    String key = null;
    try { key = literaryKeys062.read(provider); } catch (Exception ignored) {}
    if ((key == null || key.isEmpty()) && "deepseek".equals(provider)) {
      try { key = legacyDeepSeek062.read(); } catch (Exception ignored) {}
    }
    if (key == null || key.isEmpty()) throw new Exception("API Key " + provider + " não configurada.");
    return key;
  }

  private void callback062(String requestId, boolean ok, String data) {
    WebView w = literaryWeb062 != null ? literaryWeb062 : findWebView062(getWindow().getDecorView());
    if (w == null) return;
    String js = "try{if(window.onLiteraryEngineResult)window.onLiteraryEngineResult(" +
      JSONObject.quote(requestId == null ? "" : requestId) + "," +
      (ok ? "true" : "false") + "," + JSONObject.quote(data == null ? "" : data) +
      ")}catch(e){console.error(e)}";
    w.post(() -> w.evaluateJavascript(js, null));
  }

  private String safeMessage062(Exception e) {
    String m = e.getMessage();
    return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
  }

  private WebView findWebView062(View view) {
    if (view instanceof WebView) return (WebView) view;
    if (!(view instanceof ViewGroup)) return null;
    ViewGroup g = (ViewGroup) view;
    for (int i = 0; i < g.getChildCount(); i++) {
      WebView w = findWebView062(g.getChildAt(i));
      if (w != null) return w;
    }
    return null;
  }
}
