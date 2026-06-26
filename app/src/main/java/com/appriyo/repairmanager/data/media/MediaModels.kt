package com.appriyo.repairmanager.data.media

import android.net.Uri

/** Type of locally-stored media attached to a repair. */
enum class MediaType { PHOTO, VIDEO }

/**
 * A single photo or video attached to a repair, backed by a MediaStore Uri
 * pointing into shared device storage (Pictures/RepairManager or
 * Movies/RepairManager). Never sent to Firestore — local only.
 */
data class MediaAttachment(
    val uri: Uri,
    val type: MediaType
)