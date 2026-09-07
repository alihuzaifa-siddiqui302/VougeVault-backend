package com.ecommerce.VougeVault.shared.config;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.brand.entity.BrandStatus;
import com.ecommerce.VougeVault.brand.repository.BrandRepository;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductImage;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.catalog.Repository.CategoryRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductImageRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.inventory.entity.Inventory;
import com.ecommerce.VougeVault.inventory.Repository.InventoryRepository;
import com.ecommerce.VougeVault.user.entity.Role;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("dev")
public class DataSeeder {

    private final UserRepository userRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductImageRepository productImageRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedData() {
        log.info("Starting data seeding...");

        // Only seed if no data exists
        if (userRepository.count() > 0) {
            log.info("Data already exists, skipping seeding");
            return;
        }

        // 1. Create Super Admin
        seedSuperAdmin();

        // 2. Create Brand Admin + Brand + Products
        seedBrandAndProducts();

        // 3. Create Test Customers
        seedCustomers();

        // 4. Create Delivery Persons
        seedDeliveryPersons();

        log.info("Data seeding completed!");
    }

    private void seedSuperAdmin() {
        log.info("Seeding Super Admin...");

        User superAdmin = new User();
        superAdmin.setEmail("admin@vougelvault.com");
        superAdmin.setPassword(passwordEncoder.encode("admin@123"));
        superAdmin.setName("VougeVault Admin");
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setBrand(null);  // Changed from setBrandId

        userRepository.save(superAdmin);
        log.info("Super Admin created: admin@vougelvault.com / admin@123");
    }

    private void seedBrandAndProducts() {
        log.info("Seeding Brand Admin, Brand, and Products...");

        // Create Brand Admin User
        User brandAdmin = new User();
        brandAdmin.setEmail("ecowear@vougelvault.com");
        brandAdmin.setPassword(passwordEncoder.encode("brand@123"));
        brandAdmin.setName("EcoWear Admin");
        brandAdmin.setRole(Role.BRAND_ADMIN);

        brandAdmin = userRepository.save(brandAdmin);

        // Create Brand
        Brand brand = new Brand();
        brand.setName("EcoWear");
        brand.setGstNumber("18AABCT1234H1Z0");
        brand.setStatus(BrandStatus.APPROVED);
        brand.setAddress("123 Fashion Street");
        brand.setCity("Mumbai");
        brand.setState("Maharashtra");
        brand.setPincode("400001");
        brand.setPhone("9876543210");
        brand.setEmail("ecowear@vougelvault.com");

        brand = brandRepository.save(brand);
        brandAdmin.setBrand(brand);  // Changed from setBrandId
        userRepository.save(brandAdmin);

        log.info("Brand created: EcoWear (ID: {})", brand.getId());

        // Create Categories
        Category shirtCategory = new Category();
        shirtCategory.setName("Shirts");
        shirtCategory.setBrand(brand);
        shirtCategory = categoryRepository.save(shirtCategory);

        Category pantsCategory = new Category();
        pantsCategory.setName("Pants");
        pantsCategory.setBrand(brand);
        pantsCategory = categoryRepository.save(pantsCategory);

        // Create Products with Variants (now returns products)
        Product product1 = seedProduct1(brand, shirtCategory);
        Product product2 = seedProduct2(brand, shirtCategory);
        Product product3 = seedProduct3(brand, pantsCategory);

        // Seed images for products
        seedProductImages(product1, product2, product3);

        log.info("Brand Admin created: ecowear@vougelvault.com / brand@123");
    }

