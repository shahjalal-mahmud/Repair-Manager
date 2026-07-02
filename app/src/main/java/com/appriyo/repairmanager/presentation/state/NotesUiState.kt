// app/src/main/java/com/appriyo/repairmanager/presentation/state/NotesUiState.kt
package com.appriyo.repairmanager.presentation.state

import com.appriyo.repairmanager.data.media.MediaAttachment
import com.appriyo.repairmanager.data.model.Note
import com.appriyo.repairmanager.presentation.components.ToastMessage

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isDialogOpen: Boolean = false,
    val editingNote: Note? = null,
    val titleError: String? = null,
    val noteToDelete: Note? = null,
    val isDeleting: Boolean = false,
    val toast: ToastMessage? = null,

    /** Photo attachments currently shown in the add/edit dialog (local-only, never sent to Firestore). */
    val editingAttachments: List<MediaAttachment> = emptyList(),
    /** Id used to namespace new photo filenames while the dialog is open: the note's own id when
     *  editing, or a throwaway draft id when creating a new note. */
    val currentDraftId: String = "",
    /** Bumped whenever a note's stored attachments change, so note cards know to reload their thumbnail. */
    val mediaRefreshTick: Int = 0
)