package com.law.law_qa_system.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.law.law_qa_system.models.DocumentDetailEmbeddingES;
import com.law.law_qa_system.models.LegalDocumentDetail;
import com.law.law_qa_system.repositories.DocumentDetailEmbeddingESRepository;
import com.law.law_qa_system.repositories.DocumentEmbeddingRepository;
import com.law.law_qa_system.repositories.LegalDocumentDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DocumentEmbeddingService {
    @Autowired
    private DocumentEmbeddingRepository documentEmbeddingRepository;
    @Autowired
    private LegalDocumentDetailRepository legalDocumentDetailRepository;
    @Autowired
    DocumentDetailEmbeddingESRepository elasticsearchRepository;
    @Autowired
    private OllamaEmbeddingService embeddingService;
    @Autowired
    private ObjectMapper objectMapper;
    @Transactional
    public void processAndSaveEmbeddings(Long documentId) {
        LegalDocumentDetail document = legalDocumentDetailRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        // Lấy nội dung gốc
        String content = document.getContent().trim();
        // Kiểm tra nội dung có hợp lệ không
        boolean hasValidContent = !(content.isEmpty() || "Không tìm thấy nội dung".equalsIgnoreCase(content));

        List<String> chunks = hasValidContent ? TextChunker.splitText(content) : Collections.singletonList("");

        //List<DocumentDetailEmbedding> embeddings = new ArrayList<>();
        List<DocumentDetailEmbeddingES> esEmbeddings = new ArrayList<>();

        IntStream.range(0, chunks.size()).forEach(index -> {
            List<Double> embeddingVector = hasValidContent ? embeddingService.getEmbedding(chunks.get(index)) : null;
            List<Double> embeddingTitleVector = embeddingService.getEmbedding(document.getTitle());
            List<String> fieldList = Arrays.stream(document.getFields().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            DocumentDetailEmbeddingES esEntity = new DocumentDetailEmbeddingES(documentId, index, hasValidContent ? chunks.get(index) : "Nội dung trống", embeddingVector, embeddingTitleVector, document.getIssuingAgency(), document.getOfficialGazetteNumber(), document.getPublicationDate(), document.getDocumentType(), document.getSigner(), document.getTitle(), document.getIssuedDate(), document.getDocumentNumber(), document.getEffectiveDate(), fieldList);
            esEmbeddings.add(esEntity);
        });

        elasticsearchRepository.saveAll(esEmbeddings);
    }
    /*(public void saveEmbedding(Document document, List<Double> embedding) {
        System.out.println("Embedding trước khi lưu: " + embedding);

        Optional<DocumentEmbedding> existEmbedding = documentEmbeddingRepository.findByDocumentId(document.getId());

        if (existEmbedding.isPresent()) {
            System.out.println("Update exist embedding");
            DocumentEmbedding documentEmbedding = existEmbedding.get();
            documentEmbedding.setEmbedding(embedding);
            documentEmbeddingRepository.save(documentEmbedding);
        } else {
            System.out.println("Create new embedding");
            DocumentEmbedding newEmbedding = new DocumentEmbedding();
            newEmbedding.setDocument(document);
            newEmbedding.setEmbedding(embedding);
            documentEmbeddingRepository.save(newEmbedding);
        }
        System.out.println("Embedding save successfully!");
    }


    public List<Double> getEmbeddingByDocumentId(Long documentId) {
        Optional<DocumentEmbedding> embedding = documentEmbeddingRepository.findByDocumentId(documentId);

        if (embedding.isPresent()) {
            System.out.println("Embedding found for documentId: " + documentId);
            return embedding.get().getEmbedding();
        } else {
            System.out.println("No embedding found for documentId: " + documentId);
            return new ArrayList<>();
        }
    }*/
    public static class TextChunker {
        public static List<String> splitText(String text) {
            List<String> chunks = new ArrayList<>();

            String[] words = text.split("\\s+"); // Tách theo dấu cách
            StringBuilder currentChunk = new StringBuilder();
            int count = 0;

            for (String word : words) {
                currentChunk.append(word).append(" ");
                count++;
                if (count >= 300) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                    count = 0;
                }
            }
            if (!currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString().trim());
            }
            return chunks;
        }



        public static List<String> splitLawText(String text) {
            List<String> resultChunks = new ArrayList<>();

            // Regex nhận diện các mục quan trọng
            String chapterRegex = "(Chương \\w+\\.)";
            String articleRegex = "(Điều \\d+\\.)";
            String clauseRegex = "(\\(\\d+\\))";  // Khoản
            String pointRegex = "(\\([a-z]\\))";  // Điểm
            String sectionRegex = "(\\d+\\.)";   // Mục lớn "1.", "2.", "3."
            String subsectionRegex = "(\\d+\\.\\d+)"; // Tiểu mục "5.1.", "5.2."

            // Tìm vị trí các mục trong văn bản
            List<Integer> chapterPositions = findPositions(text, chapterRegex);
            List<Integer> sectionPositions = findPositions(text, sectionRegex);
            List<Integer> articlePositions = findPositions(text, articleRegex);
            List<Integer> subsectionPositions = findPositions(text, subsectionRegex);

            // Gom tất cả vị trí lại để chia đoạn
            List<Integer> allPositions = new ArrayList<>();
            allPositions.addAll(chapterPositions);
            allPositions.addAll(sectionPositions);
            allPositions.addAll(articlePositions);
            allPositions.addAll(subsectionPositions);
            Collections.sort(allPositions);

            // Nếu không có mục lớn, lấy toàn bộ văn bản
            if (allPositions.isEmpty()) {
                return splitBySentence(text, 300);
            }

            // Tách văn bản theo các mục tìm được
            for (int i = 0; i < allPositions.size(); i++) {
                int start = allPositions.get(i);
                int end = (i + 1 < allPositions.size()) ? allPositions.get(i + 1) : text.length();
                String chunk = text.substring(start, end).trim();

                if (chunk.length() > 500) {
                    resultChunks.addAll(splitBySentence(chunk, 300));
                } else {
                    resultChunks.add(chunk);
                }
            }

            return resultChunks;
        }

        public static List<Integer> findPositions(String text, String regex) {
            List<Integer> positions = new ArrayList<>();
            Matcher matcher = Pattern.compile(regex).matcher(text);
            while (matcher.find()) {
                positions.add(matcher.start());
            }
            return positions;
        }

        public static List<String> splitBySentence(String text, int maxLen) {
            List<String> result = new ArrayList<>();
            String[] sentences = text.split("(?<=[.!?])\\s+");
            StringBuilder chunk = new StringBuilder();

            for (String sentence : sentences) {
                if (chunk.length() + sentence.length() > maxLen) {
                    result.add(chunk.toString().trim());
                    chunk.setLength(0);
                }
                chunk.append(sentence).append(" ");
            }

            if (!chunk.isEmpty()) {
                result.add(chunk.toString().trim());
            }
            return result;
        }
    }


}
