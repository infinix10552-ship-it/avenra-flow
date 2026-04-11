package org.devx.automatedinvoicesystem.Validation;

import java.math.BigDecimal;

/**
 * Deterministic tax calculation validator.
 * Enforces: baseTaxableAmount + cgst + sgst + igst == totalAmount
 * Uses BigDecimal.compareTo() — NO floating point operations.
 */
public final class TaxCalculationValidator {

    private TaxCalculationValidator() {
        // Utility class — no instantiation
    }

    /**
     * Validates that the sum of base amount and all tax components equals the total.
     * All parameters must be non-null.
     *
     * @return true if the tax calculation is mathematically correct
     */
    public static boolean isValid(BigDecimal baseTaxableAmount, BigDecimal cgst,
                                   BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount) {
        if (baseTaxableAmount == null || cgst == null || sgst == null
                || igst == null || totalAmount == null) {
            return false;
        }

        BigDecimal computedTotal = baseTaxableAmount
                .add(cgst)
                .add(sgst)
                .add(igst);

        // compareTo() ignores scale differences (e.g., 100.00 == 100.0)
        return computedTotal.compareTo(totalAmount) == 0;
    }

    /**
     * Returns the mismatch amount (computed - declared).
     * Positive = over-declared, Negative = under-declared.
     */
    public static BigDecimal getMismatchAmount(BigDecimal baseTaxableAmount, BigDecimal cgst,
                                                BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount) {
        if (baseTaxableAmount == null || cgst == null || sgst == null
                || igst == null || totalAmount == null) {
            return null;
        }

        BigDecimal computedTotal = baseTaxableAmount
                .add(cgst)
                .add(sgst)
                .add(igst);

        return computedTotal.subtract(totalAmount);
    }
}
