package com.example.expenseapi.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.example.expenseapi.repository.ExpenseRepository;
import com.example.expenseapi.model.Expense;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public List<Expense> getAllExpenses() {
        return expenseRepository.findAll();
    }

    public Expense createExpense(Expense expense) {
        return expenseRepository.save(expense);
    }

    public Expense updateExpense(Long id, Expense expenseDetails) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow();
        expense.setTitle(expenseDetails.getTitle());
        expense.setAmount(expenseDetails.getAmount());
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id) {
        expenseRepository.deleteById(id);
    }
}
