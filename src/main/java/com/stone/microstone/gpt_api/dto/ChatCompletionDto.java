package com.stone.microstone.gpt_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatCompletionDto {
    private String model;
    private List<ChatRequestMsgDto> messages;
}