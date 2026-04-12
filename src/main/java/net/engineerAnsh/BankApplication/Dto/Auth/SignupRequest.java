package net.engineerAnsh.BankApplication.Dto.Auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Validations.PasswordMatches;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatches // custom validation annotation.
public class SignupRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 3, max = 50)
    private String fullName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Age is required")
    @Min(value = 12, message = "You must be at least 12 years old")
    @Max(value = 120, message = "Invalid age")
    private Integer age;

    @NotBlank(message = "Phone cannot be empty")
    @Pattern(
            regexp = "^(\\+91)?[6-9][0-9]{9}$",
            message = "Invalid Indian phone number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain upper, lower, number and special character"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

}
