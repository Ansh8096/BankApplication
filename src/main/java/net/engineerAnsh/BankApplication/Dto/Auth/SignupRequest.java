package net.engineerAnsh.BankApplication.Dto.Auth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Validations.PasswordMatches;
import org.hibernate.validator.constraints.Length;

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
    @Length(min = 13, max = 13, message = "Phone no is invalid")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).*$",
            message = "Password must contain upper, lower, number and special character"
    )
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

}
