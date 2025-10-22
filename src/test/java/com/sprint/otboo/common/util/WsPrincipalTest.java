package com.sprint.otboo.common.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WsPrincipalTest {

    @Test
    void name_그대로반환() {
        WsPrincipal p = new WsPrincipal("abc");
        assertEquals("abc", p.getName());
    }
}
