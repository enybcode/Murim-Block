package com.murimblock.qi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QiFormatTest {
    @Test
    void removesUnnecessaryDecimals() {
        assertEquals("100", QiFormat.format(100.0));
        assertEquals("1500", QiFormat.format(1500.0));
    }

    @Test
    void keepsUsefulFractionalQiWithLimitedPrecision() {
        assertEquals("72.5", QiFormat.format(72.5));
        assertEquals("72.484", QiFormat.format(72.483726193));
    }
}
