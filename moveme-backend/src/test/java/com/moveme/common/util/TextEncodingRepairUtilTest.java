package com.moveme.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextEncodingRepairUtilTest {

    @Test
    void shouldRepairMojibakeGenre() {
        assertEquals("冒险", TextEncodingRepairUtil.repairIfNeeded("å†’é™©"));
    }

    @Test
    void shouldRepairMojibakeMovieTitle() {
        assertEquals("万物生灵：2025圣诞特别集",
                TextEncodingRepairUtil.repairIfNeeded("ä¸ç©ççµï¼2025å£è¯ç¹å«é"));
    }

    @Test
    void shouldKeepReadableTextUntouched() {
        assertEquals("动作", TextEncodingRepairUtil.repairIfNeeded("动作"));
        assertEquals("MovieMe", TextEncodingRepairUtil.repairIfNeeded("MovieMe"));
    }

    @Test
    void shouldRepairControlAndCp1252MixedGenres() {
        assertEquals("古装", TextEncodingRepairUtil.repairIfNeeded("\u00e5\u008f\u00a4\u00e8\u00a3\u2026"));
        assertEquals("历史", TextEncodingRepairUtil.repairIfNeeded("\u00e5\u017d\u2020\u00e5\u008f\u00b2"));
        assertEquals("恐怖", TextEncodingRepairUtil.repairIfNeeded("\u00e6\u0081\u0090\u00e6\u20ac\u2013"));
        assertEquals("音乐", TextEncodingRepairUtil.repairIfNeeded("\u00e9\u0178\u00b3\u00e4\u00b9\u0090"));
    }
}
