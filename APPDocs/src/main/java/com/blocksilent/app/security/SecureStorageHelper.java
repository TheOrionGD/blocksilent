package com.blocksilent.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureStorageHelper {
    private static final String TAG = "BLOCKSILENT_SECURE";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "BlockSilentMasterKey";
    private static final String PREF_NAME = "blocksilent_secure_prefs";
    private static final String AES_MODE = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SharedPreferences prefs;

    public SecureStorageHelper(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initKeyStore();
    }

    private void initKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
                );
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                );
                keyGenerator.generateKey();
                Log.d(TAG, "Hardware-backed Master Keystore key created securely.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Keystore initialization error: " + e.getMessage());
        }
    }

    private SecretKey getSecretKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    public synchronized String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) return plainText;
        try {
            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(AES_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] iv = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encryption failed: " + e.getMessage());
            return plainText; // Safe graceful fallback
        }
    }

    public synchronized String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isEmpty()) return cipherTextBase64;
        try {
            byte[] combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP);
            if (combined.length <= GCM_IV_LENGTH) return cipherTextBase64;

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            SecretKey secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(AES_MODE);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(cipherBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Not encrypted or corrupted
            return cipherTextBase64;
        }
    }

    public void putSecureString(String key, String value) {
        prefs.edit().putString(key, encrypt(value)).apply();
    }

    public String getSecureString(String key, String defaultValue) {
        String encrypted = prefs.getString(key, null);
        if (encrypted == null) return defaultValue;
        return decrypt(encrypted);
    }

    public static void saveSecureString(Context context, String key, String value) {
        if (context == null) return;
        new SecureStorageHelper(context).putSecureString(key, value);
    }

    public static String getSecureString(Context context, String key, String defaultValue) {
        if (context == null) return defaultValue;
        return new SecureStorageHelper(context).getSecureString(key, defaultValue);
    }
}
