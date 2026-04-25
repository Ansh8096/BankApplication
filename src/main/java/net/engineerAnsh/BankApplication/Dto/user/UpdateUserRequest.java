package net.engineerAnsh.BankApplication.Dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    private String name;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(
            regexp = "^(\\+91)?[6-9][0-9]{9}$",
            message = "Invalid Indian phone number"
    )
    private String phoneNumber;

    @Min(value = 12, message = "You must be at least 12 years old")
    @Max(value = 120, message = "Invalid age")
    private Integer age;

}
