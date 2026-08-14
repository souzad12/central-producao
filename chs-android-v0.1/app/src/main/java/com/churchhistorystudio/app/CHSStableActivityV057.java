package com.churchhistorystudio.app;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class CHSStableActivityV057 extends CHSStableActivity {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);

    WebView webView = findStableWebView();
    if (webView == null) return;

    // A base STABLE havia perdido o WebChromeClient que existia na v0.4.8.
    // Sem ele, confirm()/alert() do JavaScript não apareciam no WebView.
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
        runOnUiThread(() -> new AlertDialog.Builder(CHSStableActivityV057.this)
          .setTitle("Church History Studio")
          .setMessage(message == null ? "Confirmar ação?" : message)
          .setPositiveButton("Confirmar", (dialog, which) -> result.confirm())
          .setNegativeButton("Cancelar", (dialog, which) -> result.cancel())
          .setOnCancelListener(dialog -> result.cancel())
          .show());
        return true;
      }

      @Override public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        runOnUiThread(() -> new AlertDialog.Builder(CHSStableActivityV057.this)
          .setTitle("Church History Studio")
          .setMessage(message == null ? "" : message)
          .setPositiveButton("OK", (dialog, which) -> result.confirm())
          .setOnCancelListener(dialog -> result.cancel())
          .show());
        return true;
      }
    });
  }

  private WebView findStableWebView() {
    View root = findViewById(android.R.id.content);
    if (!(root instanceof ViewGroup)) return null;
    ViewGroup group = (ViewGroup) root;
    for (int i = 0; i < group.getChildCount(); i++) {
      View child = group.getChildAt(i);
      WebView found = findWebView(child);
      if (found != null) return found;
    }
    return null;
  }

  private WebView findWebView(View view) {
    if (view instanceof WebView) return (WebView) view;
    if (!(view instanceof ViewGroup)) return null;
    ViewGroup group = (ViewGroup) view;
    for (int i = 0; i < group.getChildCount(); i++) {
      WebView found = findWebView(group.getChildAt(i));
      if (found != null) return found;
    }
    return null;
  }
}
