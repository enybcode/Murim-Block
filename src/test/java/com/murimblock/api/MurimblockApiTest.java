package com.murimblock.api;

import com.murimblock.api.cultivation.CultivationApi;
import com.murimblock.api.qi.QiApi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MurimblockApiTest {
    @Test
    void publicApiExposesStableSingletonFacades() {
        QiApi qi = MurimblockApi.qi();
        CultivationApi cultivation = MurimblockApi.cultivation();

        assertNotNull(qi);
        assertNotNull(cultivation);
        assertSame(qi, MurimblockApi.qi());
        assertSame(cultivation, MurimblockApi.cultivation());
    }
}
