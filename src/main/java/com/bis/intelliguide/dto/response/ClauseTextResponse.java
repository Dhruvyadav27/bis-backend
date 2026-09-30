package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ClauseTextResponse {
    private String isNumber;
    private String clauseRef;
    private String docTitle;
    private String text;          // in the user's language when translated
    private String originalText;  // English original, set only when text was translated
    private boolean translated;
}