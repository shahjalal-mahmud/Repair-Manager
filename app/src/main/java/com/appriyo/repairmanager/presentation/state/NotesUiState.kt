// app/src/main/java/com/appriyo/repairmanager/presentation/state/NotesUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.data.model.NoteCategory
import com.appriyo.repairmanager.presentation.components.ToastMessage

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",

    /**
     * Which category tab the user is currently looking at. Defaults to
     * [NoteCategory.GENERAL]. Search is always global; this tab is only
     * used to narrow the displayed list when no search is active.
     */
    val selectedTab: NoteCategory = NoteCategory.GENERAL,

    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDialogOpen: Boolean = false,
    val editingNote: Note? = null,
    val titleError: String? = null,
    val noteToDelete: Note? = null,
    val isDeleting: Boolean = false,
    val toast: ToastMessage? = null,

    /**
     * The category selected inside the Add/Edit dialog. Defaults to whichever
     * tab is currently active when the dialog is opened for a new note; for
     * edits, it's seeded with the note's existing category.
     */
    val draftCategory: NoteCategory = NoteCategory.GENERAL,

    /** Photo attachments currently shown in the add/edit dialog (local-only, never sent to Firestore). */
    val editingAttachments: List<MediaAttachment> = emptyList(),
    /** Id used to namespace new photo filenames while the dialog is open: the note's own id when
     *  editing, or a throwaway draft id when creating a new note. */
    val currentDraftId: String = "",
    /** Bumped whenever a note's stored attachments change, so note cards know to reload their thumbnail. */
    val mediaRefreshTick: Int = 0
)