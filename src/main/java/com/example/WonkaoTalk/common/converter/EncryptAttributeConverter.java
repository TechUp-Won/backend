package com.example.WonkaoTalk.common.converter;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Converter
public class EncryptAttributeConverter implements AttributeConverter<String, String> {

  private static String ALGORITHM = "AES";
  private static byte[] KEY;

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
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
      return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
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
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, ALGORITHM));
      return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.OAUTH_FAILURE_DECRYPT);
    }
  }
}
