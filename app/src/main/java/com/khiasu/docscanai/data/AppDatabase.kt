package com.khiasu.docscanai.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocDao {
    @Insert
    suspend fun insertDocument(doc: DocumentEntity): Long

    @Insert
    suspend fun insertPage(page: PageEntity): Long

    @Insert
    suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Update
    suspend fun updatePage(page: PageEntity)

    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :docId")
    suspend fun getDocument(docId: Long): DocumentEntity?

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    fun observePages(docId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE documentId = :docId ORDER BY pageIndex ASC")
    suspend fun getPages(docId: Long): List<PageEntity>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    suspend fun getPage(pageId: Long): PageEntity?

    @Query("DELETE FROM documents WHERE id = :docId")
    suspend fun deleteDocument(docId: Long)
}

@Database(entities = [DocumentEntity::class, PageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun docDao(): DocDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "docscanai.db"
                ).build().also { INSTANCE = it }
            }
    }
}
