package com.churchhistorystudio.app;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
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

public class CHSStableActivity extends Activity {
  private static final int CSV_PICKER = 5001;
  private static final int BACKUP_EXPORT = 5002;
  private static final int BACKUP_IMPORT = 5003;
  private static final int TEXT_EXPORT = 5004;
  private static final int VAULT_PICKER = 5005;
  private static final String VAULT_PREFS = "chs_vault_v1";
  private static final String VAULT_URI = "tree_uri";

  private WebView web;
  private StoreDb db;
  private ProviderKeyStore providerKeys;
  private SecureApiKeyStore legacyDeepSeek;
  private String pendingBackup;
  private String pendingText;
  private String pendingTextName;
  private boolean bundleLoaded = false;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    getWindow().setStatusBarColor(Color.rgb(17,17,17));
    getWindow().setNavigationBarColor(Color.rgb(17,17,17));
    if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);

    db = new StoreDb();
    providerKeys = new ProviderKeyStore(this);
    legacyDeepSeek = new SecureApiKeyStore(this);

    web = new WebView(this);
    web.setBackgroundColor(Color.rgb(17,17,17));
    setContentView(web);

    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);

    // Todas as pontes existem ANTES do HTML e do bundle serem executados.
    web.addJavascriptInterface(new NativeBridge(), "NativeStore");
    web.addJavascriptInterface(new ProviderBridge(), "AIProviders");
    web.addJavascriptInterface(new VaultBridge(), "CHSVault");
    web.addJavascriptInterface(new CsvBridge(), "CSVBridge");

    installSafeArea();

    web.setWebViewClient(new WebViewClient() {
      @Override public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (!bundleLoaded) {
          bundleLoaded = true;
          loadStableBundle();
        }
      }
    });
    web.loadUrl("file:///android_asset/index_v02.html");
  }

  private void installSafeArea() {
    web.setOnApplyWindowInsetsListener((v, insets) -> {
      int systemTop = Math.max(0, insets.getSystemWindowInsetTop());
      int systemBottom = Math.max(0, insets.getSystemWindowInsetBottom());
      int top = systemTop + dp(18);
      int bottom = systemBottom + dp(26);
      ViewGroup.LayoutParams raw = v.getLayoutParams();
      if (raw instanceof ViewGroup.MarginLayoutParams) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
        lp.topMargin = top;
        lp.bottomMargin = bottom;
        v.setLayoutParams(lp);
      } else {
        v.setPadding(0, top, 0, bottom);
      }
      return insets;
    });
    web.requestApplyInsets();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private void loadStableBundle() {
    try (InputStream in = getAssets().open("stable_bundle_v050.js"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[16384]; int n;
      while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
      web.evaluateJavascript(new String(out.toByteArray(), StandardCharsets.UTF_8), null);
    } catch (Exception e) {
      showMessage("Falha ao carregar CHS v0.5.0 STABLE: " + safeError(e));
    }
  }

  private class StoreDb extends SQLiteOpenHelper {
    StoreDb() { super(CHSStableActivity.this, "church_history_studio.db", null, 1); }
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
    @JavascriptInterface public String storageEngine() { return "SQLite nativo · CHS v0.5.0 STABLE"; }

    // Compatibilidade interna com funções antigas do bundle consolidado.
    @JavascriptInterface public void saveDeepSeekKey(String apiKey) {
      try { providerKeys.save("deepseek", apiKey); legacyDeepSeek.save(apiKey); }
      catch (Exception e) { throw new RuntimeException(safeError(e)); }
    }
    @JavascriptInterface public boolean hasDeepSeekKey() {
      return providerKeys.has("deepseek") || legacyDeepSeek.has();
    }
    @JavascriptInterface public void clearDeepSeekKey() {
      providerKeys.clear("deepseek"); legacyDeepSeek.clear();
    }
    @JavascriptInterface public void testDeepSeek(String model, String requestId) {
      new Thread(() -> {
        try {
          MultiAiClient.Result r = MultiAiClient.chat("deepseek", requireProviderKey("deepseek"), MultiAiClient.safeModel("deepseek", model), "Teste de conectividade. Seja mínimo.", "Responda apenas OK.", 128, false);
          callback("onAiTestResult", requestId, true, r.content.trim());
        } catch (Exception e) { callback("onAiTestResult", requestId, false, safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void prepareEpisode(String payloadJson, String model, String requestId) {
      prepareInternal(payloadJson, "deepseek", model, requestId);
    }
    @JavascriptInterface public void writeAct(String payloadJson, String model, String requestId) {
      writeInternal(payloadJson, "deepseek", model, requestId);
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
    @JavascriptInterface public void exportText(String fileName, String text) {
      pendingText = text == null ? "" : text;
      pendingTextName = (fileName == null || fileName.trim().isEmpty()) ? "CHS_ROTEIRO.txt" : fileName;
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TITLE, pendingTextName);
        startActivityForResult(i, TEXT_EXPORT);
      });
    }
  }

  private class ProviderBridge {
    @JavascriptInterface public void saveKey(String provider, String apiKey) {
      try {
        String p = MultiAiClient.safeProvider(provider);
        providerKeys.save(p, apiKey);
        if ("deepseek".equals(p)) legacyDeepSeek.save(apiKey);
      } catch (Exception e) { throw new RuntimeException(safeError(e)); }
    }
    @JavascriptInterface public boolean hasKey(String provider) {
      String p = MultiAiClient.safeProvider(provider);
      if (providerKeys.has(p)) return true;
      return "deepseek".equals(p) && legacyDeepSeek.has();
    }
    @JavascriptInterface public void clearKey(String provider) {
      String p = MultiAiClient.safeProvider(provider);
      providerKeys.clear(p);
      if ("deepseek".equals(p)) legacyDeepSeek.clear();
    }
    @JavascriptInterface public double estimateUsd(String provider, String model, int inputTokens, int outputTokens) {
      return MultiAiClient.estimateUsd(provider, model, Math.max(0,inputTokens), Math.max(0,outputTokens));
    }
    @JavascriptInterface public void test(String provider, String model, String requestId) {
      final String p = MultiAiClient.safeProvider(provider), m = MultiAiClient.safeModel(p, model);
      new Thread(() -> {
        try {
          MultiAiClient.Result r = MultiAiClient.chat(p, requireProviderKey(p), m, "Teste de conectividade. Seja mínimo.", "Responda apenas OK.", 128, false);
          callback("onProviderTestResult", requestId, true, r.content.trim());
        } catch (Exception e) { callback("onProviderTestResult", requestId, false, safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void prepare(String payloadJson, String provider, String model, String requestId) {
      prepareInternal(payloadJson, provider, model, requestId);
    }
    @JavascriptInterface public void writeAct(String payloadJson, String provider, String model, String requestId) {
      writeInternal(payloadJson, provider, model, requestId);
    }
  }

  private void prepareInternal(String payloadJson, String provider, String model, String requestId) {
    final String p = MultiAiClient.safeProvider(provider), m = MultiAiClient.safeModel(p, model);
    new Thread(() -> {
      try {
        MultiAiClient.Result r = MultiAiClient.chat(p, requireProviderKey(p), m, PreparationPrompts.system(), PreparationPrompts.user(payloadJson), 18000, true);
        JSONObject obj = new JSONObject(MultiAiClient.normalizeJsonObject(r.content));
        obj.put("_usage", r.usageJson()); obj.put("_provider", p); obj.put("_model", m);
        callback("onAiPreparationResult", requestId, true, obj.toString());
      } catch (Exception e) { callback("onAiPreparationResult", requestId, false, safeError(e)); }
    }).start();
  }

  private void writeInternal(String payloadJson, String provider, String model, String requestId) {
    final String p = MultiAiClient.safeProvider(provider), m = MultiAiClient.safeModel(p, model);
    new Thread(() -> {
      MultiAiClient.Result first = null;
      try {
        String user = WriterPrompts.user(payloadJson);
        try {
          first = MultiAiClient.chat(p, requireProviderKey(p), m, WriterPrompts.system(), user, 15000, true);
          JSONObject obj = validatedWriter(first.content);
          obj.put("_usage", first.usageJson()); obj.put("_provider", p); obj.put("_model", m);
          callback("onAiWritingResult", requestId, true, obj.toString());
          return;
        } catch (Exception initial) {
          String retry = user + "\n\nNOVA TENTATIVA: a resposta anterior não pôde ser validada. Reescreva o ato completo em JSON estrito, mantendo duração, cenas, Style Router, Anti-Repetição, Continuity Guard e TTS Ultraestrito.";
          MultiAiClient.Result second = MultiAiClient.chat(p, requireProviderKey(p), m, WriterPrompts.system(), retry, 18000, true);
          JSONObject obj = validatedWriter(second.content);
          if (first != null) {
            int in = first.inputTokens + second.inputTokens, out = first.outputTokens + second.outputTokens;
            double cost = first.costUsd + second.costUsd;
            obj.put("_usage", second.usageJson(in, out, cost, true));
          } else obj.put("_usage", second.usageJson());
          obj.put("_provider", p); obj.put("_model", m);
          callback("onAiWritingResult", requestId, true, obj.toString());
        }
      } catch (Exception e) { callback("onAiWritingResult", requestId, false, safeError(e)); }
    }).start();
  }

  private JSONObject validatedWriter(String raw) throws Exception {
    JSONObject obj = new JSONObject(MultiAiClient.normalizeJsonObject(raw));
    if (obj.optJSONArray("scenes") == null || obj.optJSONArray("scenes").length() == 0) throw new Exception("JSON sem cenas escritas.");
    return obj;
  }

  private String requireProviderKey(String provider) throws Exception {
    String key = null;
    try { key = providerKeys.read(provider); } catch (Exception ignored) {}
    if ((key == null || key.isEmpty()) && "deepseek".equals(provider)) key = legacyDeepSeek.read();
    if (key == null || key.isEmpty()) throw new Exception("API Key " + provider + " não configurada.");
    return key;
  }

  private class CsvBridge {
    @JavascriptInterface public boolean ready() { return true; }
    @JavascriptInterface public void pickCsv() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv","text/plain","application/csv","application/vnd.ms-excel"});
        startActivityForResult(i, CSV_PICKER);
      });
    }
  }

  private class VaultBridge {
    @JavascriptInterface public boolean hasFolder() { return getVaultUri() != null; }
    @JavascriptInterface public String folderName() { Uri u = getVaultUri(); return u == null ? "" : u.getLastPathSegment(); }
    @JavascriptInterface public void chooseFolder() {
      runOnUiThread(() -> {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i, VAULT_PICKER);
      });
    }
    @JavascriptInterface public void clearFolder() { getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE).edit().remove(VAULT_URI).apply(); }
    @JavascriptInterface public String saveFile(String relativePath, String text) {
      try { saveVaultFile(relativePath, text == null ? "" : text); return "OK"; }
      catch (Exception e) { return "ERROR: " + safeError(e); }
    }
  }

  private Uri getVaultUri() {
    String raw = getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE).getString(VAULT_URI, null);
    if (raw == null || raw.isEmpty()) return null;
    try { return Uri.parse(raw); } catch (Exception e) { return null; }
  }

  private void saveVaultFile(String relativePath, String text) throws Exception {
    Uri tree = getVaultUri(); if (tree == null) throw new Exception("Cofre CHS não configurado.");
    String clean = relativePath == null ? "" : relativePath.replace('\\','/').replaceAll("/+","/");
    if (clean.startsWith("/") || clean.contains("..") || clean.trim().isEmpty()) throw new Exception("Caminho inválido no Cofre.");
    String[] parts = clean.split("/");
    Uri parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
    for (int i=0; i<parts.length-1; i++) {
      String name = parts[i].trim(); if (name.isEmpty()) continue;
      Uri child = findChild(tree, parent, name);
      if (child == null) child = DocumentsContract.createDocument(getContentResolver(), parent, DocumentsContract.Document.MIME_TYPE_DIR, name);
      if (child == null) throw new Exception("Não foi possível criar pasta " + name);
      parent = child;
    }
    String fileName = parts[parts.length-1].trim(); if (fileName.isEmpty()) throw new Exception("Nome de arquivo vazio.");
    Uri file = findChild(tree, parent, fileName);
    if (file == null) {
      String mime = fileName.endsWith(".json") ? "application/json" : "text/plain";
      file = DocumentsContract.createDocument(getContentResolver(), parent, mime, fileName);
    }
    if (file == null) throw new Exception("Não foi possível criar " + fileName);
    try (OutputStream out = getContentResolver().openOutputStream(file, "wt")) {
      if (out == null) throw new Exception("Sem acesso de escrita ao Cofre.");
      out.write(text.getBytes(StandardCharsets.UTF_8)); out.flush();
    }
  }

  private Uri findChild(Uri tree, Uri parent, String name) throws Exception {
    ContentResolver r = getContentResolver();
    Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent));
    String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME};
    try (Cursor c = r.query(children, projection, null, null, null)) {
      if (c != null) while (c.moveToNext()) if (name.equals(c.getString(1))) return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0));
    }
    return null;
  }

  private String readUri(Uri uri, int maxBytes) throws Exception {
    try (InputStream in = getContentResolver().openInputStream(uri); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      if (in == null) throw new Exception("O Android não forneceu acesso ao arquivo.");
      byte[] buffer = new byte[8192]; int read, total = 0;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > maxBytes) throw new Exception("Arquivo maior que o limite permitido.");
        out.write(buffer, 0, read);
      }
      if (total == 0) throw new Exception("Arquivo vazio (0 B).");
      return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
  }

  private void importCsv(Uri uri) {
    try {
      String text = readUri(uri, 10 * 1024 * 1024);
      String js = "try{if(typeof importCSV!=='function')throw new Error('Importador CSV não carregado');importCSV(" + JSONObject.quote(text) + ")}catch(e){flash('Falha ao importar: '+(e&&e.message?e.message:e))}";
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

  private void writeText(Uri uri) {
    if (uri == null || pendingText == null) return;
    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
      if (out == null) throw new Exception("Sem acesso ao destino.");
      out.write(pendingText.getBytes(StandardCharsets.UTF_8)); out.flush();
      showMessage("Roteiro exportado com sucesso.");
    } catch (Exception e) { showMessage("Falha ao exportar roteiro: " + safeError(e)); }
    finally { pendingText = null; pendingTextName = null; }
  }

  private void restoreBackup(Uri uri) {
    try {
      String text = readUri(uri, 25 * 1024 * 1024);
      String js = "try{restoreBackupFromNative(" + JSONObject.quote(text) + ")}catch(e){flash('Backup inválido: '+(e&&e.message?e.message:e))}";
      web.post(() -> web.evaluateJavascript(js, null));
    } catch (Exception e) { showMessage("Falha ao ler backup: " + safeError(e)); }
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Uri selected = resultCode == RESULT_OK && data != null ? data.getData() : null;
    if (requestCode == CSV_PICKER) { if (resultCode == RESULT_OK && selected != null) importCsv(selected); return; }
    if (requestCode == BACKUP_EXPORT) { if (resultCode == RESULT_OK) writeBackup(selected); else pendingBackup = null; return; }
    if (requestCode == BACKUP_IMPORT) { if (resultCode == RESULT_OK && selected != null) restoreBackup(selected); return; }
    if (requestCode == TEXT_EXPORT) { if (resultCode == RESULT_OK) writeText(selected); else { pendingText = null; pendingTextName = null; } return; }
    if (requestCode == VAULT_PICKER) {
      if (resultCode == RESULT_OK && selected != null) {
        int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try { getContentResolver().takePersistableUriPermission(selected, flags); } catch (Exception ignored) {}
        getSharedPreferences(VAULT_PREFS, Context.MODE_PRIVATE).edit().putString(VAULT_URI, selected.toString()).apply();
        showJs("try{onVaultFolderSelected(" + JSONObject.quote(selected.getLastPathSegment()) + ")}catch(e){try{render()}catch(x){}};");
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private void callback(String fn, String requestId, boolean ok, String data) {
    String js = fn + "(" + JSONObject.quote(requestId) + "," + (ok ? "true" : "false") + "," + JSONObject.quote(data == null ? "" : data) + ")";
    web.post(() -> web.evaluateJavascript(js, null));
  }

  private void showMessage(String message) {
    showJs("try{flash(" + JSONObject.quote(message) + ")}catch(e){}");
  }

  private void showJs(String js) {
    if (web != null) web.post(() -> web.evaluateJavascript(js, null));
  }

  private String safeError(Exception e) {
    String m = e.getMessage();
    return m == null || m.trim().isEmpty() ? e.getClass().getSimpleName() : m;
  }
}
