package com.infinitygreenpower.organizerform.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class FormAggregate(
    @Embedded val form: FormEntity,
    @Relation(parentColumn = "id", entityColumn = "formId") val items: List<FormItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "formId") val photos: List<FormPhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "formId") val loadNote: LoadNoteEntity?
)
