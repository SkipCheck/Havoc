package com.hexedrealms.utils.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.util.prefs.Preferences;

public class SecurityComponent {
    private static final String KEY_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int AES_KEY_SIZE = 256;

    private SecretKey encryptionKey;
    private SecretKey hmacKey;

    public SecurityComponent() {
        initKeys();
    }

    private void initKeys() {
        // Получаем доступ к хранилищу настроек пользователя (реестр/файлы настроек)
        Preferences prefs = Preferences.userRoot();
        // Генерация или загрузка ключа шифрования
        byte[] encKeyBytes = prefs.getByteArray("enc_key", null);
        if (encKeyBytes == null) {
            // Ключа нет - нужно сгенерировать новый
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(KEY_ALGORITHM);
                keyGen.init(AES_KEY_SIZE);
                this.encryptionKey = keyGen.generateKey();
                prefs.putByteArray("enc_key", encryptionKey.getEncoded());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Ошибка генерации ключа", e);
            }
        } else {
            // Ключ есть - восстанавливаем его из байтового массива
            this.encryptionKey = new SecretKeySpec(encKeyBytes, KEY_ALGORITHM);
        }

        // Генерация или загрузка HMAC ключа
        byte[] hmacKeyBytes = prefs.getByteArray("hmac_key", null);
        if (hmacKeyBytes == null) {
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance(HMAC_ALGORITHM);
                this.hmacKey = keyGen.generateKey();
                prefs.putByteArray("hmac_key", hmacKey.getEncoded());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Ошибка генерации HMAC ключа", e);
            }
        } else {
            this.hmacKey = new SecretKeySpec(hmacKeyBytes, HMAC_ALGORITHM);
        }
    }

    public SecretKey getEncryptionKey() {
        return encryptionKey;
    }
    public SecretKey getHmacKey() {
        return hmacKey;
    }
}
