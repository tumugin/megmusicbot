package com.myskng.megmusicbot.test.config

import com.myskng.megmusicbot.config.splitEnvVariableToToList
import org.junit.Assert
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream

class ConfigLoaderTest {
    @ParameterizedTest
    @ArgumentsSource(TestPaths::class)
    fun testArrayEnvConfig(testString: String, expected: Array<String>) {
        val result = splitEnvVariableToToList(testString)
        Assert.assertArrayEquals(expected, result)
    }

    private class TestPaths : ArgumentsProvider {
        override fun provideArguments(context: ExtensionContext?) = Stream.of(
            Arguments.arguments("/home/ueda_reina", arrayOf("/home/ueda_reina")),
            Arguments.arguments("\"/home/kuroki honoka\"", arrayOf("/home/kuroki honoka")),
            Arguments.arguments(
                "\"/home/kuroki honoka\" \"/home/osaki amana\"",
                arrayOf("/home/kuroki honoka", "/home/osaki amana")
            ),
            Arguments.arguments(
                "\"/home/shioiri asuka\" /home/shioiri_asuka",
                arrayOf("/home/shioiri asuka", "/home/shioiri_asuka")
            ),
            Arguments.arguments("黒木ほの香", arrayOf("黒木ほの香")),
            Arguments.arguments("\"黒木ほの香\"", arrayOf("黒木ほの香")),
            Arguments.arguments("\"黒木 ほの香\" \"汐入 あすか\"", arrayOf("黒木 ほの香", "汐入 あすか")),
            Arguments.arguments("\"黒木ほの香\" \"汐入あすか\"", arrayOf("黒木ほの香", "汐入あすか")),
            Arguments.arguments("黒木ほの香 汐入あすか", arrayOf("黒木ほの香", "汐入あすか"))
        )
    }
}
