package com.example.WonkaoTalk.common.config.security.jwt;

import com.example.WonkaoTalk.common.exception.BusinessException;
import com.example.WonkaoTalk.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey key;
  @Getter
  private final long accessTokenValidTime;
  @Getter
  private final long refreshTokenValidTime;

  public JwtTokenProvider(
      @Value("${jwt.secret}")
      String secretKey,
      @Value("${jwt.access-expiration-time}")
      long accessTokenValidTime,
      @Value("${jwt.refresh-expiration-time}")
      long refreshTokenValidTime
  ) {

    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.accessTokenValidTime = accessTokenValidTime;
    this.refreshTokenValidTime = refreshTokenValidTime;

  }

  public String createAccessToken(String email, Long authId, String role) {
    long now = (new Date()).getTime();
    Date validity = new Date(now + this.accessTokenValidTime);

    return Jwts.builder()
        .setSubject(email)
        .claim("authId", authId)
        .claim("role", role)
        .signWith(key, SignatureAlgorithm.HS256)
        .setExpiration(validity)
        .compact();
  }

  public String createRefreshToken(String email) {
    long now = (new Date()).getTime();
    Date validity = new Date(now + this.refreshTokenValidTime);

    return Jwts.builder()
        .setSubject(email)
        .signWith(key, SignatureAlgorithm.HS256)
        .setExpiration(validity)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.warn("만료된 JWT 토큰입니다.");
      throw new BusinessException(ErrorCode.AUTH_EXPIRED_TOKEN);
    } catch (SecurityException | MalformedJwtException |
             UnsupportedJwtException | IllegalArgumentException e) {
      log.warn("잘못된 JWT 토큰입니다.");
      throw new BusinessException(ErrorCode.AUTH_INVALID_TOKEN);
    }
  }

  public String getEmailFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public String getRoleFromToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .get("role", String.class);
  }

}
