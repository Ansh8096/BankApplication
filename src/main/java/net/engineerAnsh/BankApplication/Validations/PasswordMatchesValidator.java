package net.engineerAnsh.BankApplication.Validations;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// This validator will validate SignupRequest when @PasswordMatches is used...
public class PasswordMatchesValidator
        implements ConstraintValidator<PasswordMatches, PasswordMatchable> { // 'PasswordMatches' → The annotation, 'SignupRequest' → The type being validated

    // This method runs automatically during validation.
    @Override
    public boolean isValid(PasswordMatchable request,
                           ConstraintValidatorContext context) {

        // If either is null → invalid...
        if (request == null) return true;

        // If both match → true, If not → false.
        String password = request.getPassword();
        String confirm = request.getConfirmPassword();

        if(password ==null||confirm ==null){
            return false;
        }

        return password.equals(confirm);
}
}