package com.murimblock.integration.epicfight;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EpicFightSkillServiceTest {
    @Test
    void acceptsOnlyRegisteredSlotsAndActualHandBookAddresses() {
        assertTrue(EpicFightSkillService.validIndices(0, 24, -1));
        assertTrue(EpicFightSkillService.validIndices(23, 24, 8));
        assertTrue(EpicFightSkillService.validIndices(1, 24, 40));
        for (int index : new int[]{-1, 24, Integer.MAX_VALUE}) assertFalse(EpicFightSkillService.validIndices(index, 24, -1));
        for (int book : new int[]{-2, 9, 36, 39, 41}) assertFalse(EpicFightSkillService.validIndices(0, 24, book));
    }
}
