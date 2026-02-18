package com.vaultpay.user.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String firstName,
        @Size(min = 2, max = 100) String lastName,
        @Size(min = 10, max = 20) String phoneNumber
) {}
