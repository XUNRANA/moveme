package com.moveme.common.util;

import org.springframework.util.StringUtils;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class TextEncodingRepairUtil {

    private static final int MAX_REPAIR_PASSES = 2;
    private static final Map<Character, Byte> CP1252_EXTENDED_BYTES = Map.ofEntries(
            Map.entry('€', (byte) 0x80),
            Map.entry('‚', (byte) 0x82),
            Map.entry('ƒ', (byte) 0x83),
            Map.entry('„', (byte) 0x84),
            Map.entry('…', (byte) 0x85),
            Map.entry('†', (byte) 0x86),
            Map.entry('‡', (byte) 0x87),
            Map.entry('ˆ', (byte) 0x88),
            Map.entry('‰', (byte) 0x89),
            Map.entry('Š', (byte) 0x8A),
            Map.entry('‹', (byte) 0x8B),
            Map.entry('Œ', (byte) 0x8C),
            Map.entry('Ž', (byte) 0x8E),
            Map.entry('‘', (byte) 0x91),
            Map.entry('’', (byte) 0x92),
            Map.entry('“', (byte) 0x93),
            Map.entry('”', (byte) 0x94),
            Map.entry('•', (byte) 0x95),
            Map.entry('–', (byte) 0x96),
            Map.entry('—', (byte) 0x97),
            Map.entry('˜', (byte) 0x98),
            Map.entry('™', (byte) 0x99),
            Map.entry('š', (byte) 0x9A),
            Map.entry('›', (byte) 0x9B),
            Map.entry('œ', (byte) 0x9C),
            Map.entry('ž', (byte) 0x9E),
            Map.entry('Ÿ', (byte) 0x9F)
    );

    private TextEncodingRepairUtil() {}

    public static String repairIfNeeded(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        String current = value;
        for (int i = 0; i < MAX_REPAIR_PASSES; i++) {
            String repaired = repairOnce(current);
            if (repaired == null || repaired.equals(current)) {
                break;
            }
            current = repaired;
        }
        return current;
    }

    private static String repairOnce(String value) {
        String repaired = decodeAsUtf8(value);
        if (repaired == null || repaired.equals(value)) {
            return value;
        }
        return readabilityScore(repaired) > readabilityScore(value) ? repaired : value;
    }

    private static String decodeAsUtf8(String value) {
        try {
            return new String(toOriginalBytes(value), StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static byte[] toOriginalBytes(String value) throws CharacterCodingException {
        byte[] bytes = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch <= 0xFF) {
                bytes[i] = (byte) ch;
                continue;
            }

            Byte mapped = CP1252_EXTENDED_BYTES.get(ch);
            if (mapped == null) {
                throw new CharacterCodingException();
            }
            bytes[i] = mapped;
        }
        return bytes;
    }

    private static int readabilityScore(String value) {
        int score = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (isReadableEastAsian(ch)) {
                score += 4;
                continue;
            }
            if (Character.isLetterOrDigit(ch)) {
                score += 1;
                continue;
            }
            if (Character.isWhitespace(ch) || isCommonPunctuation(ch)) {
                continue;
            }
            if (isSuspiciousMojibakeChar(ch)) {
                score -= 2;
                continue;
            }
            score -= 1;
        }
        return score;
    }

    private static boolean isReadableEastAsian(char ch) {
        return (ch >= '\u3400' && ch <= '\u9FFF')
                || (ch >= '\u3040' && ch <= '\u30FF')
                || (ch >= '\uAC00' && ch <= '\uD7AF')
                || (ch >= '\uFF00' && ch <= '\uFFEF');
    }

    private static boolean isCommonPunctuation(char ch) {
        return ".,!?;:'\"()[]{}<>/@#&+-_=~`|*%$^，。！？；：、“”‘’（）《》【】·…".indexOf(ch) >= 0;
    }

    private static boolean isSuspiciousMojibakeChar(char ch) {
        return (ch >= '\u00C0' && ch <= '\u00FF')
                || (ch >= '\u0080' && ch <= '\u009F')
                || "ŒœŠšŸŽž€‚„…†‡‰‹›™".indexOf(ch) >= 0;
    }
}
