package com.example.data.repository

import com.example.data.local.WorkspaceNoteEntity

data class ResolvedNoteLink(
    val rawTitle: String,
    val targetNote: WorkspaceNoteEntity?
)

object NoteBacklinksParser {
    private val WIKI_LINK_REGEX = Regex("\\[\\[([^\\]\\n]{1,120})\\]\\]")

    fun extractLinkedTitles(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return WIKI_LINK_REGEX.findAll(text)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .toList()
    }

    fun extractLinkedTitlesFromBlocks(blocks: List<WorkspaceBlock>): List<String> {
        if (blocks.isEmpty()) return emptyList()
        return extractLinkedTitles(blocks.joinToString("\n") { it.text })
    }

    fun extractLinkedTitlesFromBlocksJson(blocksJson: String): List<String> {
        return extractLinkedTitlesFromBlocks(WorkspaceBlock.parseList(blocksJson))
    }

    fun resolveLinks(
        titles: List<String>,
        allNotes: List<WorkspaceNoteEntity>,
        excludeNoteId: String? = null
    ): List<ResolvedNoteLink> {
        if (titles.isEmpty()) return emptyList()
        val byLowerTitle = allNotes
            .filter { it.deletedAt == null && it.id != excludeNoteId }
            .associateBy { it.title.trim().lowercase() }
        return titles.map { title ->
            ResolvedNoteLink(
                rawTitle = title,
                targetNote = byLowerTitle[title.trim().lowercase()]
            )
        }
    }

    fun findBacklinksForNote(
        targetNote: WorkspaceNoteEntity,
        allNotes: List<WorkspaceNoteEntity>
    ): List<WorkspaceNoteEntity> {
        val targetTitleLower = targetNote.title.trim().lowercase()
        if (targetTitleLower.isBlank()) return emptyList()
        return allNotes.filter { candidate ->
            if (candidate.deletedAt != null || candidate.id == targetNote.id) return@filter false
            extractLinkedTitlesFromBlocksJson(candidate.blocksJson)
                .any { it.trim().lowercase() == targetTitleLower }
        }
    }
}
