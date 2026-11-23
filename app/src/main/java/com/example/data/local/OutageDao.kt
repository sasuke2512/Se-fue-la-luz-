package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutageDao {

    @Query("SELECT * FROM outages WHERE endTimeMillis IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    fun observeActiveOutage(): Flow<OutageEntity?>

    @Query("SELECT * FROM outages WHERE endTimeMillis IS NULL ORDER BY startTimeMillis DESC LIMIT 1")
    suspend fun getActiveOutageOnce(): OutageEntity?

    @Query("SELECT * FROM outages ORDER BY startTimeMillis DESC")
    fun observeAllOutages(): Flow<List<OutageEntity>>

    @Query("SELECT * FROM outages WHERE endTimeMillis IS NOT NULL ORDER BY startTimeMillis DESC")
    fun observeCompletedOutages(): Flow<List<OutageEntity>>

    @Query("SELECT * FROM outages WHERE endTimeMillis IS NOT NULL ORDER BY startTimeMillis DESC")
    suspend fun getCompletedOutagesOnce(): List<OutageEntity>

    @Query("SELECT * FROM outages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): OutageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(outage: OutageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(outages: List<OutageEntity>)

    @Update
    suspend fun update(outage: OutageEntity)

    @Query("DELETE FROM outages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM outages WHERE endTimeMillis IS NULL")
    suspend fun deleteAllActive()

    @Query("DELETE FROM outages")
    suspend fun deleteAll()

    @Transaction
    suspend fun startOutageIfNoneActive(startTimeMillis: Long): Long? {
        if (getActiveOutageOnce() != null) {
            return null
        }
        return insert(
            OutageEntity(
                startTimeMillis = startTimeMillis,
                endTimeMillis = null,
                note = "",
                createdAtMillis = startTimeMillis
            )
        )
    }

    @Transaction
    suspend fun stopActiveOutage(endTimeMillis: Long): OutageEntity? {
        val active = getActiveOutageOnce() ?: return null
        val safeEnd = maxOf(endTimeMillis, active.startTimeMillis)
        val completed = active.copy(endTimeMillis = safeEnd)
        update(completed)
        return completed
    }
}
