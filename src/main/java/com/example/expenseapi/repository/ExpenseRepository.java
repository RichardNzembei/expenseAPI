package com.example.expenseapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.expenseapi.model.Expense;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}