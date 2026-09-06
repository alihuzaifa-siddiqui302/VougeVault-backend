package com.ecommerce.VougeVault.returnmanagement.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.returnmanagement.dto.CreateReturnDto;
import com.ecommerce.VougeVault.returnmanagement.dto.ReturnResponseDto;
import com.ecommerce.VougeVault.returnmanagement.dto.UpdateReturnStatusDto;
import com.ecommerce.VougeVault.returnmanagement.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    // Customer: Create return request
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReturnResponseDto> createReturn(
            @Valid @RequestBody CreateReturnDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnService.createReturn(currentUser.getUserId(), dto));
    }

    // Customer: View their returns
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReturnResponseDto>> getMyReturns(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(returnService.getMyReturns(currentUser.getUserId()));
    }

    // Customer: View specific return
    @GetMapping("/{returnId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReturnResponseDto> getReturn(
            @PathVariable Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(returnService.getReturn(returnId, currentUser.getUserId()));
    }

    // Brand Admin: Get pending returns
    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<List<ReturnResponseDto>> getPendingReturns(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(returnService.getPendingReturnsForBrand(currentUser.getBrandId()));
    }

    // Brand Admin: Approve return
    @PostMapping("/{returnId}/approve")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<ReturnResponseDto> approveReturn(
            @PathVariable Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(returnService.approveReturn(returnId, currentUser.getBrandId()));
    }

    // Brand Admin: Reject return
    @PostMapping("/{returnId}/reject")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<ReturnResponseDto> rejectReturn(
            @PathVariable Long returnId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(returnService.rejectReturn(returnId, currentUser.getBrandId()));
    }

    // Delivery Person: Update return status (picking up, received)
    @PatchMapping("/{returnId}/status")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<ReturnResponseDto> updateReturnStatus(
            @PathVariable Long returnId,
            @Valid @RequestBody UpdateReturnStatusDto dto
    ) {
        return ResponseEntity.ok(returnService.updateReturnStatus(returnId, dto));
    }
}