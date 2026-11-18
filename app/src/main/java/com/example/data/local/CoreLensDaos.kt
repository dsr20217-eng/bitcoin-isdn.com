package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayEndpointDao {
    @Query("SELECT * FROM relay_endpoints ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RelayEndpointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RelayEndpointEntity): Long

    @Update
    suspend fun update(entity: RelayEndpointEntity)

    @Query("DELETE FROM relay_endpoints WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface DispatchMessageDao {
    @Query("SELECT * FROM dispatch_messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DispatchMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DispatchMessageEntity): Long

    @Update
    suspend fun update(entity: DispatchMessageEntity)

    @Query("DELETE FROM dispatch_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AttachedSourceDao {
    @Query("SELECT * FROM attached_sources ORDER BY attachedAt DESC")
    fun observeAll(): Flow<List<AttachedSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AttachedSourceEntity): Long

    @Query("DELETE FROM attached_sources WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface InspectionHistoryDao {
    @Query("SELECT * FROM inspection_history ORDER BY createdAt DESC LIMIT 30")
    fun observeRecent(): Flow<List<InspectionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: InspectionHistoryEntity): Long

    @Query("DELETE FROM inspection_history")
    suspend fun clearAll()
}
