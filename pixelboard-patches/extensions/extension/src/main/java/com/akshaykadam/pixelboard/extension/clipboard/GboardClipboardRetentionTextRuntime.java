package com.akshaykadam.pixelboard.extension.clipboard;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

/**
 * Keeps Gboard's clipboard texts honest about how long clips last.
 *
 * <p>Google's texts say clips are deleted after 1 hour, so they are shown unchanged only while
 * the retention is 1 hour. At any other retention, including PixelBoard's 24-hour default, each
 * text is swapped for Google's own translated wording that names no duration.</p>
 */
public final class GboardClipboardRetentionTextRuntime {
    // Gboard 18.5.3 resource ids.
    /** "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour." */
    static final int PIN_TIP = 0x7f1401a6;
    /** "Gboard's clipboard lets you copy text and images, keeping them for 1 hour, ..." */
    static final int INTRO_CARD = 0x7f140192;
    /** "No items copied within the last hour" */
    static final int EMPTY_SUBTITLE = 0x7f14019e;
    /** "Clipboard allows you to access your recent copy and paste history" */
    static final int DESCRIPTION = 0x7f1401a1;
    /** The intro card's body TextView in layouts 0x7f0e0061 and 0x7f0e0062. */
    static final int INTRO_CARD_BODY_ID = 0x7f0b01ac;
    /** "Touch and hold a clip to pin it.", added to every locale by the resource patch. */
    static final String PIN_TIP_SHORT_NAME = "pixelboard_clipboard_pin_tip_short";

    private GboardClipboardRetentionTextRuntime() {
    }

    /** Returns the string to show in place of one of Gboard's clipboard strings. */
    public static int resolveText(int resId) {
        try {
            Context context = GboardClipboardHistorySettings.applicationContext();
            if (context == null || GboardClipboardHistorySettings.isOneHourRetention(context)) {
                return resId;
            }
            return replacementFor(context, resId);
        } catch (Throwable ignored) {
            return resId;
        }
    }

    /** Updates the "Copy more for faster pasting" card after Gboard inflates it. */
    public static void applyIntroCardText(View card) {
        try {
            if (card == null
                    || GboardClipboardHistorySettings.isOneHourRetention(card.getContext())) {
                return;
            }
            View body = card.findViewById(INTRO_CARD_BODY_ID);
            if (body instanceof TextView) {
                ((TextView) body).setText(DESCRIPTION);
            }
        } catch (Throwable ignored) {
        }
    }

    static int replacementFor(Context context, int resId) {
        if (resId == PIN_TIP) {
            int shortTip = context.getResources().getIdentifier(
                    PIN_TIP_SHORT_NAME, "string", context.getPackageName());
            return shortTip != 0 ? shortTip : resId;
        }
        if (resId == INTRO_CARD || resId == EMPTY_SUBTITLE) {
            return DESCRIPTION;
        }
        return resId;
    }
}
