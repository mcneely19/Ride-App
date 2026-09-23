package com.onewheel.ridetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: adds the "boards" table (boards used to be a hardcoded enum).
 * This does NOT touch the existing "rides" table, so anyone upgrading from
 * v1 keeps every ride they've already logged. It also seeds the two boards
 * that used to be hardcoded (XRV/X7) so an upgrading device's existing rides
 * still resolve to a real board afterward. A brand-new install never runs
 * this migration (Room creates the schema fresh instead), so a fresh phone
 * starts with an empty boards table and sees the first-run setup screen.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS boards (
                name TEXT NOT NULL PRIMARY KEY,
                capacityWh REAL NOT NULL,
                subtitle TEXT NOT NULL DEFAULT '',
                colorHex TEXT NOT NULL DEFAULT '#0D7D78'
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT OR IGNORE INTO boards (name, capacityWh, subtitle, colorHex) VALUES " +
                "('XRV', 648.0, 'Molicel P50B cells', '#0D7D78')"
        )
        db.execSQL(
            "INSERT OR IGNORE INTO boards (name, capacityWh, subtitle, colorHex) VALUES " +
                "('X7', 518.0, 'Fungineers Thor 400 · Superflux Mk3 · Refloat 1.3', '#B5591A')"
        )
    }
}

@Database(entities = [Ride::class, BoardEntity::class], version = 2, exportSchema = false)
abstract class RideDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun boardDao(): BoardDao

    companion object {
        @Volatile private var INSTANCE: RideDatabase? = null

        fun get(context: Context): RideDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RideDatabase::class.java,
                    "ride_telemetry.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
