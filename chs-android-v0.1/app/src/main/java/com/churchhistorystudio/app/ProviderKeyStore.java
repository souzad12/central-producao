package com.churchhistorystudio.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class ProviderKeyStore {
  private static final String PREFS="chs_provider_keys_v1";
  private static final String ALIAS="chs_provider_keys_aes_v1";
  private final Context context;
  ProviderKeyStore(Context context){this.context=context;}

  private String keyName(String provider){return "key_"+safeProvider(provider);}
  private String safeProvider(String p){
    if("openai".equalsIgnoreCase(p))return "openai";
    if("gemini".equalsIgnoreCase(p))return "gemini";
    return "deepseek";
  }
  private SecretKey aes() throws Exception {
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    if(ks.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
    KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
    return kg.generateKey();
  }
  void save(String provider,String apiKey) throws Exception {
    if(apiKey==null||apiKey.trim().length()<8)throw new Exception("Chave inválida.");
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,aes());
    byte[] encrypted=c.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
    String packed=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(encrypted,Base64.NO_WRAP);
    context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(keyName(provider),packed).apply();
  }
  String read(String provider) throws Exception {
    SharedPreferences p=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    String packed=p.getString(keyName(provider),null);if(packed==null||!packed.contains(":"))return null;
    String[] parts=packed.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE,aes(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
    return new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);
  }
  boolean has(String provider){try{String s=read(provider);return s!=null&&!s.isEmpty();}catch(Exception e){return false;}}
  void clear(String provider){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(keyName(provider)).apply();}
}