    private Product seedProduct1(Brand brand, Category category) {
        Product product = new Product();
        product.setName("Blue Cotton Shirt");
        product.setDescription("Comfortable and breathable blue cotton shirt, perfect for everyday wear");
        product.setCategory(category);
        product.setBrand(brand);
        product = productRepository.save(product);

        // Variant 1: Blue, Size S
        ProductVariant variant1 = new ProductVariant();
        variant1.setProduct(product);
        variant1.setSize("S");
        variant1.setColor("Blue");
        variant1.setPrice(new BigDecimal("1499"));
        variant1.setSku(product.getId() + "-S-Blue");
        variant1 = productVariantRepository.save(variant1);

        Inventory inv1 = new Inventory();
        inv1.setVariant(variant1);  // Changed from setProductVariant
        inv1.setStockQuantity(50);
        inventoryRepository.save(inv1);

        // Variant 2: Blue, Size M
        ProductVariant variant2 = new ProductVariant();
        variant2.setProduct(product);
        variant2.setSize("M");
        variant2.setColor("Blue");
        variant2.setPrice(new BigDecimal("1499"));
        variant2.setSku(product.getId() + "-M-Blue");
        variant2 = productVariantRepository.save(variant2);

        Inventory inv2 = new Inventory();
        inv2.setVariant(variant2);  // Changed from setProductVariant
        inv2.setStockQuantity(40);
        inventoryRepository.save(inv2);

        // Variant 3: Blue, Size L
        ProductVariant variant3 = new ProductVariant();
        variant3.setProduct(product);
        variant3.setSize("L");
        variant3.setColor("Blue");
        variant3.setPrice(new BigDecimal("1499"));
        variant3.setSku(product.getId() + "-L-Blue");
        variant3 = productVariantRepository.save(variant3);

        Inventory inv3 = new Inventory();
        inv3.setVariant(variant3);  // Changed from setProductVariant
        inv3.setStockQuantity(35);
        inventoryRepository.save(inv3);

        log.info("Product created: Blue Cotton Shirt (ID: {})", product.getId());
        return product;  // Return product
    }

    private Product seedProduct2(Brand brand, Category category) {
        Product product = new Product();
        product.setName("White Linen Shirt");
        product.setDescription("Elegant white linen shirt for summer occasions");
        product.setCategory(category);
        product.setBrand(brand);
        product = productRepository.save(product);

        // Variant 1: White, Size M
        ProductVariant variant1 = new ProductVariant();
        variant1.setProduct(product);
        variant1.setSize("M");
        variant1.setColor("White");
        variant1.setPrice(new BigDecimal("1999"));
        variant1.setSku(product.getId() + "-M-White");
        variant1 = productVariantRepository.save(variant1);

        Inventory inv1 = new Inventory();
        inv1.setVariant(variant1);  // Changed from setProductVariant
        inv1.setStockQuantity(30);
        inventoryRepository.save(inv1);

        // Variant 2: White, Size L
        ProductVariant variant2 = new ProductVariant();
        variant2.setProduct(product);
        variant2.setSize("L");
        variant2.setColor("White");
        variant2.setPrice(new BigDecimal("1999"));
        variant2.setSku(product.getId() + "-L-White");
        variant2 = productVariantRepository.save(variant2);

        Inventory inv2 = new Inventory();
        inv2.setVariant(variant2);  // Changed from setProductVariant
        inv2.setStockQuantity(25);
        inventoryRepository.save(inv2);

        log.info("Product created: White Linen Shirt (ID: {})", product.getId());
        return product;  // Return product
    }

    private Product seedProduct3(Brand brand, Category category) {
        Product product = new Product();
        product.setName("Black Denim Jeans");
        product.setDescription("Classic black denim jeans with perfect fit");
        product.setCategory(category);
        product.setBrand(brand);
        product = productRepository.save(product);

        // Variant 1: Black, Size 30
        ProductVariant variant1 = new ProductVariant();
        variant1.setProduct(product);
        variant1.setSize("30");
        variant1.setColor("Black");
        variant1.setPrice(new BigDecimal("2499"));
        variant1.setSku(product.getId() + "-30-Black");
        variant1 = productVariantRepository.save(variant1);

        Inventory inv1 = new Inventory();
        inv1.setVariant(variant1);  // Changed from setProductVariant
        inv1.setStockQuantity(45);
        inventoryRepository.save(inv1);

        // Variant 2: Black, Size 32
        ProductVariant variant2 = new ProductVariant();
        variant2.setProduct(product);
        variant2.setSize("32");
        variant2.setColor("Black");
        variant2.setPrice(new BigDecimal("2499"));
        variant2.setSku(product.getId() + "-32-Black");
        variant2 = productVariantRepository.save(variant2);

        Inventory inv2 = new Inventory();
        inv2.setVariant(variant2);  // Changed from setProductVariant
        inv2.setStockQuantity(40);
        inventoryRepository.save(inv2);

        log.info("Product created: Black Denim Jeans (ID: {})", product.getId());
        return product;  // Return product
    }

