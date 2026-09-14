package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ComplaintResponse {
    private String complaintId;
    private List<String> steps;
    private String applicableClause;
    private String status;
    private String redirectUrl;
}
