package com.law.law_qa_system.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {
    private static final String OPENAI_API_KEY = "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";
    public String rerankChunks(String userInput, List<String> chunks) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String chunkText = String.join("\n\n", chunks); // Gộp các chunk lại thành một chuỗi
            List<Map<String, String>> messages = List.of(
                    Map.of(
                            "role", "user",
                            "content", "Bạn là một trợ lý pháp lý. Tôi có câu hỏi sau:" +  userInput +
                                    "Dưới đây là các đoạn văn bản có thể liên quan: " + chunkText +
                                    "Hãy sắp xếp mức độ liên quan với câu hỏi, sau đó loại bỏ những không liên quan sau đó Hãy tổng hợp câu trả lời dễ hiểu nhất cho người dùng (chỉ cung cấp nội dung trả lời)"
                    )
            );
            Map<String, Object> payload = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", messages
            );

            // Chuyển sang JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);

            return callOpenAI(jsonPayload);
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }
    public String generateAnswer(String userInput) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<Map<String, String>> messages = List.of(
                    Map.of(
                            "role", "user",
                            "content", "Trích xuất các từ khóa quan trọng về nội dung để tìm kiếm luật từ đoạn sau và trả về dưới dạng danh sách JSON (chỉ json):\\n\\n" + userInput
                    )
            );

            Map<String, Object> payload = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", messages
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            return callOpenAI(jsonPayload);
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }
    public String callOpenAI(String jsonPayload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(OPENAI_API_KEY);

        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.openai.com/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "Lỗi: API Key không hợp lệ hoặc hết hạn.";
            }

            return response.getBody();
        } catch (HttpClientErrorException e) {
            return "Lỗi API: " + e.getMessage();
        } catch (Exception e) {
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }
}
