package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ConsumerTrendsResponse {
    private List<CategoryCount> categories;
    private List<DateCount> trend;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CategoryCount {
        private String name;
        private long count;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DateCount {
        private String date;
        private long count;
    }
}
