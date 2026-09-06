package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.ProductImageRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.catalog.dto.ProductImageDto;
import com.ecommerce.VougeVault.catalog.dto.UploadImageResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductImage;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ProductImageService productImageService;

    private Product sampleProduct;
    private Brand sampleBrand;
    private ProductImage sampleImage;

    private static final String BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(productImageService, "apiBaseUrl", BASE_URL);

        sampleBrand = new Brand();
        sampleBrand.setId(1L);
        sampleBrand.setName("Vouge Originals");

        sampleProduct = new Product();
        sampleProduct.setId(10L);
        sampleProduct.setName("Linen Shirt");
        sampleProduct.setBrand(sampleBrand);

        sampleImage = new ProductImage();
        sampleImage.setId(100L);
        sampleImage.setProduct(sampleProduct);
        sampleImage.setFileName("stored-image-uuid.png");
        sampleImage.setFilePath("stored-image-uuid.png");
        sampleImage.setOriginalFileName("shirt.png");
        sampleImage.setFileSize(2048L);
        sampleImage.setMimeType("image/png");
        sampleImage.setIsPrimary(true);
        sampleImage.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("uploadImage() Tests")
    class UploadImageTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when target product does not exist")
        void uploadImage_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.uploadImage(10L, multipartFile, true))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verifyNoInteractions(fileStorageService);
            verify(productImageRepository, never()).save(any(ProductImage.class));
        }

        @Test
        @DisplayName("Should reset existing primary images and save new image as primary when isPrimary is true")
        void uploadImage_ShouldResetOldPrimaryAndSave_WhenIsPrimaryTrue() throws IOException {
            ProductImage existingPrimary = new ProductImage();
            existingPrimary.setId(99L);
            existingPrimary.setIsPrimary(true);

            when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
            when(fileStorageService.storeFile(multipartFile, 10L)).thenReturn("stored-image-uuid.png");
            when(productImageRepository.findByProductId(10L)).thenReturn(List.of(existingPrimary));
            when(multipartFile.getOriginalFilename()).thenReturn("shirt.png");
            when(multipartFile.getSize()).thenReturn(2048L);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> {
                ProductImage img = invocation.getArgument(0);
                if (img.getId() == null) {
                    img.setId(100L);
                }
                return img;
            });

            UploadImageResponseDto response = productImageService.uploadImage(10L, multipartFile, true);

            assertThat(existingPrimary.getIsPrimary()).isFalse();
            verify(productImageRepository).save(existingPrimary);

            ArgumentCaptor<ProductImage> imageCaptor = ArgumentCaptor.forClass(ProductImage.class);
            verify(productImageRepository, times(2)).save(imageCaptor.capture());

            ProductImage newSavedImage = imageCaptor.getValue();
            assertThat(newSavedImage.getFileName()).isEqualTo("stored-image-uuid.png");
            assertThat(newSavedImage.getIsPrimary()).isTrue();
            assertThat(newSavedImage.getProduct()).isEqualTo(sampleProduct);

            assertThat(response.getImageId()).isEqualTo(100L);
            assertThat(response.getFileName()).isEqualTo("stored-image-uuid.png");
            assertThat(response.getDownloadUrl()).isEqualTo(BASE_URL + "/api/products/images/download/stored-image-uuid.png");
            assertThat(response.getIsPrimary()).isTrue();
            assertThat(response.getFileSize()).isEqualTo(2048L);
        }

        @Test
        @DisplayName("Should set isPrimary to false when parameter is false or null")
        void uploadImage_ShouldSetPrimaryFalse_WhenIsPrimaryFalseOrNull() throws IOException {
            when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
            when(fileStorageService.storeFile(multipartFile, 10L)).thenReturn("stored-image-uuid.png");
            when(multipartFile.getOriginalFilename()).thenReturn("shirt.png");
            when(multipartFile.getSize()).thenReturn(1024L);
            when(multipartFile.getContentType()).thenReturn("image/png");
            when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

            UploadImageResponseDto response = productImageService.uploadImage(10L, multipartFile, null);

            assertThat(response.getIsPrimary()).isFalse();
            verify(productImageRepository, never()).findByProductId(anyLong());
        }
    }

    @Nested
    @DisplayName("getProductImages() Tests")
    class GetProductImagesTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product does not exist")
        void getProductImages_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.getProductImages(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(productImageRepository, never()).findImagesByProductIdOrderByCreated(anyLong());
        }

        @Test
        @DisplayName("Should return mapped list of ProductImageDto when product exists")
        void getProductImages_ShouldReturnMappedDtos_WhenProductExists() {
            when(productRepository.findById(10L)).thenReturn(Optional.of(sampleProduct));
            when(productImageRepository.findImagesByProductIdOrderByCreated(10L)).thenReturn(List.of(sampleImage));

            List<ProductImageDto> result = productImageService.getProductImages(10L);

            assertThat(result).hasSize(1);
            ProductImageDto dto = result.get(0);
            assertThat(dto.getImageId()).isEqualTo(100L);
            assertThat(dto.getFileName()).isEqualTo("stored-image-uuid.png");
            assertThat(dto.getDownloadUrl()).isEqualTo(BASE_URL + "/api/products/images/download/stored-image-uuid.png");
            assertThat(dto.getFileSize()).isEqualTo(2048L);
            assertThat(dto.getMimeType()).isEqualTo("image/png");
            assertThat(dto.getIsPrimary()).isTrue();
        }
    }

    @Nested
    @DisplayName("getPrimaryImage() Tests")
    class GetPrimaryImageTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when primary image does not exist")
        void getPrimaryImage_ShouldThrowException_WhenNotFound() {
            when(productImageRepository.findPrimaryImageByProductId(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.getPrimaryImage(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Primary image not found for product");
        }

        @Test
        @DisplayName("Should return primary image DTO when found")
        void getPrimaryImage_ShouldReturnDto_WhenFound() {
            when(productImageRepository.findPrimaryImageByProductId(10L)).thenReturn(Optional.of(sampleImage));

            ProductImageDto dto = productImageService.getPrimaryImage(10L);

            assertThat(dto).isNotNull();
            assertThat(dto.getImageId()).isEqualTo(100L);
            assertThat(dto.getIsPrimary()).isTrue();
            assertThat(dto.getDownloadUrl()).isEqualTo(BASE_URL + "/api/products/images/download/stored-image-uuid.png");
        }
    }

    @Nested
    @DisplayName("downloadImage() Tests")
    class DownloadImageTests {

        @Test
        @DisplayName("Should delegate to fileStorageService and return file bytes")
        void downloadImage_ShouldReturnByteArray() throws IOException {
            byte[] mockBytes = "file-content".getBytes();
            when(fileStorageService.retrieveFile("stored-image-uuid.png")).thenReturn(mockBytes);

            byte[] result = productImageService.downloadImage("stored-image-uuid.png");

            assertThat(result).isEqualTo(mockBytes);
            verify(fileStorageService).retrieveFile("stored-image-uuid.png");
        }
    }

    @Nested
    @DisplayName("deleteImage() Tests")
    class DeleteImageTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when image is not found")
        void deleteImage_ShouldThrowException_WhenImageNotFound() {
            when(productImageRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.deleteImage(100L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Image not found");

            verify(fileStorageService, never()).deleteFile(anyString());
            verify(productImageRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand ID does not match product brand")
        void deleteImage_ShouldThrowException_WhenBrandMismatch() {
            Long unauthorizedBrandId = 999L;
            when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

            assertThatThrownBy(() -> productImageService.deleteImage(100L, unauthorizedBrandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Image not found");

            verify(fileStorageService, never()).deleteFile(anyString());
            verify(productImageRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should delete file from storage and remove entity from database when authorized")
        void deleteImage_ShouldDeleteFileAndEntity_WhenAuthorized() {
            when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

            productImageService.deleteImage(100L, 1L);

            verify(fileStorageService).deleteFile("stored-image-uuid.png");
            verify(productImageRepository).deleteById(100L);
        }
    }

    @Nested
    @DisplayName("setPrimaryImage() Tests")
    class SetPrimaryImageTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when image is not found")
        void setPrimaryImage_ShouldThrowException_WhenImageNotFound() {
            when(productImageRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productImageService.setPrimaryImage(100L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Image not found");

            verify(productImageRepository, never()).save(any(ProductImage.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand ID does not match")
        void setPrimaryImage_ShouldThrowException_WhenBrandMismatch() {
            Long unauthorizedBrandId = 999L;
            when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

            assertThatThrownBy(() -> productImageService.setPrimaryImage(100L, unauthorizedBrandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Image not found");

            verify(productImageRepository, never()).save(any(ProductImage.class));
        }

        @Test
        @DisplayName("Should unset old primary images and mark selected image as primary")
        void setPrimaryImage_ShouldUnsetOldPrimariesAndSetNew_WhenAuthorized() {
            sampleImage.setIsPrimary(false);

            ProductImage oldPrimary = new ProductImage();
            oldPrimary.setId(101L);
            oldPrimary.setIsPrimary(true);

            when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
            when(productImageRepository.findByProductId(10L)).thenReturn(List.of(oldPrimary));

            productImageService.setPrimaryImage(100L, 1L);

            assertThat(oldPrimary.getIsPrimary()).isFalse();
            verify(productImageRepository).save(oldPrimary);

            assertThat(sampleImage.getIsPrimary()).isTrue();
            verify(productImageRepository).save(sampleImage);
        }
    }
}