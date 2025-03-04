package com.law.law_qa_system.services;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.law.law_qa_system.models.DocumentDetailEmbeddingES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ElasticsearchService {
    @Autowired
    private ElasticsearchClient client;
    @Autowired
    private OllamaEmbeddingService ollamaEmbeddingService;
    @Autowired
    private OpenAIService openAIService;
    @Autowired
    private ChatDetailService chatDetailService;
    @Autowired
    private ObjectMapper objectMapper;
    public List<Map<String, Object>> searchDocuments(String query) throws IOException {
        String answer = chatDetailService.generateAnswer(query);
        System.out.println(answer);
        JsonNode rootNode = objectMapper.readTree(answer);
        String content = rootNode.path("choices").get(0).path("message").path("content").asText();
        content = content.replaceAll("(?s)```json|```", "").trim();

        List<String> keywords =  Arrays.asList(objectMapper.readValue(content, String[].class));

        System.out.println("Từ khóa trích xuất: " + keywords);

        List<Double> embeddingVector = ollamaEmbeddingService.getEmbedding(query);
        List<Float> queryEmbeddingFloat = embeddingVector.stream()
                .map(Double::floatValue)
                .toList();

        SearchRequest request = new SearchRequest.Builder()
                .index("document_embedding")
                .knn(knnQuery -> knnQuery
                        .field("embedding")
                        .queryVector(queryEmbeddingFloat)
                        .numCandidates(100)
                        .k(20)
                )
                .query(q -> q
                        .bool(b -> b
                                .should(m -> m
                                        .multiMatch(mm -> mm
                                                .fields(Arrays.asList("chunkText", "title"))
                                                .query(String.join(" ", keywords))
                                        )
                                )
                        )
                )
                .highlight(h -> h
                        .fields("chunkText", new HighlightField.Builder().build())
                        .preTags("<b style='background-color: yellow;'>")
                        .postTags("</b>")
                )
                .build();

        SearchResponse<DocumentDetailEmbeddingES> response = client.search(request, DocumentDetailEmbeddingES.class);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<DocumentDetailEmbeddingES> hit : response.hits().hits()) {
            Map result = objectMapper.convertValue(hit.source(), Map.class);
            result.put("highlight", hit.highlight().get("chunkText"));
            results.add(result);
        }
        Map<String, Map<String, Object>> groupedResults = new HashMap<>();
        for (Hit<DocumentDetailEmbeddingES> hit : response.hits().hits()) {
            Map result = objectMapper.convertValue(hit.source(), Map.class);
            Object documentIdObj  =  result.get("documentId");
            String documentId;
            if (documentIdObj instanceof String) {
                documentId = (String) documentIdObj;
            } else if (documentIdObj instanceof Number) {
                documentId = documentIdObj.toString();
            } else {
                documentId = "UNKNOWN"; // Giá trị mặc định nếu không xác định được
            }
            // Lấy danh sách highlight
            List<String> highlights = hit.highlight() != null ? hit.highlight().get("chunkText") : Collections.emptyList();

            // Nếu documentId đã có trong groupedResults, gộp highlight lại
            if (groupedResults.containsKey(documentId)) {
                Map<String, Object> existingResult = groupedResults.get(documentId);

                // Gộp highlight từ nhiều chunk khác nhau
                List<String> existingHighlights = (List<String>) existingResult.get("highlight");
                if (existingHighlights == null) {
                    existingHighlights = new ArrayList<>();  // Nếu chưa có highlight, khởi tạo danh sách rỗng
                }
                if (highlights != null) {
                    existingHighlights.addAll(highlights);  // Thêm highlight nếu highlights không phải null
                }
                existingResult.put("highlight", existingHighlights);  // Cập nhật lại giá trị "highlight"
            } else {
                // Nếu chưa có documentId này, thêm vào map
                List<String> safeHighlights = (highlights != null) ? new ArrayList<>(highlights) : new ArrayList<>();  // Đảm bảo highlights không phải null
                result.put("highlight", safeHighlights);
                groupedResults.put(documentId, result);
            }
        }
        return new ArrayList<>(groupedResults.values());
    }


}
