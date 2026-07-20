package com.tourist.server.controller;

import com.tourist.server.dto.ExtractedInfoDTO;
import com.tourist.server.service.ResumeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/extract-info")
    public ResponseEntity<ExtractedInfoDTO> extractInfoFromResume(@RequestParam("resume") MultipartFile resumeFile) throws IOException {
        ExtractedInfoDTO extractedInfo = resumeService.extractInfo(resumeFile);
        return ResponseEntity.ok(extractedInfo);
    }
}