package com.receiptbrain.service;

import com.receiptbrain.dto.AnalyticsSummaryDto;
import com.receiptbrain.dto.ReceiptDto;
import com.receiptbrain.dto.ReceiptSearchRequest;
import com.receiptbrain.dto.ReceiptUpdateRequest;
import com.receiptbrain.dto.WarrantyAlertDto;
import com.receiptbrain.entity.Receipt;
import com.receiptbrain.entity.ReceiptItem;
import com.receiptbrain.entity.User;
import com.receiptbrain.repository.ReceiptRepository;
import com.receiptbrain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final ReceiptFileStorage receiptFileStorage;

    public ReceiptDto uploadReceipt(MultipartFile file, String email) throws IOException {
        User user = userRepository.findByEmail(email).orElseThrow();
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String storedPath = receiptFileStorage.store(file, fileName);

        Receipt receipt = new Receipt();
        ExtractedReceipt extracted = extractReceiptDetails(file);
        receipt.setUser(user);
        receipt.setFileName(fileName);
        receipt.setFilePath(storedPath);
        receipt.setMerchant(extracted.merchant());
        receipt.setPurchaseDate(extracted.purchaseDate());
        receipt.setAmount(extracted.amount());
        receipt.setCurrency("INR");
        receipt.setCategory(extracted.category());
        receipt.setPaymentMethod(extracted.paymentMethod());
        receipt.setWarrantyMonths(extracted.warrantyMonths());
        receipt.setWarrantyExpiryDate(extracted.warrantyMonths() == 0 ? null : extracted.purchaseDate().plusMonths(extracted.warrantyMonths()));
        receipt.setRawText(extracted.rawText());
        receipt.setAiSummary(extracted.extracted() ? "Details were automatically extracted from the uploaded document. Please review them before saving." : "Receipt stored securely. Suggested merchant and category were inferred from the filename; add the amount, date and warranty details to complete it.");
        receipt.setItems(new ArrayList<>());

        ReceiptItem item = new ReceiptItem();
        item.setReceipt(receipt);
        item.setName("Uploaded item");
        receipt.getItems().add(item);

        Receipt saved = receiptRepository.save(receipt);
        return toDto(saved);
    }

    public List<ReceiptDto> getUserReceipts(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return receiptRepository.findByUser(user).stream().map(this::toDto).toList();
    }

    public Optional<ReceiptDto> getReceipt(Long id, String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return receiptRepository.findById(id)
                .filter(receipt -> receipt.getUser().getId().equals(user.getId()))
                .map(this::toDto);
    }

    public Optional<ReceiptDto> updateReceipt(Long id, String email, ReceiptUpdateRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return receiptRepository.findById(id)
                .filter(receipt -> receipt.getUser().getId().equals(user.getId()))
                .map(receipt -> {
                    receipt.setMerchant(request.merchant().trim());
                    receipt.setPurchaseDate(request.purchaseDate());
                    receipt.setAmount(request.amount());
                    receipt.setCurrency(blankOrDefault(request.currency(), "INR"));
                    receipt.setCategory(blankOrDefault(request.category(), "Others"));
                    receipt.setPaymentMethod(blankOrDefault(request.paymentMethod(), "Unknown"));
                    receipt.setGstNumber(blankToNull(request.gstNumber()));
                    int months = request.warrantyMonths() == null ? 0 : Math.max(0, request.warrantyMonths());
                    receipt.setWarrantyMonths(months);
                    receipt.setWarrantyExpiryDate(months == 0 ? null : request.purchaseDate().plusMonths(months));
                    receipt.setAiSummary("Receipt details verified and saved. " + (months > 0 ? "Warranty expires on " + receipt.getWarrantyExpiryDate() + "." : "No warranty reminder set."));
                    return toDto(receiptRepository.save(receipt));
                });
    }

    public List<WarrantyAlertDto> getWarrantyAlerts(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        LocalDate today = LocalDate.now();
        LocalDate alertWindow = today.plusDays(60);
        return receiptRepository.findByUser(user).stream()
                .filter(receipt -> receipt.getWarrantyExpiryDate() != null)
                .filter(receipt -> !receipt.getWarrantyExpiryDate().isAfter(alertWindow))
                .sorted(java.util.Comparator.comparing(Receipt::getWarrantyExpiryDate))
                .map(receipt -> {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(today, receipt.getWarrantyExpiryDate());
                    String status = days < 0 ? "Expired" : days == 0 ? "Expires today" : days <= 14 ? "Expiring soon" : "Upcoming";
                    String item = receipt.getItems() == null || receipt.getItems().isEmpty() ? "Purchased item" : receipt.getItems().get(0).getName();
                    return new WarrantyAlertDto(receipt.getId(), receipt.getMerchant(), item, receipt.getWarrantyExpiryDate(), days, status);
                }).toList();
    }

    public List<ReceiptDto> searchReceipts(String email, ReceiptSearchRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow();
        return receiptRepository.findByUser(user).stream()
                .filter(receipt -> request.merchant() == null || request.merchant().isBlank() || receipt.getMerchant() != null && receipt.getMerchant().toLowerCase().contains(request.merchant().toLowerCase()))
                .filter(receipt -> request.category() == null || request.category().isBlank() || receipt.getCategory() != null && receipt.getCategory().equalsIgnoreCase(request.category()))
                .filter(receipt -> request.query() == null || request.query().isBlank() || (receipt.getMerchant() != null && receipt.getMerchant().toLowerCase().contains(request.query().toLowerCase())) || (receipt.getAiSummary() != null && receipt.getAiSummary().toLowerCase().contains(request.query().toLowerCase())))
                .map(this::toDto)
                .toList();
    }

    public AnalyticsSummaryDto getAnalyticsSummary(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        List<Receipt> receipts = receiptRepository.findByUser(user);

        BigDecimal totalSpending = receipts.stream()
                .map(Receipt::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageReceipt = receipts.isEmpty() ? BigDecimal.ZERO : totalSpending.divide(BigDecimal.valueOf(receipts.size()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal highestExpense = receipts.stream()
                .map(Receipt::getAmount)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<AnalyticsSummaryDto.Breakdown> categoryBreakdown = receipts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        receipt -> receipt.getCategory() == null ? "Others" : receipt.getCategory(),
                        java.util.stream.Collectors.mapping(Receipt::getAmount, java.util.stream.Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .map(entry -> new AnalyticsSummaryDto.Breakdown(entry.getKey(), entry.getValue()))
                .toList();

        List<AnalyticsSummaryDto.Breakdown> merchantBreakdown = receipts.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        receipt -> receipt.getMerchant() == null ? "Unknown" : receipt.getMerchant(),
                        java.util.stream.Collectors.mapping(Receipt::getAmount, java.util.stream.Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
                .entrySet().stream()
                .map(entry -> new AnalyticsSummaryDto.Breakdown(entry.getKey(), entry.getValue()))
                .toList();

        List<AnalyticsSummaryDto.RecentReceiptDto> recentReceipts = receipts.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(receipt -> new AnalyticsSummaryDto.RecentReceiptDto(receipt.getId(), receipt.getMerchant(), receipt.getAmount(), receipt.getCategory(), receipt.getPurchaseDate()))
                .toList();

        return new AnalyticsSummaryDto(receipts.size(), totalSpending, averageReceipt, highestExpense, categoryBreakdown, merchantBreakdown, recentReceipts);
    }

    private ReceiptDto toDto(Receipt receipt) {
        return new ReceiptDto(receipt.getId(), receipt.getMerchant(), receipt.getPurchaseDate(), receipt.getAmount(), receipt.getCurrency(),
                receipt.getCategory(), receipt.getPaymentMethod(), receipt.getGstNumber(), receipt.getWarrantyMonths(), receipt.getWarrantyExpiryDate(), receipt.getFileName(), receipt.getAiSummary(), receipt.getCreatedAt());
    }

    private String guessMerchant(String fileName) {
        if (fileName == null || fileName.isBlank()) return "New receipt";
        String cleaned = fileName.replaceFirst("\\.[^.]+$", "").replaceAll("[_-]+", " ").trim();
        return cleaned.isBlank() ? "New receipt" : cleaned.substring(0, Math.min(cleaned.length(), 80));
    }

    private String guessCategory(String fileName) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        if (name.matches(".*(restaurant|food|cafe|swiggy|zomato).*")) return "Food & Dining";
        if (name.matches(".*(amazon|flipkart|electronics|mobile|laptop).*")) return "Shopping";
        if (name.matches(".*(fuel|petrol|uber|ola|metro).*")) return "Transport";
        if (name.matches(".*(medical|pharmacy|hospital).*")) return "Health";
        return "Others";
    }

    private String blankOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Extracts embedded text from SVG/text demo documents. Photo/PDF OCR can be plugged in here later. */
    private ExtractedReceipt extractReceiptDetails(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        ExtractedReceipt fallback = new ExtractedReceipt(guessMerchant(fileName), LocalDate.now(), BigDecimal.ZERO,
                guessCategory(fileName), "Unknown", 0, "Automatic extraction needs an OCR provider. Review the suggested details below before saving.", false);
        if (fileName == null || !(fileName.toLowerCase(Locale.ROOT).endsWith(".svg") || fileName.toLowerCase(Locale.ROOT).endsWith(".txt"))) return fallback;

        String raw = new String(file.getBytes(), StandardCharsets.UTF_8);
        String text = raw.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        BigDecimal amount = findAmount(text, "(?i)TOTAL\\s*(?:₹|INR|RS\\.?)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)");
        if (amount == null) amount = BigDecimal.ZERO;
        LocalDate purchaseDate = findDate(text);
        int warrantyMonths = findInteger(text, "(?i)(?:WARRANTY|COVERAGE PERIOD)\\s*:?\\s*(\\d+)\\s*MONTHS", 0);
        if (warrantyMonths == 0) warrantyMonths = findInteger(text, "(?i)(\\d+)\\s*MONTHS", 0);
        String merchant = merchantFromText(text, fallback.merchant());
        String payment = findPayment(text);
        String category = categoryForMerchant(merchant, fallback.category());
        boolean extracted = amount.compareTo(BigDecimal.ZERO) > 0 || !payment.equals("Unknown") || warrantyMonths > 0;
        return new ExtractedReceipt(merchant, purchaseDate, amount, category, payment, warrantyMonths, text, extracted);
    }

    private BigDecimal findAmount(String text, String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? new BigDecimal(matcher.group(1).replace(",", "")) : null;
    }

    private LocalDate findDate(String text) {
        Matcher matcher = Pattern.compile("(?i)Date:\\s*(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4})").matcher(text);
        if (matcher.find()) {
            try { return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH)); }
            catch (DateTimeParseException ignored) { }
        }
        return LocalDate.now();
    }

    private int findInteger(String text, String pattern, int fallback) {
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private String findPayment(String text) {
        Matcher matcher = Pattern.compile("(?i)(?:Payment|Paid via)\\s*:?\\s*(UPI|CARD|CASH|NET BANKING)").matcher(text);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ROOT) : "Unknown";
    }

    private String merchantFromText(String text, String fallback) {
        if (text.contains("TECHMART ELECTRONICS")) return "TechMart Electronics";
        if (text.contains("HOMECARE")) return "HomeCare";
        if (text.contains("FRESHBASKET")) return "FreshBasket";
        if (text.contains("URBANRIDE MOBILITY")) return "UrbanRide Mobility";
        return fallback;
    }

    private String categoryForMerchant(String merchant, String fallback) {
        String name = merchant.toLowerCase(Locale.ROOT);
        if (name.contains("techmart")) return "Shopping";
        if (name.contains("homecare")) return "Home";
        if (name.contains("freshbasket")) return "Groceries";
        if (name.contains("urbanride")) return "Transport";
        return fallback;
    }

    private record ExtractedReceipt(String merchant, LocalDate purchaseDate, BigDecimal amount, String category,
                                    String paymentMethod, int warrantyMonths, String rawText, boolean extracted) { }
}
