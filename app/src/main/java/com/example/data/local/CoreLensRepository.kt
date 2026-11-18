package com.example.data.local

import kotlinx.coroutines.flow.Flow

class CoreLensRepository(
    private val db: CoreLensDatabase,
    val preferences: CoreLensPreferences
) {
    val relays: Flow<List<RelayEndpointEntity>> = db.relayEndpointDao().observeAll()
    val dispatchMessages: Flow<List<DispatchMessageEntity>> = db.dispatchMessageDao().observeAll()
    val attachedSources: Flow<List<AttachedSourceEntity>> = db.attachedSourceDao().observeAll()
    val inspectionHistory: Flow<List<InspectionHistoryEntity>> = db.inspectionHistoryDao().observeRecent()

    suspend fun saveRelay(entity: RelayEndpointEntity): Long {
        return if (entity.id == 0L) {
            db.relayEndpointDao().insert(entity)
        } else {
            db.relayEndpointDao().update(entity)
            entity.id
        }
    }

    suspend fun deleteRelay(id: Long) = db.relayEndpointDao().deleteById(id)

    suspend fun saveDispatchMessage(entity: DispatchMessageEntity): Long =
        db.dispatchMessageDao().insert(entity)

    suspend fun updateDispatchMessage(entity: DispatchMessageEntity) =
        db.dispatchMessageDao().update(entity)

    suspend fun deleteDispatchMessage(id: Long) =
        db.dispatchMessageDao().deleteById(id)

    suspend fun saveAttachedSource(entity: AttachedSourceEntity): Long =
        db.attachedSourceDao().insert(entity)

    suspend fun deleteAttachedSource(id: Long) =
        db.attachedSourceDao().deleteById(id)

    suspend fun recordInspection(toolType: String, inputValue: String, summaryLabel: String) {
        if (inputValue.isBlank()) return
        db.inspectionHistoryDao().insert(
            InspectionHistoryEntity(
                toolType = toolType,
                inputValue = inputValue,
                summaryLabel = summaryLabel
            )
        )
    }

    suspend fun clearInspectionHistory() = db.inspectionHistoryDao().clearAll()
}
