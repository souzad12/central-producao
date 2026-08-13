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
  private String pendingBackup = null;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    db = new StoreDb();
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    web.addJavascriptInterface(new NativeBridge(), "NativeStore");
    web.setWebViewClient(new WebViewClient());
    web.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values", "text/plain", "application/csv"});
        try { startActivityForResult(intent, FILE_CHOOSER); return true; }
        catch (Exception e) { fileCallback = null; showMessage("Não foi possível abrir o seletor de arquivos."); return false; }
      }
    });
    web.loadUrl("file:///android_asset/index_v02.html");
  }

  private class StoreDb extends SQLiteOpenHelper {
    StoreDb() { super(MainActivity.this, "church_history_studio.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase d) {
      d.execSQL("CREATE TABLE kv_store (k TEXT PRIMARY KEY, v TEXT NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase d, int oldVersion, int newVersion) {}
    synchronized String getValue(String key) {
      try (Cursor c = getReadableDatabase().query("kv_store", new String[]{"v"}, "k=?", new String[]{key}, null, null, null)) {
        return c.moveToFirst() ? c.getString(0) : null;
      }
    }
    synchronized void setValue(String key, String value) {
      ContentValues cv = new ContentValues(); cv.put("k", key); cv.put("v", value == null ? "" : value);
      getWritableDatabase().insertWithOnConflict("kv_store", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
  }

  private class NativeBridge {
    @JavascriptInterface public String get(String key) { return db.getValue(key); }
    @JavascriptInterface public void set(String key, String value) { db.setValue(key, value); }
    @JavascriptInterface public String storageEngine() { return "SQLite nativo · church_history_studio.db"; }
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
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain"});
        startActivityForResult(i, BACKUP_IMPORT);
      });
    }
  }

  private void showMessage(String message) {
    final String js = "flash(" + JSONObject.quote(message) + ")";
    web.post(() -> web.evaluateJavascript(js, null));
  }

  private String readUri(Uri uri, int maxBytes) throws Exception {
    try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (in == null) throw new Exception("O Android não forneceu acesso ao arquivo.");
      byte[] buffer = new byte[8192]; int read, total = 0;
      while ((read = in.read(buffer)) != -1) {
        total += read; if (total > maxBytes) throw new Exception("Arquivo maior que o limite permitido.");
        out.write(buffer, 0, read);
      }
      if (total == 0) throw new Exception("Arquivo vazio (0 B).");
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private void importCsvUri(Uri uri) {
    if (uri == null) { showMessage("Nenhum arquivo foi selecionado."); return; }
    try {
      String text = readUri(uri, 5 * 1024 * 1024);
      final String js = "try{importCSV(" + JSONObject.quote(text) + ")}catch(e){flash('Falha ao importar: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) { showMessage("Falha ao ler CSV: " + e.getMessage()); }
  }

  private void writeBackup(Uri uri) {
    if (uri == null || pendingBackup == null) { showMessage("Backup cancelado."); return; }
    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
      if (out == null) throw new Exception("Sem acesso ao destino.");
      out.write(pendingBackup.getBytes(StandardCharsets.UTF_8)); out.flush();
      showMessage("Backup exportado com sucesso.");
    } catch (Exception e) { showMessage("Falha ao exportar backup: " + e.getMessage()); }
    finally { pendingBackup = null; }
  }

  private void restoreBackup(Uri uri) {
    if (uri == null) { showMessage("Restauração cancelada."); return; }
    try {
      String text = readUri(uri, 10 * 1024 * 1024);
      final String js = "try{restoreBackupFromNative(" + JSONObject.quote(text) + ")}catch(e){flash('Backup inválido: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) { showMessage("Falha ao ler backup: " + e.getMessage()); }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Uri selected = resultCode == RESULT_OK && data != null ? data.getData() : null;
    if (requestCode == FILE_CHOOSER) {
      if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
      if (resultCode == RESULT_OK) importCsvUri(selected);
      return;
    }
    if (requestCode == BACKUP_EXPORT) { if (resultCode == RESULT_OK) writeBackup(selected); else pendingBackup = null; return; }
    if (requestCode == BACKUP_IMPORT) { if (resultCode == RESULT_OK) restoreBackup(selected); return; }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
