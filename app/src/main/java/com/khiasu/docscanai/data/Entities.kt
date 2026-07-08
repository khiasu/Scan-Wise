package com.khiasu.docscanai.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One scan "session" - could be a single photo, a batch of photos, or an imported PDF. */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val sourceType: String, // "CAMERA", "GALLERY", "PDF"
    val providerUsed: String = "" // "GEMINI" / "OPENAI" / "CLAUDE"
)

/** One page/image belonging to a document. */
@Entity(
    tableName = "pages",
    indices = [Index("documentId")],
    foreignKeys = [ForeignKey(
        entity = DocumentEntity::class,
        parentColumns = ["id"],
        childColumns = ["documentId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val imagePath: String,          // local file path to the page image (jpg)
    val status: String = "PENDING", // PENDING, PROCESSING, DONE, ERROR
    val rawText: String? = null,    // full OCR text returned by the AI
    val fieldsJson: String? = null, // JSON array of {"key":..,"value":..} extracted by the AI
    val errorMessage: String? = null
)
