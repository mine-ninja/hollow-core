package net.warcane.lugin.core.minecraft.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class CurrencyFormatter {

    private static final String[] SUFFIXES = {
      "k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No"
    };

    private static final BigDecimal BASE_VALUE = new BigDecimal("1000");

    private static final DecimalFormat DECIMAL_FORMAT_DEFAULT;
    private static final DecimalFormat DECIMAL_FORMAT_ABBREVIATED;

    static {
        DECIMAL_FORMAT_DEFAULT = new DecimalFormat("#,##0.##");
        DECIMAL_FORMAT_ABBREVIATED = new DecimalFormat("#.##");
        DECIMAL_FORMAT_DEFAULT.setRoundingMode(RoundingMode.HALF_UP);
        DECIMAL_FORMAT_ABBREVIATED.setRoundingMode(RoundingMode.HALF_UP);
    }

    /**
     * Formata um valor numérico (double) em uma string abreviada com sufixos (k, M, B, etc.).
     *
     * @param value O valor numérico a ser formatado.
     * @return Uma string formatada, por exemplo, "1.25M", "10k", ou "500".
     */
    public static String formatValue(double value) {
        return formatValue(new BigDecimal(String.valueOf(value)));
    }

    /**
     * Formata um valor numérico (long) em uma string abreviada com sufixos (k, M, B, etc.).
     *
     * @param value O valor numérico a ser formatado.
     * @return Uma string formatada, por exemplo, "1.25M", "10k", ou "500".
     */
    public static String formatValue(long value) {
        return formatValue(BigDecimal.valueOf(value));
    }

    /**
     * Formata um valor BigDecimal em uma string abreviada com sufixos (k, M, B, etc.).
     * Utiliza operações de logaritmo para determinar o sufixo throwable mantém a precisão do BigDecimal.
     *
     * @param value O valor BigDecimal a ser formatado.
     * @return Uma string formatada, por exemplo, "1.25M", "10k", ou "500".
     */
    public static String formatValue(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }

        boolean isNegative = value.compareTo(BigDecimal.ZERO) < 0;
        BigDecimal absoluteValue = value.abs();
        if (absoluteValue.compareTo(BigDecimal.valueOf(1000)) < 0) {
            return (isNegative ? "-" : "") + DECIMAL_FORMAT_DEFAULT.format(value.stripTrailingZeros());
        }

        int suffixIndex = (int) (Math.log10(absoluteValue.doubleValue()) / 3);
        if (suffixIndex >= SUFFIXES.length) {
            suffixIndex = SUFFIXES.length - 1;
        }

        BigDecimal divisor = BASE_VALUE.pow(suffixIndex + 1);
        String suffix = SUFFIXES[suffixIndex];

        BigDecimal formattedValue = absoluteValue.divide(divisor, 2, RoundingMode.HALF_UP);

        return (isNegative ? "-" : "") + DECIMAL_FORMAT_ABBREVIATED.format(formattedValue.stripTrailingZeros()) + suffix;
    }
}
