package com.example

import com.example.domain.CodeExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeExtractorTest {

    @Test
    fun extractsContiguous15And16DigitCodesFromText() {
        assertEquals(
            "123456789012345",
            CodeExtractor.extractFirstCode("Your verification code is 123456789012345. Do not share.")
        )
        assertEquals(
            "9876543210987654",
            CodeExtractor.extractFirstCode("Use 9876543210987654 now!")
        )
    }

    @Test
    fun extractsCodesWithSpacesAndDashes() {
        assertEquals(
            "1234567890123456",
            CodeExtractor.extractFirstCode("Code: 1234-5678-9012-3456 valid for 5 mins")
        )
        assertEquals(
            "123456789012345",
            CodeExtractor.extractFirstCode("PIN is 123 456 789 012 345 thanks")
        )
        assertEquals(
            "1234567890123456",
            CodeExtractor.extractFirstCode("Ref: 1234 - 5678 - 9012 - 3456")
        )
    }

    @Test
    fun ignoresTooShortOrTooLongDigitSequences() {
        assertNull(CodeExtractor.extractFirstCode("Short 12345678901234 (14 digits)"))
        assertNull(CodeExtractor.extractFirstCode("Long 12345678901234567 (17 digits)"))
        assertNull(CodeExtractor.extractFirstCode("Dashed long 1234-5678-9012-3456-78"))
        assertNull(CodeExtractor.extractFirstCode("Phone +1 234 567 8901"))
    }

    @Test
    fun extractsCodeEvenWhenPrecededByDate() {
        assertEquals(
            "1234567890123456",
            CodeExtractor.extractFirstCode("2026-11-18 1234-5678-9012-3456")
        )
    }
}
