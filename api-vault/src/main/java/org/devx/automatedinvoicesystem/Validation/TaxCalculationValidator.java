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
        if (baseTaxableAmount == null || totalAmount == null) {
            return false;
        }

        BigDecimal b = baseTaxableAmount;
        BigDecimal c = cgst != null ? cgst : BigDecimal.ZERO;
        BigDecimal s = sgst != null ? sgst : BigDecimal.ZERO;
        BigDecimal i = igst != null ? igst : BigDecimal.ZERO;

        BigDecimal computedTotal = b.add(c).add(s).add(i);

        return computedTotal.compareTo(totalAmount) == 0;
    }

    public static BigDecimal getMismatchAmount(BigDecimal baseTaxableAmount, BigDecimal cgst,
                                                BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount) {
        if (baseTaxableAmount == null || totalAmount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal b = baseTaxableAmount;
        BigDecimal c = cgst != null ? cgst : BigDecimal.ZERO;
        BigDecimal s = sgst != null ? sgst : BigDecimal.ZERO;
        BigDecimal i = igst != null ? igst : BigDecimal.ZERO;

        BigDecimal computedTotal = b.add(c).add(s).add(i);

        return computedTotal.subtract(totalAmount);
    }
}
