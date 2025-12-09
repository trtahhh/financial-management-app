package com.example.finance.dto;

import lombok.Data;
import lombok.Builder;
import java.util.List;

@Data
@Builder
public class SmartAnalyticsResponse {
    private String mainMessage;
    private String detailedMessage;
    private List<TransactionSummary> transactions;
    private List<InsightItem> insights;
    private List<QuickAction> quickActions;
    
    @Data
    @Builder
    public static class TransactionSummary {
        private String description;
        private String category;
        private String date;
        private Double amount;
        private String highlight; // "largest", "unusual", "recurring"
    }
    
    @Data
    @Builder
    public static class InsightItem {
        private String icon; // emoji or icon class
        private String text;
        private String type; // "warning", "info", "success", "tip"
        private Double value; // numerical value if applicable
    }
    
    @Data
    @Builder
    public static class QuickAction {
        private String label;
        private String action; // "view_details", "get_tips", "analyze_category"
        private String categoryId; // optional, for category-specific actions
    }
}
