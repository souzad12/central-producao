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

final class SecureApiKeyStore {
  private static final String PREFS = "chs_secure_v03";
  private static final String PREF_DEEPSEEK = "deepseek_key";
  private static final String ALIAS = "chs_deepseek_key_v1";
  private final Context context;

  SecureApiKeyStore(Context context) { this.context = context; }

  private SecretKey getOrCreateKey() throws Exception {
    KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
    ks.load(null);
    if (ks.containsAlias(ALIAS)) {
      return ((KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null)).getSecretKey();
    }
    KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
    kg.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .build());
    return kg.generateKey();
  }

  void save(String apiKey) throws Exception {
    if (apiKey == null || apiKey.trim().length() < 8) throw new Exception("Chave inválida.");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
    byte[] encrypted = cipher.doFinal(apiKey.trim().getBytes(StandardCharsets.UTF_8));
    String packed = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" +
      Base64.encodeToString(encrypted, Base64.NO_WRAP);
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PREF_DEEPSEEK, packed).apply();
  }

  String read() throws Exception {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String packed = prefs.getString(PREF_DEEPSEEK, null);
    if (packed == null || !packed.contains(":")) return null;
    String[] parts = packed.split(":", 2);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
    return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
  }

  boolean has() {
    try { String value = read(); return value != null && !value.isEmpty(); }
    catch (Exception e) { return false; }
  }

  void clear() { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(PREF_DEEPSEEK).apply(); }
}
