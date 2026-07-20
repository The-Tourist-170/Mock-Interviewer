package com.tourist.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourist.server.dto.ExtractedInfoDTO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeService {

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public ResumeService(AiService aiService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public ExtractedInfoDTO extractInfo(MultipartFile resumeFile) throws IOException {
        String resumeText;
        try (PDDocument document = Loader.loadPDF(resumeFile.getBytes())) {
            resumeText = new PDFTextStripper().getText(document);
        }

        String rawJson = aiService.extractInfoFromResume(resumeText);
        String cleanedJson = rawJson.replace("```json", "").replace("```", "").trim();

        return objectMapper.readValue(cleanedJson, ExtractedInfoDTO.class);
    }
}