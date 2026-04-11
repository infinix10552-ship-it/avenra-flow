package org.devx.automatedinvoicesystem.Validation;

import java.util.regex.Pattern;

/**
 * Deterministic GSTIN validation utility.
 * GSTIN format: 2-digit state code + 10-char PAN + 1-char entity code + 'Z' + 1-char checksum.
 * Example: 27AAPFU0939F1ZV
 */
public final class GstinValidator {

    private static final Pattern GSTIN_PATTERN = Pattern.compile(
            "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
    );

    private GstinValidator() {
        // Utility class — no instantiation
    }

    /**
     * Validates if the given string is a valid GSTIN format.
     *
     * @param gstin the GSTIN string to validate
     * @return true if the format is valid, false otherwise
     */
    public static boolean isValid(String gstin) {
        if (gstin == null || gstin.length() != 15) {
            return false;
        }
        return GSTIN_PATTERN.matcher(gstin.trim().toUpperCase()).matches();
    }

    /**
     * Validates the state code portion of a GSTIN (first 2 digits).
     * Valid Indian state codes range from 01 to 37 (plus special codes).
     */
    public static boolean hasValidStateCode(String gstin) {
        if (!isValid(gstin)) {
            return false;
        }
        int stateCode = Integer.parseInt(gstin.substring(0, 2));
        return stateCode >= 1 && stateCode <= 37;
    }
}
