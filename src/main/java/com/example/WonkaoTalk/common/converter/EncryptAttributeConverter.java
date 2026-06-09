package com.example.WonkaoTalk.common.converter;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptAttributeConverter implements AttributeConverter<String, String> {

  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;
  private static String ALGORITHM = "AES/GCM/NoPadding";
  private byte[] KEY;

  @Value("${oauth.encryption.key}")
  public void setKey(String key) {
    KEY = key.getBytes(java.nio.charset.StandardCharsets.UTF_8);
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "AES"), parameterSpec);
      byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
      byte[] encryptedBuffer = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, encryptedBuffer, 0, iv.length);
      System.arraycopy(cipherText, 0, encryptedBuffer, iv.length, cipherText.length);
      return Base64.getEncoder().encodeToString(encryptedBuffer);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.OAUTH_FAILURE_ENCRYPT);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      byte[] encryptedBuffer = Base64.getDecoder().decode(dbData);
      byte[] iv = new byte[GCM_IV_LENGTH];
      System.arraycopy(encryptedBuffer, 0, iv, 0, iv.length);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), parameterSpec);
      byte[] plaintext = cipher.doFinal(encryptedBuffer, iv.length,
          encryptedBuffer.length - iv.length);
      return new String(plaintext, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.OAUTH_FAILURE_DECRYPT);
    }
  }
}
