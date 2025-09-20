package org.help789.mds.Entity.Vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Register request")
public class RegisterReq {

    @Schema(description = "Account (login name)", example = "alice")
    @NotBlank(message = "account cannot be blank")
    @Size(min = 3, max = 60, message = "account length must be 3–60")
    private String account;

    @Schema(description = "Email", example = "alice@example.com")
    @NotBlank(message = "email cannot be blank")
    @Email(message = "invalid email format")
    @Size(max = 255, message = "email too long")
    private String email;

    @Schema(description = "Role", example = "researchers",
            allowableValues = {"administrators","researchers","analysts","auditors"})
    @NotBlank(message = "role cannot be blank")
    private String role;

    @Schema(description = "Password (BCrypt on server side)", example = "Abc@12345")
    @NotBlank(message = "password cannot be blank")
    @Size(min = 6, max = 128, message = "password length must be >= 6")
    private String password;

    @Schema(description = "Confirm password", example = "Abc@12345")
    @NotBlank(message = "confirmPassword cannot be blank")
    private String confirmPassword;
}
