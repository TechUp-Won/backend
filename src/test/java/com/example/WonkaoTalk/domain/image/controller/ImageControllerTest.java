package com.example.WonkaoTalk.domain.image.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.config.JacksonConfig;
import com.example.WonkaoTalk.common.config.security.jwt.JwtExceptionFilter;
import com.example.WonkaoTalk.common.config.security.jwt.JwtTokenProvider;
import com.example.WonkaoTalk.common.redis.RedisService;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlRequest;
import com.example.WonkaoTalk.domain.image.dto.PresignedUrlResponse;
import com.example.WonkaoTalk.domain.image.service.ImageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ImageController.class)
@Import(JacksonConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class ImageControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockitoBean
  private ImageService imageService;
  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;
  @MockitoBean
  private RedisService redisService;
  @MockitoBean
  private JwtExceptionFilter jwtExceptionFilter;

  @Test
  @DisplayName("유효한 요청으로 Presigned URL 발급 성공 시 200과 응답 본문을 반환한다")
  void getPresignedUrl_validRequest_returns200() throws Exception {
    PresignedUrlResponse response = new PresignedUrlResponse(
        "http://localhost:9000/wonkao-talk/temp/test.jpg?X-Amz-Signature=abc",
        "temp/test.jpg",
        "image/jpeg"
    );
    when(imageService.issuePresignedUrl(any(PresignedUrlRequest.class))).thenReturn(response);

    mockMvc.perform(post("/api/v1/images/presigned-url")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PresignedUrlRequest("photo.jpg"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data.objectKey").value("temp/test.jpg"))
        .andExpect(jsonPath("$.data.contentType").value("image/jpeg"));
  }

  @Test
  @DisplayName("filename이 공백인 요청은 400을 반환한다")
  void getPresignedUrl_blankFilename_returns400() throws Exception {
    mockMvc.perform(post("/api/v1/images/presigned-url")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new PresignedUrlRequest(""))))
        .andExpect(status().isBadRequest());
  }
}
