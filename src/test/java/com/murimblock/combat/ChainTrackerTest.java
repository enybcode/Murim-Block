package com.murimblock.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChainTrackerTest {
    private static final int WINDOW = 10;
    private static final int SIZE = 4;

    @Test
    void goesThroughTheChainThenStartsOver() {
        ChainTracker chain = new ChainTracker();
        for (int expected : new int[] {0, 1, 2, 3, 0}) {
            int index = chain.next(WINDOW, SIZE);
            assertEquals(expected, index);
            chain.thrown(index);
            chain.tick(true);
            chain.tick(false);
        }
    }

    @Test
    void aLongPauseStartsOver() {
        ChainTracker chain = new ChainTracker();
        chain.thrown(0);
        for (int i = 0; i < WINDOW; i++) {
            chain.tick(false);
        }
        assertEquals(1, chain.next(WINDOW, SIZE), "still within the window");
        chain.tick(false);
        assertEquals(0, chain.next(WINDOW, SIZE), "one tick too late");
    }

    @Test
    void timeStrikingDoesNotCount() {
        ChainTracker chain = new ChainTracker();
        chain.thrown(1);
        for (int i = 0; i < 50; i++) {
            chain.tick(true);
        }
        assertEquals(2, chain.next(WINDOW, SIZE));
    }

    @Test
    void resetStartsOver() {
        ChainTracker chain = new ChainTracker();
        chain.thrown(2);
        chain.reset();
        assertEquals(0, chain.next(WINDOW, SIZE));
    }
}
