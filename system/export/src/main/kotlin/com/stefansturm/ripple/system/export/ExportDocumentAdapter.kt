package com.stefansturm.ripple.system.export

import com.stefansturm.ripple.core.domain.ExportPayload

data class ExportDocument(
    val filename: String,
    val mimeType: String,
    val content: String
)

object ExportDocumentAdapter {
    fun json(payload: ExportPayload): ExportDocument = ExportDocument(
        filename = "ripple-export.json",
        mimeType = "application/json",
        content = payload.json
    )

    fun csv(payload: ExportPayload): ExportDocument = ExportDocument(
        filename = "ripple-export.csv",
        mimeType = "text/csv",
        content = payload.csv
    )
}