    private void seedProductImages(Product product1, Product product2, Product product3) {
        log.info("Seeding product images...");

        try {
            downloadAndSaveImage(product1.getId(),
                    "https://via.placeholder.com/500x600/0066cc/ffffff?text=Blue+Cotton+Shirt",
                    "blue_shirt.jpg", true);

            downloadAndSaveImage(product2.getId(),
                    "https://via.placeholder.com/500x600/ffffff/000000?text=White+Linen+Shirt",
                    "white_shirt.jpg", true);

            downloadAndSaveImage(product3.getId(),
                    "https://via.placeholder.com/500x600/1a1a1a/ffffff?text=Black+Denim+Jeans",
                    "black_jeans.jpg", true);

            log.info("Product images seeded successfully");
        } catch (Exception e) {
            log.error("Error seeding product images: {}", e.getMessage());
        }
    }

    private void downloadAndSaveImage(Long productId, String imageUrl, String originalFileName, Boolean isPrimary) {
        try {
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                log.warn("Product not found: {}", productId);
                return;
            }

            log.info("Downloading image for product {}: {}", productId, imageUrl);

            // Download image
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (InputStream is = connection.getInputStream()) {
                // Create upload directory if not exists
                Path uploadPath = Paths.get("uploads/products");
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    log.info("Created upload directory: {}", uploadPath);
                }

                // Generate unique filename
                String storedFileName = UUID.randomUUID().toString() + ".jpg";
                Path filePath = uploadPath.resolve(storedFileName);

                // Save file
                long bytesCopied = Files.copy(is, filePath);
                log.info("Image saved: {} ({} bytes)", storedFileName, bytesCopied);

                // Save to database
                ProductImage productImage = new ProductImage();
                productImage.setProduct(product);
                productImage.setFileName(storedFileName);
                productImage.setFilePath(storedFileName);
                productImage.setOriginalFileName(originalFileName);
                productImage.setFileSize(bytesCopied);
                productImage.setMimeType("image/jpeg");
                productImage.setIsPrimary(isPrimary);

                productImageRepository.save(productImage);
                log.info("Image record saved for product {} ({})", productId, originalFileName);
            }
        } catch (Exception e) {
            log.error("Failed to download/save image for product {}: {}", productId, e.getMessage());
        }
    }

    private void seedCustomers() {
        log.info("Seeding Test Customers...");

        String[] customerEmails = {
                "john@example.com",
                "jane@example.com",
                "mike@example.com",
                "sarah@example.com",
                "alex@example.com"
        };

        String[] customerNames = {
                "John Doe",
                "Jane Smith",
                "Mike Johnson",
                "Sarah Williams",
                "Alex Brown"
        };

        for (int i = 0; i < customerEmails.length; i++) {
            User customer = new User();
            customer.setEmail(customerEmails[i]);
            customer.setPassword(passwordEncoder.encode("customer@123"));
            customer.setName(customerNames[i]);
            customer.setRole(Role.CUSTOMER);
            customer.setBrand(null);  // Changed from setBrandId

            userRepository.save(customer);
        }

        log.info("Created {} test customers (password: customer@123)", customerEmails.length);
    }

    private void seedDeliveryPersons() {
        log.info("Seeding Test Delivery Persons...");

        String[] deliveryEmails = {
                "delivery1@vougelvault.com",
                "delivery2@vougelvault.com",
                "delivery3@vougelvault.com"
        };

        String[] deliveryNames = {
                "Raj Kumar",
                "Priya Singh",
                "Arun Patel"
        };

        for (int i = 0; i < deliveryEmails.length; i++) {
            User deliveryPerson = new User();
            deliveryPerson.setEmail(deliveryEmails[i]);
            deliveryPerson.setPassword(passwordEncoder.encode("delivery@123"));
            deliveryPerson.setName(deliveryNames[i]);
            deliveryPerson.setRole(Role.DELIVERY_PERSON);
            deliveryPerson.setBrand(null);  // Changed from setBrandId

            userRepository.save(deliveryPerson);
        }

        log.info("Created 3 test delivery persons (password: delivery@123)");
    }
}