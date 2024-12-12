package com.stone.microstone.gpt_api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswerResponse {
    private int wb_id;
    private String wb_title;
    private String question;
    private String answer;
}
