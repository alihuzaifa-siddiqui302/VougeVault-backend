package com.ecommerce.VougeVault.brand.repository;

import com.ecommerce.VougeVault.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrandRepository extends JpaRepository<Brand,Long> {


}
