package com.simats.genetics.utils

object PasswordValidator {

    /**
     * Validates a password against the backend requirements:
     * - Minimum 8 characters.
     * - At least one uppercase letter (A-Z).
     * - At least one lowercase letter (a-z).
     * - At least one number (0-9).
     * - At least one special character.
     * 
     * Returns a list of missing requirement descriptions. 
     * If the list is empty, the password is valid.
     */
    fun validate(password: String): List<String> {
        val missing = mutableListOf<String>()

        if (password.length < 8) {
            missing.add("At least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            missing.add("One uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            missing.add("One lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            missing.add("One number")
        }
        
        val specialChars = "!@#$%^&*()_-+=[{]};:'\",<.>/?\\|`~"
        if (!password.any { it in specialChars }) {
            missing.add("One special character")
        }

        return missing
    }
}
