package com.bis.intelliguide.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintGuidanceResponse {
    private String answer;
    private String applicableClause;
    private String compensationInfo;
}
