package com.churchhistorystudio.app;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV047 extends WriterActivityV046Flow {
  private WebView safeWeb;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(17,17,17));
    getWindow().setNavigationBarColor(Color.rgb(17,17,17));
    getWindow().getDecorView().post(this::installHardSafeArea);
    getWindow().getDecorView().postDelayed(this::loadV047Patch, 8600);
  }

  @Override protected void onResume() {
    super.onResume();
    if (safeWeb != null) safeWeb.requestApplyInsets();
  }

  private void installHardSafeArea() {
    safeWeb = findWebView(getWindow().getDecorView());
    if (safeWeb == null) return;
    safeWeb.setPadding(0, 0, 0, 0);
    safeWeb.setOnApplyWindowInsetsListener((v, insets) -> {
      int systemTop = Math.max(0, insets.getSystemWindowInsetTop());
      int systemBottom = Math.max(0, insets.getSystemWindowInsetBottom());
      int top = systemTop + dp(18);
      int bottom = systemBottom + dp(26);
      ViewGroup.LayoutParams raw = v.getLayoutParams();
      if (raw instanceof ViewGroup.MarginLayoutParams) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
        if (lp.topMargin != top || lp.bottomMargin != bottom) {
          lp.topMargin = top;
          lp.bottomMargin = bottom;
          v.setLayoutParams(lp);
        }
      } else {
        v.setPadding(0, top, 0, bottom);
      }
      return insets;
    });
    safeWeb.requestApplyInsets();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void loadV047Patch() {
    try {
      WebView web = safeWeb != null ? safeWeb : findWebView(getWindow().getDecorView());
      if (web == null) return;
      try (InputStream in = getAssets().open("ui_delete_v047.js"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        byte[] buf = new byte[4096]; int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        web.evaluateJavascript(new String(out.toByteArray(), StandardCharsets.UTF_8), null);
      }
    } catch (Exception ignored) {}
  }

  private WebView findWebView(View v) {
    if (v instanceof WebView) return (WebView) v;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i = 0; i < g.getChildCount(); i++) {
        WebView w = findWebView(g.getChildAt(i));
        if (w != null) return w;
      }
    }
    return null;
  }
}
