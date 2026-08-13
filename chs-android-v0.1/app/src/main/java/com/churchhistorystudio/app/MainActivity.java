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
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER = 1001;
  private ValueCallback<Uri[]> fileCallback;
  private WebView web;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    web.setWebViewClient(new WebViewClient() {
      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        view.evaluateJavascript("document.querySelector('.sub').textContent='SHOWRUNNER HISTÓRICO · ANDROID v0.1.1';", null);
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        Intent intent = params.createIntent();
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv"});
        try {
          startActivityForResult(intent, FILE_CHOOSER);
          return true;
        } catch (Exception e) {
          fileCallback = null;
          showMessage("Não foi possível abrir o seletor de arquivos.");
          return false;
        }
      }
    });
    web.loadUrl("file:///android_asset/index.html");
  }

  private void showMessage(String message) {
    final String js = "flash(" + JSONObject.quote(message) + ")";
    web.post(() -> web.evaluateJavascript(js, null));
  }

  private void importCsvUri(Uri uri) {
    if (uri == null) {
      showMessage("Nenhum arquivo foi selecionado.");
      return;
    }
    try (InputStream in = getContentResolver().openInputStream(uri);
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (in == null) throw new Exception("O Android não forneceu acesso ao arquivo.");
      byte[] buffer = new byte[8192];
      int read;
      int total = 0;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > 5 * 1024 * 1024) throw new Exception("CSV maior que 5 MB.");
        out.write(buffer, 0, read);
      }
      if (total == 0) {
        showMessage("Este CSV está vazio (0 B). Escolha o arquivo de aproximadamente 57 kB.");
        return;
      }
      String text = new String(out.toByteArray(), StandardCharsets.UTF_8);
      final String js = "try{importCSV(" + JSONObject.quote(text) + ")}catch(e){flash('Falha ao importar: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) {
      showMessage("Falha ao ler CSV: " + e.getMessage());
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FILE_CHOOSER) {
      Uri selected = null;
      if (resultCode == RESULT_OK && data != null) {
        if (data.getData() != null) selected = data.getData();
        else if (data.getClipData() != null && data.getClipData().getItemCount() > 0) selected = data.getClipData().getItemAt(0).getUri();
      }

      // Cancelamos a leitura pelo WebView e fazemos a leitura pelo Android nativo.
      if (fileCallback != null) {
        fileCallback.onReceiveValue(null);
        fileCallback = null;
      }
      if (resultCode == RESULT_OK) importCsvUri(selected);
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
