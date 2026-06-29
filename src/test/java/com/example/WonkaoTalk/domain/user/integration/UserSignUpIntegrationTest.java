package com.example.WonkaoTalk.domain.user.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.WonkaoTalk.common.integration.BaseIntegrationTest;
import com.example.WonkaoTalk.domain.auth.repo.AuthLocalRepo;
import com.example.WonkaoTalk.domain.user.dto.UserSignUpRequest;
import com.example.WonkaoTalk.domain.user.enums.Gender;
import com.example.WonkaoTalk.domain.user.repo.UserRepo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserSignUpIntegrationTest extends BaseIntegrationTest {

  @Autowired
  private AuthLocalRepo authLocalRepo;
  @Autowired
  private UserRepo userRepo;

  @Test
  @DisplayName("회원가입은 벌크헤드 풀에서 비동기로 처리되며, BCrypt 해싱과 User/AuthLocal 저장이 한 트랜잭션으로 커밋된다")
  void signUp_Success_PersistsAuthLocalAndUserInOneTransaction() throws Exception {
    // given
    UserSignUpRequest request = new UserSignUpRequest(
        "signup-async@test.com", "Qwer1234", "Qwer1234",
        "테스터", "테스터닉네임", "010-1234-5678", null, Gender.NONE);

    // when
    MvcResult mvcResult = mockMvc.perform(post("/api/v1/users/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(request().asyncStarted())
        .andReturn();

    // then
    mockMvc.perform(asyncDispatch(mvcResult))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("SUCCESS"));

    assertThat(authLocalRepo.findByEmail("signup-async@test.com")).isPresent();
    assertThat(userRepo.findAll())
        .anyMatch(user -> "테스터닉네임".equals(user.getNickname()));
  }
}
