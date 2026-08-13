package com.churchhistorystudio.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV046 extends WriterActivityV045 {
  private static final int VAULT_PICKER = 4601;
  private static final String VAULT_PREFS = "chs_vault_v1";
  private static final String VAULT_URI = "tree_uri";
  private WebView providerWeb;
  private ProviderKeyStore providerKeys;
  private SecureApiKeyStore legacyDeepSeek;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    providerKeys = new ProviderKeyStore(this);
    legacyDeepSeek = new SecureApiKeyStore(this);
    providerWeb = findWebView(getWindow().getDecorView());
    if (providerWeb != null) {
      providerWeb.addJavascriptInterface(new ProviderBridge(), "AIProviders");
      providerWeb.addJavascriptInterface(new VaultBridge(), "CHSVault");
      installSafeArea(providerWeb);
      getWindow().getDecorView().postDelayed(this::installV046Patch, 6200);
    }
  }

  private void installSafeArea(WebView web) {
    final int extraTop = dp(10), extraBottom = dp(18);
    web.setOnApplyWindowInsetsListener((v, insets) -> {
      int top = Math.max(0, insets.getSystemWindowInsetTop()) + extraTop;
      int bottom = Math.max(0, insets.getSystemWindowInsetBottom()) + extraBottom;
      v.setPadding(0, top, 0, bottom);
      return insets;
    });
    web.requestApplyInsets();
  }

  private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

  private void installV046Patch() {
    try {
      if (providerWeb == null) return;
      InputStream in = getAssets().open("provider_vault_v046.js");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192]; int n;
      while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
      in.close();
      providerWeb.evaluateJavascript(new String(out.toByteArray(), StandardCharsets.UTF_8), null);
    } catch (Exception e) { showJs("flash('Falha ao carregar recursos v0.4.6: '+" + JSONObject.quote(safeError(e)) + ")"); }
  }

  private class ProviderBridge {
    @JavascriptInterface public void saveKey(String provider, String apiKey) {
      try {
        provider = MultiAiClient.safeProvider(provider);
        providerKeys.save(provider, apiKey);
        if ("deepseek".equals(provider)) legacyDeepSeek.save(apiKey);
      } catch (Exception e) { throw new RuntimeException(safeError(e)); }
    }
    @JavascriptInterface public boolean hasKey(String provider) {
      provider = MultiAiClient.safeProvider(provider);
      if (providerKeys.has(provider)) return true;
      return "deepseek".equals(provider) && legacyDeepSeek.has();
    }
    @JavascriptInterface public void clearKey(String provider) {
      provider = MultiAiClient.safeProvider(provider);
      providerKeys.clear(provider);
      if ("deepseek".equals(provider)) legacyDeepSeek.clear();
    }
    @JavascriptInterface public double estimateUsd(String provider, String model, int inputTokens, int outputTokens) {
      return MultiAiClient.estimateUsd(provider, model, Math.max(0,inputTokens), Math.max(0,outputTokens));
    }
    @JavascriptInterface public void test(String provider, String model, String requestId) {
      final String p=MultiAiClient.safeProvider(provider), m=MultiAiClient.safeModel(p,model);
      new Thread(() -> {
        try {
          MultiAiClient.Result r=MultiAiClient.chat(p, requireProviderKey(p), m, "Teste de conectividade. Seja mínimo.", "Responda apenas OK.", 128, false);
          callback("onProviderTestResult", requestId, true, r.content.trim());
        } catch (Exception e) { callback("onProviderTestResult", requestId, false, safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void prepare(String payloadJson, String provider, String model, String requestId) {
      final String p=MultiAiClient.safeProvider(provider), m=MultiAiClient.safeModel(p,model);
      new Thread(() -> {
        try {
          MultiAiClient.Result r=MultiAiClient.chat(p, requireProviderKey(p), m, PreparationPrompts.system(), PreparationPrompts.user(payloadJson), 18000, true);
          String normalized=MultiAiClient.normalizeJsonObject(r.content);
          JSONObject obj=new JSONObject(normalized);
          obj.put("_usage",r.usageJson()); obj.put("_provider",p); obj.put("_model",m);
          callback("onAiPreparationResult",requestId,true,obj.toString());
        } catch (Exception e) { callback("onAiPreparationResult",requestId,false,safeError(e)); }
      }).start();
    }
    @JavascriptInterface public void writeAct(String payloadJson, String provider, String model, String requestId) {
      final String p=MultiAiClient.safeProvider(provider), m=MultiAiClient.safeModel(p,model);
      new Thread(() -> {
        MultiAiClient.Result first=null;
        try {
          String user=WriterPrompts.user(payloadJson);
          try {
            first=MultiAiClient.chat(p,requireProviderKey(p),m,WriterPrompts.system(),user,15000,true);
            JSONObject obj=validatedWriter(first.content);
            obj.put("_usage",first.usageJson());obj.put("_provider",p);obj.put("_model",m);
            callback("onAiWritingResult",requestId,true,obj.toString());return;
          } catch (Exception initial) {
            String retry=user+"\n\nNOVA TENTATIVA: a resposta anterior não pôde ser validada. Reescreva o ato completo em JSON estrito, mantendo duração, cenas, Style Router, Anti-Repetição, Continuity Guard e TTS Ultraestrito.";
            MultiAiClient.Result second=MultiAiClient.chat(p,requireProviderKey(p),m,WriterPrompts.system(),retry,18000,true);
            JSONObject obj=validatedWriter(second.content);
            if(first!=null){int in=first.inputTokens+second.inputTokens,out=first.outputTokens+second.outputTokens;double cost=first.costUsd+second.costUsd;obj.put("_usage",second.usageJson(in,out,cost,true));}
            else obj.put("_usage",second.usageJson());
            obj.put("_provider",p);obj.put("_model",m);
            callback("onAiWritingResult",requestId,true,obj.toString());
          }
        } catch (Exception e) { callback("onAiWritingResult",requestId,false,safeError(e)); }
      }).start();
    }
  }

  private JSONObject validatedWriter(String raw) throws Exception {
    JSONObject obj=new JSONObject(MultiAiClient.normalizeJsonObject(raw));
    if(obj.optJSONArray("scenes")==null||obj.optJSONArray("scenes").length()==0)throw new Exception("JSON sem cenas escritas.");
    return obj;
  }

  private String requireProviderKey(String provider) throws Exception {
    String key=null;
    try { key=providerKeys.read(provider); } catch (Exception ignored) {}
    if((key==null||key.isEmpty())&&"deepseek".equals(provider))key=legacyDeepSeek.read();
    if(key==null||key.isEmpty())throw new Exception("API Key "+provider+" não configurada.");
    return key;
  }

  private class VaultBridge {
    @JavascriptInterface public boolean hasFolder(){ return getVaultUri()!=null; }
    @JavascriptInterface public String folderName(){ Uri u=getVaultUri(); return u==null?"":u.getLastPathSegment(); }
    @JavascriptInterface public void chooseFolder(){
      runOnUiThread(() -> {
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(i,VAULT_PICKER);
      });
    }
    @JavascriptInterface public void clearFolder(){ getSharedPreferences(VAULT_PREFS,Context.MODE_PRIVATE).edit().remove(VAULT_URI).apply(); }
    @JavascriptInterface public String saveFile(String relativePath,String text){
      try { saveVaultFile(relativePath,text==null?"":text); return "OK"; }
      catch(Exception e){ return "ERROR: "+safeError(e); }
    }
  }

  private Uri getVaultUri(){
    String raw=getSharedPreferences(VAULT_PREFS,Context.MODE_PRIVATE).getString(VAULT_URI,null);
    if(raw==null||raw.isEmpty())return null;
    try{return Uri.parse(raw);}catch(Exception e){return null;}
  }

  private void saveVaultFile(String relativePath,String text) throws Exception {
    Uri tree=getVaultUri();if(tree==null)throw new Exception("Cofre CHS não configurado.");
    String clean=relativePath==null?"":relativePath.replace('\\','/').replaceAll("/+","/");
    if(clean.startsWith("/")||clean.contains("..")||clean.trim().isEmpty())throw new Exception("Caminho inválido no Cofre.");
    String[] parts=clean.split("/");
    Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
    for(int i=0;i<parts.length-1;i++){
      String name=parts[i].trim();if(name.isEmpty())continue;
      Uri child=findChild(tree,parent,name);
      if(child==null)child=DocumentsContract.createDocument(getContentResolver(),parent,DocumentsContract.Document.MIME_TYPE_DIR,name);
      if(child==null)throw new Exception("Não foi possível criar pasta "+name);
      parent=child;
    }
    String fileName=parts[parts.length-1].trim();if(fileName.isEmpty())throw new Exception("Nome de arquivo vazio.");
    Uri file=findChild(tree,parent,fileName);
    if(file==null){String mime=fileName.endsWith(".json")?"application/json":"text/plain";file=DocumentsContract.createDocument(getContentResolver(),parent,mime,fileName);}
    if(file==null)throw new Exception("Não foi possível criar "+fileName);
    try(OutputStream out=getContentResolver().openOutputStream(file,"wt")){if(out==null)throw new Exception("Sem acesso de escrita ao Cofre.");out.write(text.getBytes(StandardCharsets.UTF_8));out.flush();}
  }

  private Uri findChild(Uri tree,Uri parent,String name) throws Exception {
    ContentResolver r=getContentResolver();
    Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getDocumentId(parent));
    String[] projection={DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME};
    try(Cursor c=r.query(children,projection,null,null,null)){
      if(c!=null)while(c.moveToNext())if(name.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(tree,c.getString(0));
    }
    return null;
  }

  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    if(requestCode==VAULT_PICKER){
      if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
        Uri uri=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try{getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}
        getSharedPreferences(VAULT_PREFS,Context.MODE_PRIVATE).edit().putString(VAULT_URI,uri.toString()).apply();
        showJs("try{onVaultFolderSelected("+JSONObject.quote(uri.getLastPathSegment())+")}catch(e){try{render()}catch(x){}};");
      }
      return;
    }
    super.onActivityResult(requestCode,resultCode,data);
  }

  private void callback(String fn,String requestId,boolean ok,String data){
    if(providerWeb==null)return;
    String js=fn+"("+JSONObject.quote(requestId)+","+(ok?"true":"false")+","+JSONObject.quote(data==null?"":data)+")";
    providerWeb.post(()->providerWeb.evaluateJavascript(js,null));
  }
  private void showJs(String js){if(providerWeb!=null)providerWeb.post(()->providerWeb.evaluateJavascript(js,null));}
  private String safeError(Exception e){String m=e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;}
  private WebView findWebView(View v){if(v instanceof WebView)return(WebView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView w=findWebView(g.getChildAt(i));if(w!=null)return w;}}return null;}
}
