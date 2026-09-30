package com.bis.intelliguide.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneyAskResponse {
    private String answer;
    private int relatedStep;
}
