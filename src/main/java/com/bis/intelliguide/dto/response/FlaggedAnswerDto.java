package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FlaggedAnswerDto {
    private String id;
    private String question;
    private String agent;
    private double confidence;
    private String answer;
    private String source;
}
