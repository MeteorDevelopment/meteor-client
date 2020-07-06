package minegame159.meteorclient.accountsfriends;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class AESEncryption {
    public static String encrypt(String string, String key) {
        try {
            byte[] a = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            a = Arrays.copyOf(a, 16);

            SecretKeySpec keySpec = new SecretKeySpec(a, "AES");

            Cipher des = Cipher.getInstance("AES");
            des.init(Cipher.ENCRYPT_MODE, keySpec);

            return new String(Base64.getEncoder().encode(des.doFinal(string.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String decrypt(String string, String key) throws Exception {
        byte[] a = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        a = Arrays.copyOf(a, 16);

        SecretKeySpec keySpec = new SecretKeySpec(a, "AES");

        Cipher des = Cipher.getInstance("AES");
        des.init(Cipher.DECRYPT_MODE, keySpec);

        return new String(des.doFinal(Base64.getDecoder().decode(string.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);
    }
}
