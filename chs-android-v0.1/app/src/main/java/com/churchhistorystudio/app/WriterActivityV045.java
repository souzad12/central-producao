package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV045 extends WriterActivityV044 {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().getDecorView().postDelayed(this::installStyleRouter, 4700);
  }
  @Override protected void onResume() {
    super.onResume();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }
  private void installStyleRouter() {
    try {
      WebView web=findWebView(getWindow().getDecorView());
      if(web==null)return;
      InputStream in=getAssets().open("style_router_v045.js");
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] buf=new byte[4096]; int n;
      while((n=in.read(buf))!=-1)out.write(buf,0,n);
      in.close();
      web.evaluateJavascript(new String(out.toByteArray(),StandardCharsets.UTF_8),null);
    } catch(Exception ignored) {}
  }
  private WebView findWebView(View v) {
    if(v instanceof WebView)return (WebView)v;
    if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView w=findWebView(g.getChildAt(i));if(w!=null)return w;}}
    return null;
  }
}
