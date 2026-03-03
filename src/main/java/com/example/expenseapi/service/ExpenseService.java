package com.example.expenseapi.service;

import com.example.expenseapi.model.Expense;
import com.example.expenseapi.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final ReceiptStorageService receiptStorage;

    public ExpenseService(ExpenseRepository repository, ReceiptStorageService receiptStorage) {
        this.repository = repository;
        this.receiptStorage = receiptStorage;
    }
    public List<Expense> getAllExpenses() {
        return repository.findAll();
    }
    public Expense createExpense(Expense expense) {
        return repository.save(expense);
    }

    public Expense updateExpense(Long id, Expense expense) {
        Expense existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found with id " + id));
        existing.setTitle(expense.getTitle());
        existing.setAmount(expense.getAmount());
        existing.setCategory(expense.getCategory());
        if (expense.getReceiptPath() != null) {
            if (existing.getReceiptPath() != null && !existing.getReceiptPath().equals(expense.getReceiptPath())) {
                receiptStorage.delete(existing.getReceiptPath());
            }
            existing.setReceiptPath(expense.getReceiptPath());
        }
        return repository.save(existing);
    }

    public void deleteExpense(Long id) {
        repository.findById(id).ifPresent(e -> receiptStorage.delete(e.getReceiptPath()));
        repository.deleteById(id);
    }
    public Expense findById(Long id) {
        return repository.findById(id).orElse(null);
    }
    public List<Expense> findAll() {
        return getAllExpenses();
    }
    public Expense save(Expense expense) {
        if (expense.getId() == null) {
            return createExpense(expense);
        } else {
            return updateExpense(expense.getId(), expense);
        }
    }
    public void delete(Long id) {
        deleteExpense(id);
    }
}