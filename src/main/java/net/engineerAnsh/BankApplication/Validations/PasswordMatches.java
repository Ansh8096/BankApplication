package net.engineerAnsh.BankApplication.Validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE}) // Means: this annotation can be applied only on class levels
@Retention(RetentionPolicy.RUNTIME) // Makes this annotation available at runtime
@Constraint(validatedBy = PasswordMatchesValidator.class) // It tells Spring: “When you see @PasswordMatches, use PasswordMatchesValidator to validate it.”
@Documented // Makes the annotation appear in JavaDoc
public @interface PasswordMatches { // @PasswordMatches -> It is a custom annotation that we created...

    // Required Methods Inside Custom Constraint:
    String message() default "Passwords do not match"; // Error message...
    Class<?>[] groups() default {}; // Used for grouped validation...
    Class<? extends Payload>[] payload() default {}; // For custom metadata...

}
