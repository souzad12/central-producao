package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class WriterActivityV043 extends WriterActivityV042 {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().postDelayed(this::installHotfixUi, 3200);
  }

  private void installHotfixUi() {
    WebView web = findWebView(getWindow().getDecorView());
    if (web == null) return;
    String js = "(function(){" +
      "var s=document.querySelector('.sub');if(s)s.textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.4.3 · HOTFIX';" +
      "var t=document.getElementById('topStatus');if(t)t.textContent='SQLITE + IA + WRITER + TTS + FIX';" +
      "window.addEventListener('error',function(e){try{flash('Erro interno: '+(e.message||'JavaScript'));}catch(x){}});" +
      "window.addEventListener('unhandledrejection',function(e){try{flash('Erro interno: '+((e.reason&&e.reason.message)||e.reason||'Promise'));}catch(x){}});" +
      "})();";
    web.evaluateJavascript(js, null);
  }

  private WebView findWebView(View v) {
    if (v instanceof WebView) return (WebView) v;
    if (v instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) v;
      for (int i=0;i<g.getChildCount();i++) {
        WebView w=findWebView(g.getChildAt(i));
        if(w!=null)return w;
      }
    }
    return null;
  }
}
