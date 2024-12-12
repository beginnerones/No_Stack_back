package com.stone.microstone.gpt_api.service;

import java.util.Map;

public interface ChatGPTService {
    Map<String, Object> summarizeText(String text);
    Map<String, Object> generateQuestion(String summarizedText);
    Map<String,Object> regenerateQuestion(String summarizedText,String contextText);
    Map<String, Object> generateAnswer(String questionText);
}