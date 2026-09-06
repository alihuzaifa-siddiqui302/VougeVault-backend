package com.ecommerce.VougeVault.returnmanagement.repository;

import com.ecommerce.VougeVault.returnmanagement.entity.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

        @Query("SELECT ri FROM ReturnItem ri WHERE ri.return_.id = :returnId")
        List<ReturnItem> findByReturnId(@Param("returnId") Long returnId);
    }
