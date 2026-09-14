package com.bis.intelliguide.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Standard/clause citation attached to every substantive AI answer. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CitationRef {
    private String doc;
    private String clause;
}
