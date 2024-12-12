package com.stone.microstone.gpt_api.service.impl;

import com.stone.microstone.gpt_api.config.ChatGPTConfig;
import com.stone.microstone.gpt_api.dto.ChatCompletionDto;
import com.stone.microstone.gpt_api.dto.ChatRequestMsgDto;
import com.stone.microstone.gpt_api.service.ChatGPTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ChatGPTServiceImpl implements ChatGPTService {

    private final ChatGPTConfig chatGPTConfig;

    public ChatGPTServiceImpl(ChatGPTConfig chatGPTConfig) {
        this.chatGPTConfig = chatGPTConfig;
    }

    @Override
    public Map<String, Object> summarizeText(String text) {
        log.debug("[+] 문제 텍스트를 요약합니다.");

        ChatCompletionDto chatCompletionDto = ChatCompletionDto.builder()
                .model("gpt-4o-mini")
                .messages(List.of(ChatRequestMsgDto.builder()
                        .role("user")
                        .content("다음 텍스트를 한국어로 단락별 핵심으로 요약해줘: " + text)
                        .build()))
                .build();
        log.debug("요약된 정보={}", chatCompletionDto.toString());

        Map<String, Object> response = executePrompt(chatCompletionDto);
        log.debug("요약 응답: {}", response.get("content"));

        return response;
    }

    @Override
    public Map<String, Object> generateQuestion(String summarizedText) {
        if (summarizedText == null || summarizedText.trim().isEmpty()) {
            log.error("요약된 텍스트가 없습니다.");
            throw new IllegalArgumentException("요약된 텍스트가 없습니다.");
        }

        log.debug("[+] 요약된 텍스트를 기반으로 문제를 생성합니다.");

        ChatCompletionDto chatCompletionDto = ChatCompletionDto.builder()
                .model("gpt-4o-mini")
                .messages(List.of(ChatRequestMsgDto.builder()
                        .role("user")
                        .content("다음 요약된 텍스트를 기반으로 서론없이 객관식을 15문제 생성해줘. 수능문제처럼 말투를 사용하되, 답은 나오지 않게 생성해줘. : " + summarizedText)
                        .build()))
                .build();
        log.debug("문제 생성 정보={}", chatCompletionDto.toString());

        return executePrompt(chatCompletionDto);
    }

    @Override
    public Map<String, Object> generateAnswer(String questionText) {
        if (questionText == null || questionText.trim().isEmpty()) {
            log.error("질문 텍스트가 없습니다.");
            throw new IllegalArgumentException("질문 텍스트가 없습니다.");
        }

        log.debug("[+] 질문 텍스트를 기반으로 답변을 생성합니다.");

        ChatCompletionDto chatCompletionDto = ChatCompletionDto.builder()
                .model("gpt-4o-mini")
                .messages(List.of(ChatRequestMsgDto.builder()
                        .role("user")
                        .content("다음 생성된 문제들의 서론없이 정확한 답과 4줄이 넘지 않는 자세한 해설을 생성해줘. '*'이 필요하면 사용하되, '*'을 사용해서 강조하지 말아줘.: " + questionText)
                        .build()))
                .build();
        log.debug("답변 생성 정보={}", chatCompletionDto.toString());

        return executePrompt(chatCompletionDto);
    }

    @Override
    public Map<String, Object> regenerateQuestion(String summarizedText,String contextText) {
        ChatCompletionDto chatCompletionDto = ChatCompletionDto.builder()
                .model("gpt-4o-mini")
                .messages(List.of(ChatRequestMsgDto.builder()
                        .role("user")
                        .content("다음 주어진 텍스트를 기반으로, 이전 문제와 겹치지 않는 새로운 객관식 문제를 서론 없이 15문제 생성해줘. 수능문제처럼 말투를 사용하되, 답은 나오지 않게 생성해줘. " +
                                "[이전문제] "+contextText + "[요약텍스트]" +summarizedText)
                        .build()))
                .build();
        log.debug("재생성 문제 정보={}", chatCompletionDto.toString());
        return executePrompt(chatCompletionDto);
    }

    private Map<String, Object> executePrompt(ChatCompletionDto chatCompletionDto) {
        Map<String, Object> resultMap = new HashMap<>();
        HttpHeaders headers = chatGPTConfig.httpHeaders();
        HttpEntity<ChatCompletionDto> requestEntity = new HttpEntity<>(chatCompletionDto, headers);

        String promptUrl = chatGPTConfig.getApiUrl(); // 설정된 API URL 가져오기

        ResponseEntity<String> response = chatGPTConfig
                .restTemplate()
                .exchange(promptUrl, HttpMethod.POST, requestEntity, String.class);
        try {
            ObjectMapper om = new ObjectMapper();
            Map<String, Object> responseMap = om.readValue(response.getBody(), new TypeReference<>() {});
            log.debug("API 응답: {}", responseMap); // 응답 확인을 위한 로그

            // 응답에서 필요한 부분을 추출
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    resultMap.put("content", message.get("content"));
                }
            }
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException :: " + e.getMessage());
        } catch (RuntimeException e) {
            log.debug("RuntimeException :: " + e.getMessage());
        }
        return resultMap;
    }


}
