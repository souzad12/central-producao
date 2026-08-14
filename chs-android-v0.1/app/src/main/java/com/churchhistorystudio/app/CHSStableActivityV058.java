package com.churchhistorystudio.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CHSStableActivityV058 extends CHSStableActivityV057 {
  private static final int CSV_PICKER_V058 = 5801;
  private WebView csvWeb;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    csvWeb = findWebView(getWindow().getDecorView());
    if (csvWeb != null) {
      // Substitui SOMENTE a ponte CSV. A Lixeira e os diálogos da v0.5.7 permanecem intactos.
      try { csvWeb.removeJavascriptInterface("CSVBridge"); } catch (Exception ignored) {}
      csvWeb.addJavascriptInterface(new CsvBridgeV058(), "CSVBridge");
    }
  }

  private class CsvBridgeV058 {
    @JavascriptInterface public boolean ready() { return true; }

    @JavascriptInterface public void pickCsv() {
      runOnUiThread(() -> {
        try {
          Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          i.addCategory(Intent.CATEGORY_OPENABLE);
          // Não confiar no MIME do gerenciador de arquivos: alguns Xiaomi marcam CSV como text/plain ou octet-stream.
          i.setType("*/*");
          i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
          i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
          startActivityForResult(i, CSV_PICKER_V058);
        } catch (Exception e) {
          showNativeError("Não foi possível abrir o seletor de arquivos: " + safeMessage(e));
        }
      });
    }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != CSV_PICKER_V058) {
      super.onActivityResult(requestCode, resultCode, data);
      return;
    }

    if (resultCode != RESULT_OK) {
      Toast.makeText(this, "Seleção de CSV cancelada.", Toast.LENGTH_SHORT).show();
      return;
    }

    Uri uri = extractSelectedUri(data);
    if (uri == null) {
      showNativeError("O gerenciador de arquivos retornou a seleção sem um endereço de arquivo (URI). Tente selecionar o CSV novamente.");
      return;
    }

    try {
      int flags = data == null ? Intent.FLAG_GRANT_READ_URI_PERMISSION :
        data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      if (flags != 0) {
        try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (Exception ignored) {}
      }

      final String text = readCsv(uri);
      final WebView web = csvWeb != null ? csvWeb : findWebView(getWindow().getDecorView());
      if (web == null) throw new Exception("WebView do CHS não disponível.");

      final String js = "(function(){try{" +
        "if(typeof window.importCSV!=='function')throw new Error('Importador CSV não carregado');" +
        "window.importCSV(" + JSONObject.quote(text) + ");return true;" +
        "}catch(e){try{flash('Falha ao importar: '+(e&&e.message?e.message:e))}catch(_e){};return false;}})()";

      web.post(() -> web.evaluateJavascript(js, value -> runOnUiThread(() -> {
        if ("true".equals(String.valueOf(value))) {
          Toast.makeText(CHSStableActivityV058.this, "CSV selecionado e processado pelo CHS.", Toast.LENGTH_SHORT).show();
        } else {
          showNativeError("O arquivo foi selecionado, mas o CHS não conseguiu importá-lo. A mensagem de erro também deve aparecer dentro do aplicativo.");
        }
      })));
    } catch (Exception e) {
      showNativeError("Falha ao ler o CSV selecionado: " + safeMessage(e));
    }
  }

  private Uri extractSelectedUri(Intent data) {
    if (data == null) return null;
    Uri direct = data.getData();
    if (direct != null) return direct;
    ClipData clip = data.getClipData();
    if (clip != null && clip.getItemCount() > 0 && clip.getItemAt(0) != null) {
      return clip.getItemAt(0).getUri();
    }
    return null;
  }

  private String readCsv(Uri uri) throws Exception {
    try (InputStream in = getContentResolver().openInputStream(uri);
         ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (in == null) throw new Exception("O Android não concedeu acesso de leitura ao arquivo.");
      byte[] buf = new byte[8192];
      int n, total = 0;
      while ((n = in.read(buf)) != -1) {
        total += n;
        if (total > 10 * 1024 * 1024) throw new Exception("CSV maior que 10 MB.");
        out.write(buf, 0, n);
      }
      if (total == 0) throw new Exception("O arquivo selecionado está vazio.");
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private void showNativeError(String message) {
    runOnUiThread(() -> new AlertDialog.Builder(this)
      .setTitle("Importação CSV")
      .setMessage(message)
      .setPositiveButton("OK", null)
      .show());
  }

  private String safeMessage(Exception e) {
    String m = e.getMessage();
    return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
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
