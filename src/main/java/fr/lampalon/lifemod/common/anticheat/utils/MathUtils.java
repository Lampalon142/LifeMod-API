package fr.lampalon.lifemod.common.anticheat.utils;

import java.util.Collection;

/**
 * Utility class for statistical and mathematical calculations used by the AntiCheat module.
 */
public final class MathUtils {

    private MathUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculates the variance of a collection of numbers.
     *
     * @param samples The numeric samples.
     * @return The variance.
     */
    public static double getVariance(Collection<? extends Number> samples) {
        if (samples.isEmpty()) return 0;
        double average = getAverage(samples);
        double temp = 0;
        for (Number a : samples) {
            temp += Math.pow(a.doubleValue() - average, 2);
        }
        return temp / samples.size();
    }

    /**
     * Calculates the standard deviation (sigma) of a collection of numbers.
     *
     * @param samples The numeric samples.
     * @return The standard deviation.
     */
    public static double getStandardDeviation(Collection<? extends Number> samples) {
        return Math.sqrt(getVariance(samples));
    }

    /**
     * Calculates the average of a collection of numbers.
     *
     * @param samples The numeric samples.
     * @return The average.
     */
    public static double getAverage(Collection<? extends Number> samples) {
        if (samples.isEmpty()) return 0;
        return samples.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
    }
    
    /**
     * Calculates the entropy of a collection of numeric samples.
     * High entropy means high chaos (human-like).
     *
     * @param samples The numeric samples.
     * @return The entropy value.
     */
    public static double getEntropy(Collection<? extends Number> samples) {
        if (samples.size() < 2) return 0;
        double entropy = 0;
        int size = samples.size();
        java.util.Map<Double, Integer> frequencies = new java.util.HashMap<>();
        for (Number n : samples) {
            frequencies.merge(n.doubleValue(), 1, Integer::sum);
        }
        for (int count : frequencies.values()) {
            double p = (double) count / size;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }
}
