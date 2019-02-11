package com.myskng.megmusicbot.store

import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

fun readJsonConfig(filePath: String): BotConfig {
    val mapper = jacksonObjectMapper()
    mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    return mapper.readValue(File(filePath))
}