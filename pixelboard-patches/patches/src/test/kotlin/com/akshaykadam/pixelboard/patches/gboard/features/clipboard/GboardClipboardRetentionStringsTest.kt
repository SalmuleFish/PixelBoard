package com.akshaykadam.pixelboard.patches.gboard.features.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

class GboardClipboardRetentionStringsTest {
    @Test
    fun `pin tip keeps only the first sentence in every script`() {
        val cases = mapOf(
            "Touch and hold a clip to pin it. Unpinned clips will be deleted after 1 hour." to
                "Touch and hold a clip to pin it.",
            "Appuyez de manière prolongée sur un élément pour l'épingler. Les éléments retirés seront supprimés au bout d'une heure." to
                "Appuyez de manière prolongée sur un élément pour l'épingler.",
            "クリップを長押しするとクリップが固定されます。固定を解除したクリップは 1 時間後に削除されます。" to
                "クリップを長押しするとクリップが固定されます。",
            "किसी टेक्स्ट को पिन करने के लिए, उसे दबाकर रखें. अनपिन किए गए टेक्स्ट एक घंटे बाद मिटा दिए जाएंगे." to
                "किसी टेक्स्ट को पिन करने के लिए, उसे दबाकर रखें.",
            "क्लिपलाई पिन गर्न त्यसलाई टच एण्ड होल्ड गर्नुहोस्। पिन नगरिएका क्लिपहरू १ घन्टापछि मेटाइने छन्।" to
                "क्लिपलाई पिन गर्न त्यसलाई टच एण्ड होल्ड गर्नुहोस्।",
            "ለመሰካት አንድ ቅንጥብ ነክተው ይያዙ። ያልተሰኩ ቅንጥቦች ከ1 ሰዓት በኋላ ይሰረዛሉ።" to
                "ለመሰካት አንድ ቅንጥብ ነክተው ይያዙ።",
            "Հպեք հոլովակին և պահեք՝ այն ամրացնելու համար։ Ապամրացված հոլովակները կջնջվեն 1 ժամից:" to
                "Հպեք հոլովակին և պահեք՝ այն ամրացնելու համար։",
            "اسے پن کرنے کے لیے کلپ کو ٹچ کر کے دبائے رکھیں۔ 1 گھنٹے کے بعد پن ہٹائی گئی کلپس کو حذف کر دیا جائے گا۔" to
                "اسے پن کرنے کے لیے کلپ کو ٹچ کر کے دبائے رکھیں۔",
            "ចុច​លើ​ផ្នែកដក​ចេញ​ឱ្យជាប់ ដើម្បីខ្ទាស់​វា។ ផ្នែក​ដកចេញ​ដែលបាន​ដកខ្ទាស់​នឹង​ត្រូវ​លុប បន្ទាប់ពីរយៈពេល​ 1 ម៉ោង។" to
                "ចុច​លើ​ផ្នែកដក​ចេញ​ឱ្យជាប់ ដើម្បីខ្ទាស់​វា។",
            "ဗီဒီယိုအပိုင်းကို ပင်ထိုးရန် ဖိထားပါ။ ပင်ထိုးမထားသည့် ဗီဒီယိုပိုင်းများကို ၁ နာရီကြာလျှင် ဖျက်လိုက်ပါမည်။" to
                "ဗီဒီယိုအပိုင်းကို ပင်ထိုးရန် ဖိထားပါ။",
            "แตะคลิปค้างไว้เพื่อปักหมุด ระบบจะลบคลิปที่เลิกปักหมุดหลังจากผ่านไป 1 ชั่วโมง" to
                "แตะคลิปค้างไว้เพื่อปักหมุด",
        )
        for ((stock, expected) in cases) {
            assertEquals(expected, firstSentence(stock))
            assertEquals(expected, firstSentence(expected))
        }
    }

    @Test
    fun `short pin tip is added next to Google's tip without touching other strings`() {
        val stock = """
            <resources>
              <string name="string_0x7f140192">Gboard's clipboard lets you copy text and images, keeping them for 1 hour, to quickly paste many things at once.</string>
              <string name="string_0x7f1401a6">Appuyez de manière prolongée sur un élément pour l\'épingler. Les éléments retirés seront supprimés au bout d\'une heure.</string>
            </resources>
        """.trimIndent()

        val patched = addShortPinTip(stock)

        assertEquals(
            stock.removeSuffix("</resources>") +
                """    <string name="pixelboard_clipboard_pin_tip_short">""" +
                """Appuyez de manière prolongée sur un élément pour l\'épingler.</string>""" + "\n" +
                "</resources>",
            patched,
        )
        assertEquals(patched, addShortPinTip(patched))
        assertEquals("<resources>\n</resources>", addShortPinTip("<resources>\n</resources>"))
    }
}
