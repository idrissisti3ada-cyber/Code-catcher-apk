package com.example.domain

import android.app.Notification
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat

object CodeExtractor {

    // Exact contiguous 15-16 digit number inside text
    private val contiguousRegex = Regex("""\b\d{15,16}\b""")

    // Dash-separated digit sequence (e.g. 1234-5678-9012-3456 or 123-456-789-012-345)
    private val dashedRegex = Regex("""(?<![\d\-\u2013\u2014])\d+(?:[\-\u2013\u2014]\d+)+(?![\d\-\u2013\u2014])""")

    // Space-separated digit sequence (e.g. 1234 5678 9012 3456 or 1234 5678 9012 345)
    private val spacedRegex = Regex("""(?<![\d \u00A0])\d+(?:[ \u00A0]\d+)+(?![\d \u00A0])""")

    // Mixed space/dash digit sequence (e.g. 1234 - 5678 - 9012 - 3456)
    private val mixedRegex = Regex(
        """(?<![\d\-\u2013\u2014])\d(?:[ \u00A0\-\u2013\u2014]*\d){14,15}(?![\d\-\u2013\u2014])"""
    )

    private val stripSeparatorsRegex = Regex("""[ \u00A0\-\u2013\u2014]""")

    /**
     * Finds the first 15 or 16 digit numeric code in [text], supporting contiguous digits as well
     * as digits separated by spaces or dashes. Returns the normalized digits-only string, or null.
     */
    fun extractFirstCode(text: String): String? {
        return extractCodes(text).firstOrNull()
    }

    /**
     * Finds all distinct 15 or 16 digit numeric codes in [text], supporting contiguous digits as
     * well as digits separated by spaces or dashes.
     */
    fun extractCodes(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val found = LinkedHashSet<String>()

        // Pass 1: exact \b\d{15,16}\b
        for (match in contiguousRegex.findAll(text)) {
            val digits = match.value
            if (digits.length in 15..16) {
                found.add(digits)
            }
        }

        // Pass 2: dash-separated numbers (strip dashes, verify 15-16 length)
        for (match in dashedRegex.findAll(text)) {
            val normalized = stripSeparatorsRegex.replace(match.value, "")
            if (normalized.length in 15..16 && normalized.all { it.isDigit() }) {
                found.add(normalized)
            }
        }

        // Pass 3: space-separated numbers (strip spaces, verify 15-16 length)
        for (match in spacedRegex.findAll(text)) {
            val normalized = stripSeparatorsRegex.replace(match.value, "")
            if (normalized.length in 15..16 && normalized.all { it.isDigit() }) {
                found.add(normalized)
            }
        }

        // Pass 4: mixed spaces and dashes
        for (match in mixedRegex.findAll(text)) {
            val normalized = stripSeparatorsRegex.replace(match.value, "")
            if (normalized.length in 15..16 && normalized.all { it.isDigit() }) {
                found.add(normalized)
            }
        }

        return found.toList()
    }

    /**
     * Collects all relevant text segments from a notification, ordered newest-first for stacked
     * lines / MessagingStyle conversations so the freshest message code is prioritized.
     */
    fun collectNotificationTexts(notification: Notification): List<String> {
        val extras = notification.extras ?: return emptyList()
        val segments = mutableListOf<String>()

        // 1. MessagingStyle messages (newest first)
        val messagingStyle = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null) {
            val messages = messagingStyle.messages
            for (i in messages.indices.reversed()) {
                val msg = messages[i]
                msg.text?.toString()?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
                msg.person?.name?.toString()?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
            }
            messagingStyle.conversationTitle?.toString()?.takeIf { it.isNotBlank() }?.let {
                segments.add(it)
            }
        }

        // 2. Raw EXTRA_MESSAGES bundle fallback (newest first)
        @Suppress("DEPRECATION")
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES, Bundle::class.java)
        } else {
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        }
        if (rawMessages != null) {
            for (i in rawMessages.indices.reversed()) {
                val bundle = rawMessages[i] as? Bundle ?: continue
                bundle.getCharSequence("text")?.toString()?.takeIf { it.isNotBlank() }?.let {
                    segments.add(it)
                }
                bundle.getCharSequence("sender")?.toString()?.takeIf { it.isNotBlank() }?.let {
                    segments.add(it)
                }
            }
        }

        // 3. BigText and standard Text
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
        extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }

        // 4. InboxStyle / grouped notification lines (newest first)
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null) {
            for (i in textLines.indices.reversed()) {
                textLines[i]?.toString()?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
            }
        }

        // 5. Titles, subText, summaryText
        extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
        extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
            ?.takeIf { it.isNotBlank() }?.let { segments.add(it) }

        return segments.distinct()
    }

    fun extractTitle(notification: Notification): String {
        val extras = notification.extras ?: return ""
        return extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?.takeIf { it.isNotBlank() }
            ?: ""
    }
}
