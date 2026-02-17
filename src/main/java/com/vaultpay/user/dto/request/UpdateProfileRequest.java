package com.vaultpay.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100)
    private String firstName;

    @Size(min = 2, max = 100)
    private String lastName;

    @Size(min = 10, max = 20)
    private String phoneNumber;
}
