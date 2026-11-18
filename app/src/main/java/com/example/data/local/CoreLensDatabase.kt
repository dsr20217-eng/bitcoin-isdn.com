package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        RelayEndpointEntity::class,
        DispatchMessageEntity::class,
        AttachedSourceEntity::class,
        InspectionHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CoreLensDatabase : RoomDatabase() {
    abstract fun relayEndpointDao(): RelayEndpointDao
    abstract fun dispatchMessageDao(): DispatchMessageDao
    abstract fun attachedSourceDao(): AttachedSourceDao
    abstract fun inspectionHistoryDao(): InspectionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: CoreLensDatabase? = null

        fun getInstance(context: Context): CoreLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CoreLensDatabase::class.java,
                    "corelens_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
