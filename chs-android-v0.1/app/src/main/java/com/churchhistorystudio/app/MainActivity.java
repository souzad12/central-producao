package com.churchhistorystudio.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER = 1001;
  private ValueCallback<Uri[]> fileCallback;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    WebView web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        Intent intent = params.createIntent();
        try { startActivityForResult(intent, FILE_CHOOSER); return true; }
        catch (Exception e) { fileCallback = null; return false; }
      }
    });
    web.loadUrl("file:///android_asset/index.html");
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FILE_CHOOSER && fileCallback != null) {
      Uri[] results = null;
      if (resultCode == RESULT_OK && data != null && data.getData() != null) results = new Uri[]{data.getData()};
      fileCallback.onReceiveValue(results);
      fileCallback = null;
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
