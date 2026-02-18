package com.rcdownload.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadHistoryEntity): Long

    /** Emite a lista atualizada sempre que o banco for modificado. */
    @Query("SELECT * FROM download_history ORDER BY downloadedAt DESC")
    fun getAllHistory(): Flow<List<DownloadHistoryEntity>>

    @Query("SELECT * FROM download_history WHERE videoId = :videoId")
    suspend fun getByVideoId(videoId: String): List<DownloadHistoryEntity>

    @Query("DELETE FROM download_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM download_history")
    suspend fun count(): Int
}
