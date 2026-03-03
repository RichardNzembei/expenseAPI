package com.example.expenseapi.controller;

import com.example.expenseapi.model.Expense;
import com.example.expenseapi.service.ExpenseService;
import com.example.expenseapi.service.ReceiptStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final ReceiptStorageService receiptStorage;

    @GetMapping
    public List<Expense> getAllExpenses() {
        return expenseService.getAllExpenses();
    }
    @PostMapping
    public Expense createExpense(@RequestBody Expense expense) {
        return expenseService.createExpense(expense);
    }
    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
        return expenseService.updateExpense(id, expense);
    }
    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        expenseService.deleteExpense(id);
    }
    @PostMapping(value = "/{id}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Expense> uploadReceipt(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        Expense expense = expenseService.findById(id);
        if (expense == null) return ResponseEntity.notFound().build();
        receiptStorage.delete(expense.getReceiptPath());

        String stored = receiptStorage.store(file.getOriginalFilename(), file.getBytes());
        expense.setReceiptPath(stored);
        return ResponseEntity.ok(expenseService.save(expense));
    }
    @GetMapping("/{id}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id) {
        Expense expense = expenseService.findById(id);
        if (expense == null || expense.getReceiptPath() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = receiptStorage.load(expense.getReceiptPath());
        String filename = expense.getReceiptPath();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (filename.endsWith(".pdf"))  mediaType = MediaType.APPLICATION_PDF;
        if (filename.endsWith(".png"))  mediaType = MediaType.IMAGE_PNG;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) mediaType = MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(data);
    }
}