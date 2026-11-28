package net.mypapit.mobile.myrepeater.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface RepeaterDao {
    @Query("SELECT * FROM repeaters ORDER BY callsign, downlinkMhz")
    fun observeAll(): Flow<List<Repeater>>

    @Query("SELECT COUNT(*) FROM repeaters")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repeaters")
    suspend fun count(): Int

    @Query("DELETE FROM repeaters")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rows: List<Repeater>)
}

@Dao
interface CallsignDao {
    /** Matches the start of a callsign or any part of the holder's name. */
    @Query(
        "SELECT * FROM callsigns " +
            "WHERE callsign LIKE :query || '%' OR holder LIKE '%' || :query || '%' " +
            "ORDER BY callsign LIMIT :limit",
    )
    suspend fun search(query: String, limit: Int = 100): List<CallsignRecord>

    @Query("SELECT COUNT(*) FROM callsigns")
    suspend fun count(): Int

    @Query("DELETE FROM callsigns")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(rows: List<CallsignRecord>)
}

/**
 * The database is a cache of the two downloaded CSV files, so a schema change
 * simply drops it and the next sync refills it.
 */
@Database(entities = [Repeater::class, CallsignRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun repeaterDao(): RepeaterDao
    abstract fun callsignDao(): CallsignDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "repeater.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
