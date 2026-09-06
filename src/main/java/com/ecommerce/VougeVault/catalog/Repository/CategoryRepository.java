package com.ecommerce.VougeVault.catalog.Repository;

import com.ecommerce.VougeVault.catalog.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {

    List<Category> findByBrandId(Long brandId);
    Optional<Category> findByIdAndBrandId(Long id, Long brandId);

}
