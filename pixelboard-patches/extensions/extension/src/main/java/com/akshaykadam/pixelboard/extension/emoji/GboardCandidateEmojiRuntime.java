package com.akshaykadam.pixelboard.extension.emoji;

import android.text.TextPaint;
import android.widget.TextView;

/**
 * Runtime helper for candidate strip emoji rendering.
 *
 * Ensures emoji glyphs in suggestion candidates render at full opacity (alpha 255)
 * instead of adopting secondary candidate translucent text styles that make color
 * emoji bitmap glyphs appear dull and washed out.
 */
public final class GboardCandidateEmojiRuntime {

    private GboardCandidateEmojiRuntime() {
    }

    /**
     * Checks if the character sequence contains one or more emoji characters.
     */
    public static boolean containsEmoji(CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        int len = cs.length();
        for (int i = 0; i < len; i++) {
            char c = cs.charAt(i);
            // High/low surrogate pairs (SMP Plane 1: modern emojis U+1F000 - U+1FAFF)
            if (Character.isSurrogate(c)) {
                return true;
            }
            // BMP emoji and symbol blocks (Dingbats, Misc Symbols, Arrows, Enclosed)
            if (c >= 0x203C && c <= 0x3299) {
                return true;
            }
            // Variation Selector-16 (emoji presentation selector)
            if (c == 0xFE0F) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adjusts the candidate TextView TextPaint to full opacity if the candidate text contains emojis.
     */
    public static void adjustCandidatePaint(TextView textView, TextPaint paint) {
        if (textView == null || paint == null) {
            return;
        }
        CharSequence text = textView.getText();
        if (containsEmoji(text)) {
            paint.setColor(paint.getColor() | 0xFF000000);
            paint.setAlpha(255);
        }
    }
}
