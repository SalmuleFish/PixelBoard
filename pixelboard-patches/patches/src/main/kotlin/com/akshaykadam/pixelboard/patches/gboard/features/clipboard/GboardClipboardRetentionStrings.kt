package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

// Google's pin tip says unpinned clips are deleted after 1 hour, which is only true at the
// default retention. Add the tip's first sentence ("Touch and hold a clip to pin it.") to every
// locale so GboardClipboardRetentionTextRuntime can show it when the retention is changed.
private const val PIN_TIP = "string_0x7f1401a6" // "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour."
internal const val PIN_TIP_SHORT = "pixelboard_clipboard_pin_tip_short"

private val SENTENCE_ENDS = listOf("。", "।", "။", "።", "։", "۔", "។")
private val PERIOD_BEFORE_SPACE = Regex("""\.(?=\s)""")
private val PIN_TIP_REGEX = Regex("""<string name="$PIN_TIP">(.*?)</string>""")

/** Adds the short pin tip to one strings.xml, leaving everything else untouched. */
internal fun addShortPinTip(xml: String): String {
    if (xml.contains("name=\"$PIN_TIP_SHORT\"")) return xml
    val tip = PIN_TIP_REGEX.find(xml)?.groupValues?.get(1) ?: return xml
    val end = xml.lastIndexOf("</resources>")
    if (end < 0) return xml
    return xml.substring(0, end) +
        "    <string name=\"$PIN_TIP_SHORT\">${firstSentence(tip)}</string>\n" +
        xml.substring(end)
}

/** Keeps the first sentence ("Touch and hold a clip to pin it.") of a two-sentence string. */
internal fun firstSentence(value: String): String {
    val ends = SENTENCE_ENDS.mapNotNull { end ->
        value.indexOf(end).takeIf { it >= 0 }?.plus(end.length)
    } + listOfNotNull(PERIOD_BEFORE_SPACE.find(value)?.range?.last?.plus(1))
    val end = ends.minOrNull()
    if (end != null) return value.substring(0, end)
    if (value.endsWith(".") || SENTENCE_ENDS.any(value::endsWith)) return value
    // Thai separates sentences with a space instead of punctuation.
    val space = value.indexOf(' ')
    return if (space > 0) value.substring(0, space) else value
}
