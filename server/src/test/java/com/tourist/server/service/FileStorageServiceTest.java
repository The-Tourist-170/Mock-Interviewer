package com.tourist.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private MultipartFile mockFile;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void storeFile_normalFile_writesAndReturnsPath() {
        MultipartFile file = new MockMultipartFile("resume", "my-resume.pdf",
                "application/pdf", "test content".getBytes());

        String storedPath = fileStorageService.storeFile(file);

        assertThat(storedPath).startsWith(tempDir.toAbsolutePath().toString());
        Path storedFile = Path.of(storedPath);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storedFile.getFileName().toString()).endsWith("my-resume.pdf");
    }

    @Test
    void storeFile_pathTraversal_throws() {
        MultipartFile file = new MockMultipartFile("resume", "../../../etc/passwd",
                "application/pdf", "malicious".getBytes());

        assertThatThrownBy(() -> fileStorageService.storeFile(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid path");
    }

    @Test
    void storeFile_nullOriginalFilename_defaultsToFile() throws Exception {
        when(mockFile.getOriginalFilename()).thenReturn(null);
        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        String storedPath = fileStorageService.storeFile(mockFile);

        Path storedFile = Path.of(storedPath);
        assertThat(Files.exists(storedFile)).isTrue();
        assertThat(storedFile.getFileName().toString()).endsWith("file");
    }
}
