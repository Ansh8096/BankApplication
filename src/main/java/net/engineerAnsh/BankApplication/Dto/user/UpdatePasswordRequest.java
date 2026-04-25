package net.engineerAnsh.BankApplication.Dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import net.engineerAnsh.BankApplication.Validations.PasswordMatchable;
import net.engineerAnsh.BankApplication.Validations.PasswordMatches;

@Getter
@Setter
@PasswordMatches(message = "New password and confirm password must match")
public class UpdatePasswordRequest implements PasswordMatchable {

    @NotBlank(message = "Old password is required")
    private String oldPassword;

    @NotBlank(message = "New password is required")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain upper, lower, number and special character"
    )
    private String password;

    private String confirmPassword;
}