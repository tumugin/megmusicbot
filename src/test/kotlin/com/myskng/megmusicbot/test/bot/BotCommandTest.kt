package com.myskng.megmusicbot.test.bot

import com.myskng.megmusicbot.bot.BotCommand
import com.myskng.megmusicbot.test.base.AbstractDefaultTester
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BotCommandTest : AbstractDefaultTester() {
    @Test
    fun testCommandSplitter() {
        val testText = "/title 楽園 /artist 関裕美"
        val testArray = arrayOf("/title", "楽園", "/artist", "関裕美")
        val splittedArray = BotCommand.splitCommandToArray(testText)
        Assertions.assertArrayEquals(testArray, splittedArray)
    }

    @Test
    fun testCommandSplitterWithQuote() {
        val testText = "/title \"Last Kiss\" /artist 三船美優"
        val testArray = arrayOf("/title", "Last Kiss", "/artist", "三船美優")
        val splittedArray = BotCommand.splitCommandToArray(testText)
        Assertions.assertArrayEquals(testArray, splittedArray)
    }
}