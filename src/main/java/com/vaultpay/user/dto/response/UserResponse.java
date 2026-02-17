package com.vaultpay.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String status;
    private String kycLevel;
    private LocalDateTime createdAt;
}
