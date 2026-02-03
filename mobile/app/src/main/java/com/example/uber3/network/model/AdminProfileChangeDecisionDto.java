package com.example.uber3.network.model;

public class AdminProfileChangeDecisionDto {

    public boolean approved;
    public String rejectReason;

    public AdminProfileChangeDecisionDto(
            boolean approved,
            String rejectReason
    ) {
        this.approved = approved;
        this.rejectReason = rejectReason;
    }
}
