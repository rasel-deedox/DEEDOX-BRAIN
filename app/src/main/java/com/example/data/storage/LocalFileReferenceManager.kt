package com.example.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FileHealthStatus(val code: String, val label: String) {
    AVAILABLE("AVAILABLE", "Available"),
    PERMISSION_NEEDED("PERMISSION_NEEDED", "Permission Needed"),
    MISSING("MISSING", "Missing"),
    UNSUPPORTED_PROVIDER("UNSUPPORTED_PROVIDER", "Unsupported Provider"),
    NOT_YET_CHECKED("NOT_YET_CHECKED", "Not Yet Checked")
}

data class InspectedLocalFile(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAt: Long?,
    val extension: String,
    val persistedPermission: Boolean,
    val healthStatus: FileHealthStatus,
    val extractedPreviewText: String?
)

class LocalFileReferenceManager(private val context: Context) {

    fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeIf { it.isNotBlank() } ?: "android_local_device"
        } catch (_: Exception) {
            "android_local_device"
        }
    }

    fun tryTakePersistablePermission(uri: Uri, isTree: Boolean = false): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            true
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun hasPersistedReadPermission(uri: Uri): Boolean {
        val target = uri.toString()
        return context.contentResolver.persistedUriPermissions.any { perm ->
            perm.uri.toString() == target && perm.isReadPermission
        }
    }

    suspend fun inspectFileUri(uri: Uri): InspectedLocalFile = withContext(Dispatchers.IO) {
        val persisted = hasPersistedReadPermission(uri)
        val mimeType = try {
            context.contentResolver.getType(uri) ?: "application/octet-stream"
        } catch (_: Exception) {
            "application/octet-stream"
        }

        var displayName = uri.lastPathSegment?.substringAfterLast('/') ?: "Local File"
        var sizeBytes = 0L
        var modifiedAt: Long? = null
        var health = FileHealthStatus.NOT_YET_CHECKED

        try {
            context.contentResolver.query(
                uri,
                arrayOf(
                    OpenableColumns.DISPLAY_NAME,
                    OpenableColumns.SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        displayName = cursor.getString(nameIdx)
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                    val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                    if (modIdx >= 0 && !cursor.isNull(modIdx)) {
                        modifiedAt = cursor.getLong(modIdx)
                    }
                    health = FileHealthStatus.AVAILABLE
                } else {
                    health = FileHealthStatus.MISSING
                }
            } ?: run {
                health = FileHealthStatus.UNSUPPORTED_PROVIDER
            }
        } catch (_: SecurityException) {
            health = FileHealthStatus.PERMISSION_NEEDED
        } catch (_: IllegalArgumentException) {
            health = FileHealthStatus.UNSUPPORTED_PROVIDER
        } catch (_: Exception) {
            health = FileHealthStatus.MISSING
        }

        if (health == FileHealthStatus.AVAILABLE) {
            try {
                context.contentResolver.openInputStream(uri)?.use { /* readable */ }
                    ?: run { health = FileHealthStatus.MISSING }
            } catch (_: SecurityException) {
                health = FileHealthStatus.PERMISSION_NEEDED
            } catch (_: Exception) {
                health = FileHealthStatus.MISSING
            }
        }

        val extension = displayName.substringAfterLast('.', "").lowercase()
        val extractedText = if (health == FileHealthStatus.AVAILABLE &&
            (mimeType.startsWith("text/") || extension in setOf("txt", "md", "csv", "json"))
        ) {
            extractBoundedTextSnippet(uri)
        } else {
            null
        }

        InspectedLocalFile(
            uriString = uri.toString(),
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            modifiedAt = modifiedAt,
            extension = extension,
            persistedPermission = persisted,
            healthStatus = health,
            extractedPreviewText = extractedText
        )
    }

    suspend fun checkHealth(uriString: String): FileHealthStatus = withContext(Dispatchers.IO) {
        val uri = try {
            Uri.parse(uriString)
        } catch (_: Exception) {
            return@withContext FileHealthStatus.UNSUPPORTED_PROVIDER
        }

        if (uri.scheme != "content") {
            return@withContext FileHealthStatus.UNSUPPORTED_PROVIDER
        }

        try {
            context.contentResolver.openInputStream(uri)?.use {
                return@withContext FileHealthStatus.AVAILABLE
            }
            FileHealthStatus.MISSING
        } catch (_: SecurityException) {
            FileHealthStatus.PERMISSION_NEEDED
        } catch (_: IllegalArgumentException) {
            FileHealthStatus.UNSUPPORTED_PROVIDER
        } catch (_: Exception) {
            FileHealthStatus.MISSING
        }
    }

    suspend fun scanDocumentTree(treeUri: Uri, maxItems: Int = 40): List<InspectedLocalFile> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<InspectedLocalFile>()
            try {
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                context.contentResolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    while (cursor.moveToNext() && results.size < maxItems) {
                        val childId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) else ""
                        if (mime != DocumentsContract.Document.MIME_TYPE_DIR) {
                            val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                            results.add(inspectFileUri(childUri))
                        }
                    }
                }
            } catch (_: Exception) {
                // Handled gracefully
            }
            results
        }

    fun resolveTreeDisplayName(treeUri: Uri): String {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
            context.contentResolver.query(
                rootDocUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val docIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val openIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val resolved = when {
                        docIdx >= 0 && !cursor.isNull(docIdx) -> cursor.getString(docIdx)
                        openIdx >= 0 && !cursor.isNull(openIdx) -> cursor.getString(openIdx)
                        else -> null
                    }?.trim()
                    if (!resolved.isNullOrBlank()) return resolved
                }
            }
        } catch (_: Exception) {
        }
        return treeUri.lastPathSegment
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "Connected Folder"
    }

    private fun extractBoundedTextSnippet(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(16 * 1024)
                val read = stream.read(buffer)
                if (read > 0) String(buffer, 0, read, Charsets.UTF_8).trim() else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
