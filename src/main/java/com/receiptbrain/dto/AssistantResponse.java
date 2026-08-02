package com.receiptbrain.dto;

import java.util.List;

public record AssistantResponse(String answer, List<ReceiptDto> matchingReceipts) { }
