package com.carlosdaza.splitexpense.controller;

import com.carlosdaza.splitexpense.config.AuthHelper;
import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import com.carlosdaza.splitexpense.service.ExpenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
@RequiredArgsConstructor
@Tag(name = "Expenses", description = "Record and query group expenses")
@SecurityRequirement(name = "bearerAuth")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final AuthHelper authHelper;

    @PostMapping
    @Operation(summary = "Add an expense to the group")
    public ResponseEntity<SplitDto.ExpenseResponse> create(
            @PathVariable UUID groupId,
            @Valid @RequestBody SplitDto.ExpenseRequest request) {
        UUID payerId = authHelper.getCurrentUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(expenseService.createExpense(groupId, payerId, request));
    }

    @GetMapping
    @Operation(summary = "List all expenses in the group")
    public ResponseEntity<List<SplitDto.ExpenseResponse>> getAll(@PathVariable UUID groupId) {
        return ResponseEntity.ok(expenseService.getGroupExpenses(groupId));
    }

    @GetMapping("/balance")
    @Operation(summary = "Get balance summary and debts for the group")
    public ResponseEntity<SplitDto.GroupBalanceResponse> getBalance(@PathVariable UUID groupId) {
        return ResponseEntity.ok(expenseService.getGroupBalance(groupId));
    }
}
