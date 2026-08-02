package com.receiptbrain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ReceiptFileStorage {
    @Value("${app.upload.dir:uploads}") private String localUploadDir;
    @Value("${app.storage.supabase-url:}") private String supabaseUrl;
    @Value("${app.storage.service-key:}") private String serviceKey;
    @Value("${app.storage.bucket:receipts}") private String bucket;

    public String store(MultipartFile file, String storedName) throws IOException {
        if (supabaseUrl == null || supabaseUrl.isBlank() || serviceKey == null || serviceKey.isBlank()) return storeLocally(file, storedName);
        String endpoint = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + bucket + "/" + storedName;
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
        connection.setRequestMethod("POST"); connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + serviceKey);
        connection.setRequestProperty("apikey", serviceKey);
        connection.setRequestProperty("x-upsert", "false");
        connection.setRequestProperty("Content-Type", file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        connection.getOutputStream().write(file.getBytes());
        if (connection.getResponseCode() >= 300) throw new IOException("Supabase Storage upload failed (HTTP " + connection.getResponseCode() + ").");
        return endpoint;
    }

    private String storeLocally(MultipartFile file, String storedName) throws IOException {
        Path directory = Paths.get(localUploadDir); Files.createDirectories(directory);
        Path destination = directory.resolve(storedName); file.transferTo(destination);
        return destination.toString();
    }
}
