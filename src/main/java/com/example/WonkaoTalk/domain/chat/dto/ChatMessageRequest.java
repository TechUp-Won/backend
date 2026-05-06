package com.example.WonkaoTalk.domain.chat.dto;

import com.example.WonkaoTalk.domain.chat.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ChatMessageRequest(
    @NotBlank(message = "메시지 내용을 입력해주세요.")
    String content,

    @NotNull(message = "메시지 타입은 필수입니다.")
    MessageType messageType,

    Long answerMessageId // 답장이 아니면 null
) {

}
