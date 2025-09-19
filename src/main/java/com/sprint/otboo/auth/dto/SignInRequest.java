package com.sprint.otboo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest (
    @NotBlank
    @Email
    String username,

    @NotBlank
    @Size(min = 4, max = 20)
    String password
){

}
