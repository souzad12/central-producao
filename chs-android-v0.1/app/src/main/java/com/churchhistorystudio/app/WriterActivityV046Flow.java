package com.churchhistorystudio.app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV046Flow extends WriterActivityV046 {
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().postDelayed(this::migrateLifecycle, 7200);
  }
  private void migrateLifecycle() {
    try {
      WebView web=findWebView(getWindow().getDecorView());
      if(web==null)return;
      try(InputStream in=getAssets().open("lifecycle_migration_v046.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
        web.evaluateJavascript(new String(out.toByteArray(),StandardCharsets.UTF_8),null);
      }
    } catch(Exception ignored) {}
  }
  private WebView findWebView(View v){if(v instanceof WebView)return(WebView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView w=findWebView(g.getChildAt(i));if(w!=null)return w;}}return null;}
}
