package fr.lampalon.lifemod.common.anticheat.utils;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for statistical and mathematical calculations used by the AntiCheat module.
 * All methods are thread-safe by taking a snapshot of the data.
 */
public final class MathUtils {

    private MathUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Calculates the variance of a collection of numbers.
     */
    public static double getVariance(Collection<? extends Number> samples) {
        List<Number> copy = new ArrayList<>(samples);
        if (copy.isEmpty()) return 0;
        double average = getAverage(copy);
        double temp = 0;
        for (Number a : copy) {
            temp += Math.pow(a.doubleValue() - average, 2);
        }
        return temp / copy.size();
    }

    /**
     * Calculates the standard deviation (sigma) of a collection of numbers.
     */
    public static double getStandardDeviation(Collection<? extends Number> samples) {
        return Math.sqrt(getVariance(samples));
    }

    /**
     * Calculates the average of a collection of numbers.
     */
    public static double getAverage(Collection<? extends Number> samples) {
        List<Number> copy = new ArrayList<>(samples);
        if (copy.isEmpty()) return 0;
        return copy.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
    }
    
    /**
     * Calculates the entropy of a collection of numeric samples.
     * High entropy means high chaos (human-like).
     */
    public static double getEntropy(Collection<? extends Number> samples) {
        List<Number> copy = new ArrayList<>(samples);
        if (copy.size() < 2) return 0;
        double entropy = 0;
        int size = copy.size();
        java.util.Map<Double, Integer> frequencies = new java.util.HashMap<>();
        for (Number n : copy) {
            frequencies.merge(n.doubleValue(), 1, Integer::sum);
        }
        for (int count : frequencies.values()) {
            double p = (double) count / size;
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /**
     * Calculates the GCD of two double values.
     */
    public static double getGcd(double a, double b) {
        if (a < b) return getGcd(b, a);
        if (Math.abs(b) < 0.001) return a;
        return getGcd(b, a - Math.floor(a / b) * b);
    }
}
