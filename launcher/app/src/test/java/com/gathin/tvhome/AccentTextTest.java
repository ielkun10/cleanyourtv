package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AccentTextTest {
    @Test
    public void goldIsOpaque() {
        assertEquals(0xFFC9A961, AccentText.parse("#C9A961"));
        assertEquals(0xFFC9A961, AccentText.parse(" #c9a961 "));
    }

    @Test
    public void anythingElseKeepsTheLaunchersColour() {
        assertEquals(0, AccentText.parse(""));
        assertEquals(0, AccentText.parse(null));
        assertEquals(0, AccentText.parse("#80C9A961"));
        assertEquals(0, AccentText.parse("gold"));
        assertEquals(0, AccentText.parse("#C9A96"));
    }
}
