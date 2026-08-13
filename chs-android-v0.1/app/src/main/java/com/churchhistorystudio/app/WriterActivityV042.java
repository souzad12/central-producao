package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV042 extends WriterActivityV041 {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().postDelayed(this::loadTtsLayer, 2300);
  }

  private void loadTtsLayer() {
    try {
      WebView web = findWebView(getWindow().getDecorView());
      if (web == null) return;
      try (InputStream in = getAssets().open("tts_ultrastrict_patch_v042.js"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
