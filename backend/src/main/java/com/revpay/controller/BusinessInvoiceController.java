package com.revpay.controller;

import com.revpay.dto.CreateInvoiceRequest;
import com.revpay.dto.InvoicePaymentRequest;
import com.revpay.dto.InvoiceResponse;
import com.revpay.service.BusinessInvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business/invoices")
@RequiredArgsConstructor
public class BusinessInvoiceController {
    private final BusinessInvoiceService businessInvoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.ok(businessInvoiceService.createInvoice(userDetails.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> listMyInvoices(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(businessInvoiceService.getMyInvoices(userDetails.getUsername(), status));
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<InvoiceResponse>> lookupInvoices(
            @RequestParam String type,
            @RequestParam String value) {
        return ResponseEntity.ok(businessInvoiceService.lookupInvoices(type, value));
    }

    @PostMapping("/pay")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InvoicePaymentRequest request) {
        return ResponseEntity.ok(businessInvoiceService.payInvoice(userDetails.getUsername(), request));
    }
}
