package com.ecommerce.VougeVault.user.repository;

import com.ecommerce.VougeVault.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    // Add these methods to existing UserRepository

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER'")
    Long countCustomers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'BRAND_ADMIN'")
    Long countBrandAdmins();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'DELIVERY_PERSON'")
    Long countDeliveryPersons();
}
