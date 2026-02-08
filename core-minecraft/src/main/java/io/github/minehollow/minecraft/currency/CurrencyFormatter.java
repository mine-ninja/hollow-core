package io.github.minehollow.minecraft.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyFormatter {

    // Expandido para suportar até 10^90 (Novemvigintillion)
    private static final String[] SUFFIXES = {
      "", "k", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc",
      "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod", "Vg",
      "Uvg", "Dvg", "Tvg", "Qavg", "Qivg", "Sxvg", "Spvg", "Ocvg", "Novg"
    };

    /**
     * Otimização Extrema: O(1) Magnitude Discovery.
     * Sem locks, sem DecimalFormat (que não é thread-safe).
     */
    public static String formatValue(BigDecimal value) {
        if (value == null || value.signum() == 0) return "0";

        // 1. Lógica de Sinal (Fast path)
        int signum = value.signum();
        if (signum == -1) value = value.abs();

        // 2. Cálculo de Magnitude via Metadados (Sem log10 lento)
        // precision - scale nos dá quantos dígitos existem antes da vírgula
        long digitsBeforePoint = value.precision() - value.scale();
        int suffixIndex = (int) ((digitsBeforePoint - 1) / 3);

        // Se for menor que 1000, formata simples e retorna
        if (suffixIndex <= 0) {
            String res = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            return signum == -1 ? "-" + res : res;
        }

        // Limita ao máximo de sufixos disponíveis
        if (suffixIndex >= SUFFIXES.length) suffixIndex = SUFFIXES.length - 1;

        // 3. Redução de Escala Virtual (Custo quase zero)
        // movePointLeft é ordens de magnitude mais rápido que .divide()
        BigDecimal shortValue = value.movePointLeft(suffixIndex * 3)
          .setScale(2, RoundingMode.HALF_UP)
          .stripTrailingZeros();

        String base = shortValue.toPlainString();
        String suffix = SUFFIXES[suffixIndex];

        // 4. StringBuilder com capacidade exata (Evita redimensionamento de array interno)
        StringBuilder sb = new StringBuilder(base.length() + suffix.length() + 1);
        if (signum == -1) sb.append('-');
        return sb.append(base).append(suffix).toString();
    }

    public static String formatValue(double value) {
        // Otimização: BigDecimal.valueOf(double) é mais rápido que new BigDecimal(String)
        return formatValue(BigDecimal.valueOf(value));
    }

    public static String formatValue(long value) {
        return formatValue(BigDecimal.valueOf(value));
    }
}