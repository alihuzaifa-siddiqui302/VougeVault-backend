package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.CategoryRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.catalog.dto.ProductDto;
import com.ecommerce.VougeVault.catalog.dto.ProductFilterRequest;
import com.ecommerce.VougeVault.catalog.dto.ProductResponseDto;
import com.ecommerce.VougeVault.catalog.dto.ProductVariantResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.inventory.Repository.InventoryRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Data
@RequiredArgsConstructor
public class ProductService {
private final CategoryRepository categoryRepository;
private final ProductRepository productRepository;
private final InventoryRepository inventoryRepository;
    public Product createProduct(ProductDto dto,Long brandId) {
        Category category = categoryRepository.findByIdAndBrandId(dto.getCategoryId(), brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(category);

        Brand brand = new Brand();
        brand.setId(brandId);
        product.setBrand(brand);

        return productRepository.save(product);
    }
       public List<Product> getMyProducts(Long brandId){
        return productRepository.findByBrandId(brandId);
       }
       public Product getMyProduct(Long productId,Long brandId){
           Product product = productRepository.findByIdAndBrandId(productId, brandId)
                   .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
           return product;
       }

       public Product updateProduct(Long productId, ProductDto dto,Long brandId){
           Category category = categoryRepository.findByIdAndBrandId(dto.getCategoryId(), brandId)
                   .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
           Product product = productRepository.findByIdAndBrandId(productId, brandId)
                   .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
           product.setName(dto.getName());
           product.setDescription(dto.getDescription());
           product.setCategory(category);
           return productRepository.save(product);
       }
    public void deleteProduct(Long productId, Long brandId) {
        Product product = productRepository.findByIdAndBrandId(productId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);   // cascades to variants, per entity config
    }

    public List<ProductResponseDto> browseProducts(ProductFilterRequest filter) {
        List<Product> products;

        boolean hasCategory = filter.getCategoryId() != null;
        boolean hasBrand = filter.getBrandId() != null;
        boolean hasPriceRange = filter.getMinPrice() != null && filter.getMaxPrice() != null;

        if (hasCategory && hasBrand && hasPriceRange) {
            products = productRepository.findByCategoryIdAndBrandIdAndVariants_PriceBetween(
                    filter.getCategoryId(), filter.getBrandId(), filter.getMinPrice(), filter.getMaxPrice());
        } else if (hasCategory && hasBrand) {
            products = productRepository.findByCategoryIdAndBrandId(filter.getCategoryId(), filter.getBrandId());
        } else if (hasCategory && hasPriceRange) {
            products = productRepository.findByCategoryIdAndVariants_PriceBetween(
                    filter.getCategoryId(), filter.getMinPrice(), filter.getMaxPrice());
        } else if (hasBrand && hasPriceRange) {
            products = productRepository.findByBrandIdAndVariants_PriceBetween(
                    filter.getBrandId(), filter.getMinPrice(), filter.getMaxPrice());
        }else if (hasBrand) {
            products = productRepository.findByBrandId(filter.getBrandId());
        } else if (hasCategory) {
            products = productRepository.findByCategoryId(filter.getCategoryId());
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public ProductResponseDto toResponseDto(Product product){
        List<ProductVariantResponseDto> variantDtos = product.getVariants().stream()
                .map(variant -> new ProductVariantResponseDto(
                        variant.getId(),
                        variant.getSize(),
                        variant.getColor(),
                        variant.getPrice(),
                        variant.getSku(),
                        inventoryRepository.findByProductVariantId(variant.getId())
                                .map(inv -> inv.getStockQuantity())
                                .orElse(0)
                ))
                .collect(Collectors.toList());

        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getName(),
                product.getBrand().getName(),
                variantDtos
        );
    }

    }
