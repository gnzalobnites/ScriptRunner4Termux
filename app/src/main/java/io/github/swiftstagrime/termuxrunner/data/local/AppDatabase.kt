package io.github.swiftstagrime.termuxrunner.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CustomThemeDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CustomThemeEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import org.json.JSONArray

@Database(
    entities = [ScriptEntity::class, CategoryEntity::class, AutomationEntity::class, AutomationLogEntity::class, CustomThemeEntity::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), AutoMigration(
            from = 2,
            to = 3,
        ), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    abstract fun categoryDao(): CategoryDao

    abstract fun automationDao(): AutomationDao

    abstract fun automationLogDao(): AutomationLogDao

    abstract fun customThemeDao(): CustomThemeDao
}

val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE scripts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                codePages TEXT NOT NULL,
                page_names TEXT NOT NULL DEFAULT '',
                interpreter TEXT NOT NULL,
                fileExtension TEXT NOT NULL,
                commandPrefix TEXT NOT NULL,
                runInBackground INTEGER NOT NULL,
                openNewSession INTEGER NOT NULL,
                executionParams TEXT NOT NULL,
                iconPath TEXT,
                envVars TEXT NOT NULL,
                keepSessionOpen INTEGER NOT NULL,
                useHeartbeat INTEGER NOT NULL DEFAULT 0,
                heartbeatTimeout INTEGER NOT NULL DEFAULT 30000,
                heartbeatInterval INTEGER NOT NULL DEFAULT 10000,
                categoryId INTEGER DEFAULT NULL,
                orderIndex INTEGER NOT NULL DEFAULT 0,
                notifyOnResult INTEGER NOT NULL DEFAULT 0,
                interactionMode TEXT NOT NULL DEFAULT 'NONE',
                argumentPresets TEXT NOT NULL DEFAULT '',
                prefixPresets TEXT NOT NULL DEFAULT '',
                envVarPresets TEXT NOT NULL DEFAULT '',
                adbCode TEXT DEFAULT NULL
            )
        """)

        val cursor = database.query("SELECT * FROM scripts")

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val values = ContentValues()

                val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                val oldCode = cursor.getString(cursor.getColumnIndexOrThrow("code"))

                val codePagesJson = JSONArray().apply { put(oldCode) }.toString()

                values.put("id", id)
                values.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                values.put("codePages", codePagesJson)
                values.put("page_names", "[]")
                values.put("interpreter", cursor.getString(cursor.getColumnIndexOrThrow("interpreter")))
                values.put("fileExtension", cursor.getString(cursor.getColumnIndexOrThrow("fileExtension")))
                values.put("commandPrefix", cursor.getString(cursor.getColumnIndexOrThrow("commandPrefix")))
                values.put("runInBackground", cursor.getInt(cursor.getColumnIndexOrThrow("runInBackground")))
                values.put("openNewSession", cursor.getInt(cursor.getColumnIndexOrThrow("openNewSession")))
                values.put("executionParams", cursor.getString(cursor.getColumnIndexOrThrow("executionParams")))
                values.put("iconPath", cursor.getString(cursor.getColumnIndexOrThrow("iconPath")))
                values.put("envVars", cursor.getString(cursor.getColumnIndexOrThrow("envVars")))
                values.put("keepSessionOpen", cursor.getInt(cursor.getColumnIndexOrThrow("keepSessionOpen")))
                values.put("useHeartbeat", cursor.getInt(cursor.getColumnIndexOrThrow("useHeartbeat")))
                values.put("heartbeatTimeout", cursor.getInt(cursor.getColumnIndexOrThrow("heartbeatTimeout")))
                values.put("heartbeatInterval", cursor.getInt(cursor.getColumnIndexOrThrow("heartbeatInterval")))

                val catIdx = cursor.getColumnIndexOrThrow("categoryId")
                if (cursor.isNull(catIdx)) {
                    values.putNull("categoryId")
                } else {
                    values.put("categoryId", cursor.getLong(catIdx))
                }

                values.put("orderIndex", cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex")))
                values.put("notifyOnResult", cursor.getInt(cursor.getColumnIndexOrThrow("notifyOnResult")))
                values.put("interactionMode", cursor.getString(cursor.getColumnIndexOrThrow("interactionMode")))
                values.put("argumentPresets", cursor.getString(cursor.getColumnIndexOrThrow("argumentPresets")))
                values.put("prefixPresets", cursor.getString(cursor.getColumnIndexOrThrow("prefixPresets")))
                values.put("envVarPresets", cursor.getString(cursor.getColumnIndexOrThrow("envVarPresets")))
                values.put("adbCode", cursor.getString(cursor.getColumnIndexOrThrow("adbCode")))

                database.insert("scripts_new", SQLiteDatabase.CONFLICT_REPLACE, values)
            }
        }

        database.execSQL("DROP TABLE scripts")
        database.execSQL("ALTER TABLE scripts_new RENAME TO scripts")
    }
}
