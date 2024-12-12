package com.stone.microstone.gpt_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatRequestMsgDto {
    private String role;
    private String content;
}