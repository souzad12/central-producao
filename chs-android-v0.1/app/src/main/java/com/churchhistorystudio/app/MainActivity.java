package com.churchhistorystudio.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER = 1001;
  private static final int BACKUP_EXPORT = 2001;
  private static final int BACKUP_IMPORT = 2002;
  private ValueCallback<Uri[]> fileCallback;
  private WebView web;
  private StoreDb db;
  private SecureApiKeyStore secureKeys;
  private String pendingBackup;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    db = new StoreDb();
    secureKeys = new SecureApiKeyStore(this);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    web.addJavascriptInterface(new NativeBridge(), "NativeStore");
    web.setWebViewClient(new WebViewClient() {
      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        injectAiPatch();
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv","text/plain","application/csv"});
        try { startActivityForResult(i, FILE_CHOOSER); return true; }
        catch (Exception e) { fileCallback = null; showMessage("Não foi possível abrir o seletor de arquivos."); return false; }
      }
    });
    web.loadUrl("file:///android_asset/index_v02.html");
  }

  private void injectAiPatch() {
    try (InputStream in = getAssets().open("ai_patch_v03.js")) {
      String script = readStream(in, 512 * 1024);
      web.evaluateJavascript(script, null);
    } catch (Exception e) { showMessage("Falha ao carregar módulo de IA: " + safeError(e)); }
  }

  private class StoreDb extends SQLiteOpenHelper {
    StoreDb() { super(MainActivity.this, "church_history_studio.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase d) { d.execSQL("CREATE TABLE kv_store (k TEXT PRIMARY KEY, v TEXT NOT NULL)"); }
    @Override public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {}
    synchronized String getValue(String key) {
      try (Cursor c = getReadableDatabase().query("kv_store", new String[]{"v"}, "k=?", new String[]{key}, null, null, null)) {
        return c.moveToFirst() ? c.getString(0) : null;
      }
    }
    synchronized void setValue(String key, String value) {
      ContentValues cv = new ContentValues();
      cv.put("k", key); cv.put("v", value == null ? "" : value);
      getWritableDatabase().insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
  }

  private class NativeBridge {
    @JavascriptInterface public String get(String key) { return db.getValue(key); }
    @JavascriptInterface public void set(String key, String value) { db.setValue(key, value); }
    @JavascriptInterface public String storageEngine() { return "SQLite nativo · church_history_studio.db"; }
    @JavascriptInterface public void saveDeepSeekKey(String apiKey) {
      try { secureKeys.save(apiKey); }
      catch (Exception e) { throw new RuntimeException(e.getMessage()); }
    }
    @JavascriptInterface public boolean hasDeepSeekKey() { return secureKeys.has(); }
    @JavascriptInterface public void clearDeepSeekKey() { secureKeys.clear(); }
    @JavascriptInterface public void testDeepSeek(String model, String requestId) {
      new Thread(() -> {
        try {
          String content = DeepSeekClient.chat(requireKey(), model, "Teste de conectividade. Seja mínimo.", "Responda apenas OK.", 32, false);
          callback("onAiTestResult", requestId, true, content.trim());
        } catch (Exception e) { callback("onAiTestResult", requestId, false, safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void prepareEpisode(String payloadJson, String model, String requestId) {
      new Thread(() -> {
        try {
          String content = DeepSeekClient.chat(requireKey(), model, PreparationPrompts.system(), PreparationPrompts.user(payloadJson), 16000, true);
          content = DeepSeekClient.normalizeJsonObject(content);
          new JSONObject(content);
          callback("onAiPreparationResult", requestId, true, content);
        } catch (Exception e) { callback("onAiPreparationResult", requestId, false, safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void exportBackup(String json) {
      pendingBackup = json;
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(new Date());
        i.putExtra(Intent.EXTRA_TITLE, "CHS_BACKUP_" + stamp + ".json");
        startActivityForResult(i, BACKUP_EXPORT);
      });
    }
    @JavascriptInterface public void importBackup() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json","text/plain"});
        startActivityForResult(i, BACKUP_IMPORT);
      });
    }
  }

  private String requireKey() throws Exception {
    String key = secureKeys.read();
    if (key == null || key.isEmpty()) throw new Exception("API Key DeepSeek não configurada.");
    return key;
  }
  private String safeError(Exception e) {
    String m = e.getMessage();
    return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
  }
  private void callback(String fn, String requestId, boolean ok, String data) {
    String js = fn + "(" + JSONObject.quote(requestId) + "," + (ok ? "true" : "false") + "," + JSONObject.quote(data == null ? "" : data) + ")";
    web.post(() -> web.evaluateJavascript(js, null));
  }
  private void showMessage(String message) {
    String js = "flash(" + JSONObject.quote(message) + ")";
    web.post(() -> web.evaluateJavascript(js, null));
  }

  private String readStream(InputStream in, int maxBytes) throws Exception {
    try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[8192]; int read, total = 0;
      while ((read = input.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) throw new Exception("Conteúdo maior que o limite permitido.");
        out.write(buffer, 0, read);
      }
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }
  private String readUri(Uri uri, int maxBytes) throws Exception {
    try (InputStream in = getContentResolver().openInputStream(uri)) {
      if (in == null) throw new Exception("O Android não forneceu acesso ao arquivo.");
      String text = readStream(in, maxBytes);
      if (text.isEmpty()) throw new Exception("Arquivo vazio (0 B).");
      return text;
    }
  }
  private void importCsv(Uri uri) {
    try {
      String text = readUri(uri, 5 * 1024 * 1024);
      String js = "try{importCSV(" + JSONObject.quote(text) + ")}catch(e){flash('Falha ao importar: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) { showMessage("Falha ao ler CSV: " + safeError(e)); }
  }
  private void writeBackup(Uri uri) {
    if (uri == null || pendingBackup == null) return;
    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
      if (out == null) throw new Exception("Sem acesso ao destino.");
      out.write(pendingBackup.getBytes(StandardCharsets.UTF_8)); out.flush();
      showMessage("Backup exportado com sucesso.");
    } catch (Exception e) { showMessage("Falha ao exportar backup: " + safeError(e)); }
    finally { pendingBackup = null; }
  }
  private void restoreBackup(Uri uri) {
    try {
      String text = readUri(uri, 15 * 1024 * 1024);
      String js = "try{restoreBackupFromNative(" + JSONObject.quote(text) + ")}catch(e){flash('Backup inválido: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) { showMessage("Falha ao ler backup: " + safeError(e)); }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Uri selected = resultCode == RESULT_OK && data != null ? data.getData() : null;
    if (requestCode == FILE_CHOOSER) {
      if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
      if (resultCode == RESULT_OK && selected != null) importCsv(selected);
      return;
    }
    if (requestCode == BACKUP_EXPORT) { if (resultCode == RESULT_OK) writeBackup(selected); else pendingBackup = null; return; }
    if (requestCode == BACKUP_IMPORT) { if (resultCode == RESULT_OK && selected != null) restoreBackup(selected); return; }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
