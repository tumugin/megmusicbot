package com.myskng.megmusicbot.main

import picocli.CommandLine

class MegmusicMain {
    class AppCommand {
        @CommandLine.Option(names = ["bot"])
        var isBotMode: Boolean = false
        @CommandLine.Option(names = ["scanner"])
        var isScannerMode: Boolean = false
        val isModeNotSet
            get() = !(isBotMode || isScannerMode)
        @CommandLine.Option(names = ["--config"])
        var configPath = "./config.json"

        fun checkCommand() {
            if (isModeNotSet) {
                throw Exception("Specify bot or scanner mode.")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val command = AppCommand()
            CommandLine(command).parse(*args)
            command.checkCommand()
        }
    }
}