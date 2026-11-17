package com.example.data.backup

import android.content.Context
import android.net.Uri
import com.example.data.local.CollectionEntity
import com.example.data.local.CollectionItemCrossRef
import com.example.data.local.FolderEntity
import com.example.data.local.ItemEntity
import com.example.data.local.ItemFolderCrossRef
import com.example.data.local.ItemTagCrossRef
import com.example.data.local.NoteEntity
import com.example.data.local.TagEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class LibraryBackupPayload(
    val items: List<ItemEntity>,
    val folders: List<FolderEntity>,
    val itemFolders: List<ItemFolderCrossRef>,
    val tags: List<TagEntity>,
    val itemTags: List<ItemTagCrossRef>,
    val notes: List<NoteEntity>,
    val collections: List<CollectionEntity>,
    val collectionItems: List<CollectionItemCrossRef>
)

object BackupExporter {

    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        payload: LibraryBackupPayload
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject().apply {
                put("schemaVersion", 1)
                put("exportedAt", System.currentTimeMillis())
                put("app", "Personal AI Library Android v1.0")
                put("items", JSONArray().apply {
                    payload.items.forEach { item ->
                        put(JSONObject().apply {
                            put("id", item.id)
                            put("title", item.title)
                            put("description", item.description)
                            put("originalUrl", item.originalUrl)
                            put("canonicalUrl", item.canonicalUrl)
                            put("domain", item.domain)
                            put("contentType", item.contentType)
                            put("sourceType", item.sourceType)
                            put("thumbnailUrl", item.thumbnailUrl)
                            put("isFavorite", item.isFavorite)
                            put("status", item.status)
                            put("isDeviceLocal", item.isDeviceLocal)
                            put("localOnly", item.localOnly)
                            put("localVersion", item.localVersion)
                            put("createdAt", item.createdAt)
                            put("updatedAt", item.updatedAt)
                        })
                    }
                })
                put("folders", JSONArray().apply {
                    payload.folders.forEach { folder ->
                        put(JSONObject().apply {
                            put("id", folder.id)
                            put("name", folder.name)
                            put("parentFolderId", folder.parentFolderId)
                            put("colorHex", folder.colorHex)
                            put("iconKey", folder.iconKey)
                            put("sortOrder", folder.sortOrder)
                            put("isFavorite", folder.isFavorite)
                            put("isArchived", folder.isArchived)
                            put("isInbox", folder.isInbox)
                            put("createdAt", folder.createdAt)
                            put("updatedAt", folder.updatedAt)
                        })
                    }
                })
                put("itemFolders", JSONArray().apply {
                    payload.itemFolders.forEach { ref ->
                        put(JSONObject().apply {
                            put("itemId", ref.itemId)
                            put("folderId", ref.folderId)
                            put("addedAt", ref.addedAt)
                        })
                    }
                })
                put("tags", JSONArray().apply {
                    payload.tags.forEach { tag ->
                        put(JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                            put("normalizedName", tag.normalizedName)
                            put("origin", tag.origin)
                            put("createdAt", tag.createdAt)
                        })
                    }
                })
                put("itemTags", JSONArray().apply {
                    payload.itemTags.forEach { ref ->
                        put(JSONObject().apply {
                            put("itemId", ref.itemId)
                            put("tagId", ref.tagId)
                        })
                    }
                })
                put("notes", JSONArray().apply {
                    payload.notes.forEach { note ->
                        put(JSONObject().apply {
                            put("id", note.id)
                            put("itemId", note.itemId)
                            put("body", note.body)
                            put("isPinned", note.isPinned)
                            put("createdAt", note.createdAt)
                            put("updatedAt", note.updatedAt)
                        })
                    }
                })
                put("collections", JSONArray().apply {
                    payload.collections.forEach { c ->
                        put(JSONObject().apply {
                            put("id", c.id)
                            put("name", c.name)
                            put("description", c.description)
                            put("createdAt", c.createdAt)
                            put("updatedAt", c.updatedAt)
                        })
                    }
                })
                put("collectionItems", JSONArray().apply {
                    payload.collectionItems.forEach { ref ->
                        put(JSONObject().apply {
                            put("collectionId", ref.collectionId)
                            put("itemId", ref.itemId)
                            put("sortOrder", ref.sortOrder)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext Result.failure(IllegalStateException("Unable to open export destination"))

            Result.success(payload.items.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Result<LibraryBackupPayload> =
        withContext(Dispatchers.IO) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext Result.failure(IllegalStateException("Unable to read backup file"))

                val root = JSONObject(raw)
                val itemsArr = root.optJSONArray("items") ?: JSONArray()
                val items = (0 until itemsArr.length()).mapNotNull { idx ->
                    val o = itemsArr.optJSONObject(idx) ?: return@mapNotNull null
                    ItemEntity(
                        id = o.optString("id"),
                        title = o.optString("title", "Imported Item"),
                        description = o.optString("description", ""),
                        originalUrl = o.optString("originalUrl").takeIf { it.isNotBlank() && it != "null" },
                        canonicalUrl = o.optString("canonicalUrl").takeIf { it.isNotBlank() && it != "null" },
                        domain = o.optString("domain").takeIf { it.isNotBlank() && it != "null" },
                        contentType = o.optString("contentType", "link"),
                        sourceType = o.optString("sourceType", "manual"),
                        thumbnailUrl = o.optString("thumbnailUrl").takeIf { it.isNotBlank() && it != "null" },
                        isFavorite = o.optBoolean("isFavorite", false),
                        status = o.optString("status", "unsorted"),
                        isDeviceLocal = o.optBoolean("isDeviceLocal", false),
                        localOnly = o.optBoolean("localOnly", false),
                        localVersion = o.optInt("localVersion", 1),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                }

                val foldersArr = root.optJSONArray("folders") ?: JSONArray()
                val folders = (0 until foldersArr.length()).mapNotNull { idx ->
                    val o = foldersArr.optJSONObject(idx) ?: return@mapNotNull null
                    FolderEntity(
                        id = o.optString("id"),
                        name = o.optString("name", "Imported Folder"),
                        parentFolderId = o.optString("parentFolderId").takeIf { it.isNotBlank() && it != "null" },
                        colorHex = o.optString("colorHex", "#E53935"),
                        iconKey = o.optString("iconKey", "folder"),
                        sortOrder = o.optInt("sortOrder", 0),
                        isFavorite = o.optBoolean("isFavorite", false),
                        isArchived = o.optBoolean("isArchived", false),
                        isInbox = o.optBoolean("isInbox", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                }

                val itemFoldersArr = root.optJSONArray("itemFolders") ?: JSONArray()
                val itemFolders = (0 until itemFoldersArr.length()).mapNotNull { idx ->
                    val o = itemFoldersArr.optJSONObject(idx) ?: return@mapNotNull null
                    ItemFolderCrossRef(
                        itemId = o.optString("itemId"),
                        folderId = o.optString("folderId"),
                        addedAt = o.optLong("addedAt", System.currentTimeMillis())
                    )
                }

                val tagsArr = root.optJSONArray("tags") ?: JSONArray()
                val tags = (0 until tagsArr.length()).mapNotNull { idx ->
                    val o = tagsArr.optJSONObject(idx) ?: return@mapNotNull null
                    TagEntity(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        normalizedName = o.optString("normalizedName"),
                        origin = o.optString("origin", "user"),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis())
                    )
                }

                val itemTagsArr = root.optJSONArray("itemTags") ?: JSONArray()
                val itemTags = (0 until itemTagsArr.length()).mapNotNull { idx ->
                    val o = itemTagsArr.optJSONObject(idx) ?: return@mapNotNull null
                    ItemTagCrossRef(
                        itemId = o.optString("itemId"),
                        tagId = o.optString("tagId")
                    )
                }

                val notesArr = root.optJSONArray("notes") ?: JSONArray()
                val notes = (0 until notesArr.length()).mapNotNull { idx ->
                    val o = notesArr.optJSONObject(idx) ?: return@mapNotNull null
                    NoteEntity(
                        id = o.optString("id"),
                        itemId = o.optString("itemId"),
                        body = o.optString("body"),
                        isPinned = o.optBoolean("isPinned", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                }

                val colsArr = root.optJSONArray("collections") ?: JSONArray()
                val collections = (0 until colsArr.length()).mapNotNull { idx ->
                    val o = colsArr.optJSONObject(idx) ?: return@mapNotNull null
                    CollectionEntity(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        description = o.optString("description", ""),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                }

                val colItemsArr = root.optJSONArray("collectionItems") ?: JSONArray()
                val collectionItems = (0 until colItemsArr.length()).mapNotNull { idx ->
                    val o = colItemsArr.optJSONObject(idx) ?: return@mapNotNull null
                    CollectionItemCrossRef(
                        collectionId = o.optString("collectionId"),
                        itemId = o.optString("itemId"),
                        sortOrder = o.optInt("sortOrder", 0)
                    )
                }

                Result.success(
                    LibraryBackupPayload(
                        items = items,
                        folders = folders,
                        itemFolders = itemFolders,
                        tags = tags,
                        itemTags = itemTags,
                        notes = notes,
                        collections = collections,
                        collectionItems = collectionItems
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
