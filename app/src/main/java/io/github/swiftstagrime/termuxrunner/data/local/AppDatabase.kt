package io.github.swiftstagrime.termuxrunner.data.local

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

        database.execSQL("""
            INSERT INTO scripts_new (
                id, name, codePages, page_names, interpreter, fileExtension, commandPrefix,
                runInBackground, openNewSession, executionParams, iconPath,
                envVars, keepSessionOpen, useHeartbeat, heartbeatTimeout,
                heartbeatInterval, categoryId, orderIndex, notifyOnResult,
                interactionMode, argumentPresets, prefixPresets, envVarPresets, adbCode
            )
            SELECT
                id, name,
                '[' || REPLACE(REPLACE(code, '\\', '\\\\'), '"', '\\"') || ']',
                '',
                interpreter, fileExtension, commandPrefix,
                runInBackground, openNewSession, executionParams, iconPath,
                envVars, keepSessionOpen, useHeartbeat, heartbeatTimeout,
                heartbeatInterval, categoryId, orderIndex, notifyOnResult,
                interactionMode, argumentPresets, prefixPresets, envVarPresets, adbCode
            FROM scripts
        """)

        database.execSQL("DROP TABLE scripts")
        database.execSQL("ALTER TABLE scripts_new RENAME TO scripts")
    }
}
