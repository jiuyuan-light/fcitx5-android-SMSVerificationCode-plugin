package org.fcitx.fcitx5.android.plugin.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpParserTest {
    @Test
    fun parseKeywords_splitsAndTrims() {
        val raw = "验证码, 动态码;  口令\n验证码"
        val result = parseKeywords(raw)
        assertEquals(listOf("验证码", "动态码", "口令"), result)
    }

    @Test
    fun pickOtp_prefersKeywordProximity() {
        val text = "备用码 1111，您的验证码是 222222"
        val code = pickOtp(text, listOf("验证码"))
        assertEquals("222222", code)
    }

    @Test
    fun pickOtp_fallsBackToPreferredLength() {
        val text = "代码 1111 与 333333"
        val code = pickOtp(text, emptyList())
        assertEquals("333333", code)
    }

    @Test
    fun pickOtp_handlesTypicalSmsBody() {
        val text = "您的验证码是 246810，请勿泄露。"
        val code = pickOtp(text, listOf("验证码"))
        assertEquals("246810", code)
    }
}
