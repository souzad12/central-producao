package com.churchhistorystudio.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WriterActivityV048 extends WriterActivityV047 {
  private static final int CSV_NATIVE_PICKER = 4901;
  private WebView activeWeb;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().getDecorView().post(() -> {
      activeWeb=findWebView(getWindow().getDecorView());
      if(activeWeb!=null)activeWeb.addJavascriptInterface(new CsvBridge(),"CSVBridge");
    });
    getWindow().getDecorView().postDelayed(this::loadTrashLayer,9800);
    getWindow().getDecorView().postDelayed(this::loadCsvLayer,11000);
  }

  private class CsvBridge {
    @JavascriptInterface public void pickCsv(){
      runOnUiThread(()->{
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"text/csv","text/plain","application/csv","application/vnd.ms-excel"});
        startActivityForResult(i,CSV_NATIVE_PICKER);
      });
    }
  }

  private void loadTrashLayer(){loadAsset("editorial_trash_v048.js");}
  private void loadCsvLayer(){loadAsset("native_csv_v049.js");}

  private void loadAsset(String name){
    try{
      WebView web=activeWeb!=null?activeWeb:findWebView(getWindow().getDecorView());
      if(web==null)return;
      try(InputStream in=getAssets().open(name);ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
        web.evaluateJavascript(new String(out.toByteArray(),StandardCharsets.UTF_8),null);
      }
    }catch(Exception e){showJs("Falha ao carregar "+name+": "+safeError(e));}
  }

  private String readCsv(Uri uri)throws Exception{
    try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      if(in==null)throw new Exception("O Android não forneceu acesso ao arquivo.");
      byte[] buf=new byte[8192];int n,total=0;
      while((n=in.read(buf))!=-1){total+=n;if(total>10*1024*1024)throw new Exception("CSV maior que 10 MB.");out.write(buf,0,n);}
      if(total==0)throw new Exception("CSV vazio.");
      return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
  }

  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    if(requestCode==CSV_NATIVE_PICKER){
      if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
        try{
          String text=readCsv(data.getData());
          WebView web=activeWeb!=null?activeWeb:findWebView(getWindow().getDecorView());
          if(web==null)throw new Exception("WebView não disponível.");
          String js="try{if(typeof importCSV!=='function')throw new Error('Importador CSV não carregado');importCSV("+JSONObject.quote(text)+")}catch(e){flash('Falha ao importar: '+(e&&e.message?e.message:e))}";
          web.post(()->web.evaluateJavascript(js,null));
        }catch(Exception e){showJs("Falha ao ler CSV: "+safeError(e));}
      }
      return;
    }
    super.onActivityResult(requestCode,resultCode,data);
  }

  private void showJs(String message){
    WebView web=activeWeb!=null?activeWeb:findWebView(getWindow().getDecorView());if(web==null)return;
    String js="try{flash("+JSONObject.quote(message)+")}catch(e){}";web.post(()->web.evaluateJavascript(js,null));
  }
  private String safeError(Exception e){String m=e.getMessage();return m==null||m.trim().isEmpty()?e.getClass().getSimpleName():m;}
  private WebView findWebView(View v){if(v instanceof WebView)return(WebView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){WebView w=findWebView(g.getChildAt(i));if(w!=null)return w;}}return null;}
}
