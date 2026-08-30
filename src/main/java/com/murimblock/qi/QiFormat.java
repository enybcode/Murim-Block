package com.murimblock.qi;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class QiFormat {
    private QiFormat() {
    }

    public static String format(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
