package com.bis.intelliguide.service.agents;

import com.bis.intelliguide.dto.response.HuidVerifyResponse;
import com.bis.intelliguide.model.HuidRecord;
import com.bis.intelliguide.repository.HuidRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HallmarkingService {

    private final HuidRecordRepository huidRecordRepository;

    public HuidVerifyResponse verifyHuid(String huid) {
        Optional<HuidRecord> record = huidRecordRepository.findByHuid(huid);

        if (record.isEmpty()) {
            return HuidVerifyResponse.builder()
                    .verified(false)
                    .message("No record found for this HUID. Double-check the 6-digit code, "
                            + "or this may not be a genuine BIS-hallmarked article.")
                    .build();
        }

        HuidRecord r = record.get();
        return HuidVerifyResponse.builder()
                .verified(true)
                .purity(r.getPurity())
                .ahcCentre(r.getAhcCentre())
                .hallmarkedOn(r.getHallmarkedOn())
                .message("Verified against BIS hallmarking records.")
                .build();
    }
}
