package org.example.quoraproject.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    /**
     * 8-72 chars (72 is the effective BCrypt input limit), at least one
     * lowercase, one uppercase, one digit, one special character.
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,72}$");

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // @NotBlank handles null/empty; avoid duplicate error messages here.
        if (password == null || password.isBlank()) {
            return true;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
