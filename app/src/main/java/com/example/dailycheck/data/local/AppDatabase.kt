package com.example.dailycheck.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dailycheck.data.local.converter.Converters
import com.example.dailycheck.data.local.dao.CheckInRecordDao
import com.example.dailycheck.data.local.dao.TaskDao
import com.example.dailycheck.data.local.entity.CheckInRecordEntity
import com.example.dailycheck.data.local.entity.TaskEntity

@Database(
    entities = [TaskEntity::class, CheckInRecordEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun checkInRecordDao(): CheckInRecordDao

    companion object {
        const val NAME = "daily_check.db"

        /**
         * v1 -> v2：
         * 1) tasks 增加 type / targetCount / targetDurationMinutes
         * 2) 新增 check_in_records
         * 用"建新表 + 搬数据 + 改名"而不是 ALTER ... DEFAULT，
         * 保证列定义与实体完全一致（Room 启动时会校验 schema）。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS tasks_new (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "iconKey TEXT NOT NULL, " +
                        "colorArgb INTEGER NOT NULL, " +
                        "category TEXT, " +
                        "repeatRule TEXT NOT NULL, " +
                        "note TEXT, " +
                        "reminderEnabled INTEGER NOT NULL, " +
                        "reminderMinutes INTEGER NOT NULL, " +
                        "createdAt INTEGER NOT NULL, " +
                        "completedOnEpochDay INTEGER, " +
                        "type TEXT NOT NULL, " +
                        "targetCount INTEGER NOT NULL, " +
                        "targetDurationMinutes INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO tasks_new (" +
                        "id, name, iconKey, colorArgb, category, repeatRule, note, " +
                        "reminderEnabled, reminderMinutes, createdAt, completedOnEpochDay, " +
                        "type, targetCount, targetDurationMinutes) " +
                        "SELECT id, name, iconKey, colorArgb, category, repeatRule, note, " +
                        "reminderEnabled, reminderMinutes, createdAt, completedOnEpochDay, " +
                        "'COMPLETE', 1, 25 FROM tasks"
                )
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS check_in_records (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "taskId INTEGER NOT NULL, " +
                        "epochDay INTEGER NOT NULL, " +
                        "completedAtMillis INTEGER NOT NULL, " +
                        "elapsedSeconds INTEGER NOT NULL, " +
                        "source TEXT NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_check_in_records_taskId_epochDay " +
                        "ON check_in_records (taskId, epochDay)"
                )
            }
        }

        /** v2 -> v3：计时型改为存总秒数，旧数据按分钟换算 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN targetDurationSeconds INTEGER NOT NULL DEFAULT 1500"
                )
                db.execSQL("UPDATE tasks SET targetDurationSeconds = targetDurationMinutes * 60")
            }
        }
    }
}
