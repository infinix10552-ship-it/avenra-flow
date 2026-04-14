package org.devx.automatedinvoicesystem.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CurrencyConversionService — Live exchange rate fetcher.
 * Uses the free exchangerate-api.com public endpoint (no API key required).
 * Includes an in-memory cache (TTL: 1 hour) to avoid hammering the API.
 */
@Service
public class CurrencyConversionService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String API_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static final long CACHE_TTL_MS = 60 * 60 * 1000; // 1 hour

    // Simple cache: currency -> { rate, timestamp }
    private final ConcurrentHashMap<String, CachedRate> rateCache = new ConcurrentHashMap<>();

    /**
     * Get the live exchange rate from a given currency to INR.
     * Returns BigDecimal.ONE if the source currency is already INR or if the lookup fails.
     */
    public BigDecimal getExchangeRateToINR(String sourceCurrency) {
        if (sourceCurrency == null || sourceCurrency.equalsIgnoreCase("INR")) {
            return BigDecimal.ONE;
        }

        String key = sourceCurrency.toUpperCase();

        // Check cache first
        CachedRate cached = rateCache.get(key);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL_MS) {
            System.out.println("💱 [FOREX] Cache hit for " + key + " → INR: " + cached.rate);
            return cached.rate;
        }

        // Fetch live rate
        try {
            String url = API_URL + key;
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                Object inrRateObj = rates.get("INR");

                if (inrRateObj != null) {
                    BigDecimal rate = new BigDecimal(inrRateObj.toString()).setScale(6, RoundingMode.HALF_UP);
                    rateCache.put(key, new CachedRate(rate, System.currentTimeMillis()));
                    System.out.println("💱 [FOREX] Live rate fetched: 1 " + key + " = " + rate + " INR");
                    return rate;
                }
            }

            System.err.println("❌ [FOREX] No INR rate found in response for " + key);
        } catch (Exception e) {
            System.err.println("❌ [FOREX] Failed to fetch live rate for " + key + ": " + e.getMessage());
        }

        // Fallback: return ONE (no conversion applied, prevents data corruption)
        return BigDecimal.ONE;
    }

    /**
     * Convert an amount from source currency to INR using the live exchange rate.
     */
    public BigDecimal convertToINR(BigDecimal amount, String sourceCurrency) {
        if (amount == null) return BigDecimal.ZERO;
        BigDecimal rate = getExchangeRateToINR(sourceCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private static class CachedRate {
        final BigDecimal rate;
        final long timestamp;

        CachedRate(BigDecimal rate, long timestamp) {
            this.rate = rate;
            this.timestamp = timestamp;
        }
    }
}
