package net.engineerAnsh.BankApplication.Validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import net.engineerAnsh.BankApplication.Dto.Auth.SignupRequest;

// This validator will validate SignupRequest when @PasswordMatches is used...
public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, SignupRequest> { // 'PasswordMatches' → The annotation, 'SignupRequest' → The type being validated

    // This method runs automatically during validation.
    @Override
    public boolean isValid(SignupRequest request,
                           ConstraintValidatorContext context) {

        // If either is null → invalid...
        if (request.getPassword() == null ||
                request.getConfirmPassword() == null) {
            return false;
        }

        // If both match → true, If not → false.
        return request.getPassword().equals(request.getConfirmPassword());
    }
}