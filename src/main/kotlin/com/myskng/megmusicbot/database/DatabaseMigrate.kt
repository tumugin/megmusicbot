package com.myskng.megmusicbot.database

import com.myskng.megmusicbot.config.BotConfig
import org.flywaydb.core.Flyway

fun runDatabaseMigrate(config: BotConfig) {
    println("====== MIGRATION MODE ======")
    val flyway = Flyway.configure()
        .dataSource(config.dbConnectionString, config.dbConnectionUser, config.dbConnectionPassword)
        .load()
    println("\n** Current Migration Status **")
    flyway.info().all().forEach {
        println("=> [V${it.version.version}][${it.state.displayName}] ${it.description}")
    }
    println("\nRunning incomplete migration above...")
    flyway.migrate()
    println("Migration done!")
}
