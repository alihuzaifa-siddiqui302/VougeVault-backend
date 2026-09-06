package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.shared.config.FileUploadConfig;
import com.ecommerce.VougeVault.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileUploadConfig fileUploadConfig;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private FileStorageService fileStorageService;

    @TempDir
    Path tempUploadDir;

    @BeforeEach
    void setUp() {
        lenient().when(fileUploadConfig.getUploadPath()).thenReturn(tempUploadDir);
    }

    @Nested
    @DisplayName("storeFile() Tests")
    class StoreFileTests {

        @Test
        @DisplayName("Should throw BusinessException when file is null")
        void storeFile_ShouldThrowException_WhenFileIsNull() {
            assertThatThrownBy(() -> fileStorageService.storeFile(null, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("File is empty");
        }

        @Test
        @DisplayName("Should throw BusinessException when file is empty")
        void storeFile_ShouldThrowException_WhenFileIsEmpty() {
            when(multipartFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("File is empty");
        }

        @Test
        @DisplayName("Should throw BusinessException when MIME type is not allowed")
        void storeFile_ShouldThrowException_WhenMimeTypeNotAllowed() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("application/pdf");
            when(fileUploadConfig.isAllowedMimeType("application/pdf")).thenReturn(false);
            when(fileUploadConfig.getAllowedMimeTypes()).thenReturn("image/jpeg, image/png"); // <-- Updated here

            assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("File type not allowed");
        }

        @Test
        @DisplayName("Should throw BusinessException when file size exceeds limit")
        void storeFile_ShouldThrowException_WhenFileSizeExceedsLimit() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getSize()).thenReturn(10 * 1024 * 1024L); // 10 MB
            when(fileUploadConfig.isAllowedMimeType("image/png")).thenReturn(true);
            when(fileUploadConfig.isValidFileSize(anyLong())).thenReturn(false);
            when(fileUploadConfig.getMaxFileSize()).thenReturn(5 * 1024 * 1024L); // 5 MB

            assertThatThrownBy(() -> fileStorageService.storeFile(multipartFile, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("File size exceeds maximum allowed size");
        }

        @Test
        @DisplayName("Should store file on disk and return generated unique filename")
        void storeFile_ShouldStoreFileAndReturnFileName_WhenValid() throws IOException {
            byte[] fileContent = "dummy-image-content".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(multipartFile.getSize()).thenReturn((long) fileContent.length);
            when(multipartFile.getOriginalFilename()).thenReturn("sample-image.png");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

            when(fileUploadConfig.isAllowedMimeType("image/png")).thenReturn(true);
            when(fileUploadConfig.isValidFileSize(anyLong())).thenReturn(true);

            String storedFileName = fileStorageService.storeFile(multipartFile, 100L);

            assertThat(storedFileName).endsWith(".png");
            Path storedFilePath = tempUploadDir.resolve(storedFileName);
            assertThat(Files.exists(storedFilePath)).isTrue();
            assertThat(Files.readAllBytes(storedFilePath)).isEqualTo(fileContent);
        }

        @Test
        @DisplayName("Should fallback to .jpg extension when original filename is null")
        void storeFile_ShouldFallbackToJpg_WhenOriginalFilenameIsNull() throws IOException {
            byte[] fileContent = "dummy-image-content".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getContentType()).thenReturn("image/jpeg");
            when(multipartFile.getSize()).thenReturn((long) fileContent.length);
            when(multipartFile.getOriginalFilename()).thenReturn(null);
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(fileContent));

            when(fileUploadConfig.isAllowedMimeType("image/jpeg")).thenReturn(true);
            when(fileUploadConfig.isValidFileSize(anyLong())).thenReturn(true);

            String storedFileName = fileStorageService.storeFile(multipartFile, 100L);

            assertThat(storedFileName).endsWith(".jpg");
            assertThat(Files.exists(tempUploadDir.resolve(storedFileName))).isTrue();
        }
    }

    @Nested
    @DisplayName("retrieveFile() Tests")
    class RetrieveFileTests {

        @Test
        @DisplayName("Should throw BusinessException when path traversal is detected")
        void retrieveFile_ShouldThrowException_WhenPathTraversalDetected() {
            String maliciousFileName = "../../../etc/passwd";

            assertThatThrownBy(() -> fileStorageService.retrieveFile(maliciousFileName))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Invalid file path");
        }

        @Test
        @DisplayName("Should throw BusinessException when file does not exist")
        void retrieveFile_ShouldThrowException_WhenFileNotFound() {
            String nonExistentFileName = "missing-file.png";

            assertThatThrownBy(() -> fileStorageService.retrieveFile(nonExistentFileName))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("File not found: " + nonExistentFileName);
        }

        @Test
        @DisplayName("Should retrieve file content bytes when file exists")
        void retrieveFile_ShouldReturnBytes_WhenFileExists() throws IOException {
            String fileName = "test-image.png";
            byte[] expectedBytes = "real-file-bytes".getBytes();
            Files.write(tempUploadDir.resolve(fileName), expectedBytes);

            byte[] actualBytes = fileStorageService.retrieveFile(fileName);

            assertThat(actualBytes).isEqualTo(expectedBytes);
        }
    }

    @Nested
    @DisplayName("deleteFile() Tests")
    class DeleteFileTests {

        @Test
        @DisplayName("Should throw BusinessException when path traversal is attempted during deletion")
        void deleteFile_ShouldThrowException_WhenPathTraversalAttempted() {
            String maliciousFileName = "../secret.txt";

            assertThatThrownBy(() -> fileStorageService.deleteFile(maliciousFileName))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Invalid file path");
        }

        @Test
        @DisplayName("Should delete existing file from disk")
        void deleteFile_ShouldDeleteFile_WhenExists() throws IOException {
            String fileName = "file-to-delete.png";
            Path targetPath = tempUploadDir.resolve(fileName);
            Files.write(targetPath, "some data".getBytes());

            assertThat(Files.exists(targetPath)).isTrue();

            fileStorageService.deleteFile(fileName);

            assertThat(Files.exists(targetPath)).isFalse();
        }

        @Test
        @DisplayName("Should execute silently when deleting non-existent file")
        void deleteFile_ShouldNotThrowException_WhenFileDoesNotExist() {
            String fileName = "non-existent-file.png";

            fileStorageService.deleteFile(fileName);

            assertThat(Files.exists(tempUploadDir.resolve(fileName))).isFalse();
        }
    }
}