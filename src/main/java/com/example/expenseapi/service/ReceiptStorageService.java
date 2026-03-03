package com.example.expenseapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
public class ReceiptStorageService {

    private final Path uploadDir;

    public ReceiptStorageService(@Value("${app.receipt.upload-dir:./receipts}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create receipt upload directory: " + this.uploadDir, e);
        }
    }
    public String store(String filename, byte[] content) {
        try {
            String extension = "";
            int dot = filename.lastIndexOf('.');
            if (dot >= 0) extension = filename.substring(dot);
            String stored = "receipt_" + System.currentTimeMillis() + extension;
            Path target = uploadDir.resolve(stored);
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return stored;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store receipt file", e);
        }
    }
    public byte[] load(String filename) {
        try {
            Path file = uploadDir.resolve(filename).normalize();
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException("Could not read receipt file: " + filename, e);
        }
    }
    public void delete(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path file = uploadDir.resolve(filename).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
        }
    }
    public boolean exists(String filename) {
        if (filename == null || filename.isBlank()) return false;
        return Files.exists(uploadDir.resolve(filename).normalize());
    }
    public Path getUploadDir() {
        return uploadDir;
    }
}