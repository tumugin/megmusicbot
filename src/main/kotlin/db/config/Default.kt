package db.config

import com.improve_future.harmonica.core.DbConfig
import com.improve_future.harmonica.core.Dbms

class Default : DbConfig({
    dbms = Dbms.SQLite
    dbName = "megmusicbot"
})