package db.migration

import com.improve_future.harmonica.core.AbstractMigration

/**
 * Migration
 */
class M20190113054842432_Migration : AbstractMigration() {
    override fun up() {
        createTable("songs") {
            text("title")
            text("album")
            text("artist")
            text("file_path")
        }
    }

    override fun down() {
        dropTable("songs")
    }
}