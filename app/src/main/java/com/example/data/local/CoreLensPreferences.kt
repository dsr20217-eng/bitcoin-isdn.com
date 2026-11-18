package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.coreLensDataStore: DataStore<Preferences> by preferencesDataStore(name = "corelens_prefs")

data class AutoSavedSettings(
    val lastRpcHost: String = "http://127.0.0.1",
    val lastRpcPort: Int = 8332,
    val lastChainId: String = "main",
    val customDomain: String = "corelens.local",
    val senderEmail: String = "dispatch@corelens.local",
    val recipientEmail: String = "",
    val autoSaveHistory: Boolean = true
)

class CoreLensPreferences(private val context: Context) {

    private object Keys {
        val LAST_RPC_HOST = stringPreferencesKey("last_rpc_host")
        val LAST_RPC_PORT = intPreferencesKey("last_rpc_port")
        val LAST_CHAIN_ID = stringPreferencesKey("last_chain_id")
        val CUSTOM_DOMAIN = stringPreferencesKey("custom_domain")
        val SENDER_EMAIL = stringPreferencesKey("sender_email")
        val RECIPIENT_EMAIL = stringPreferencesKey("recipient_email")
        val AUTO_SAVE_HISTORY = booleanPreferencesKey("auto_save_history")
    }

    val settingsFlow: Flow<AutoSavedSettings> = context.coreLensDataStore.data.map { prefs ->
        AutoSavedSettings(
            lastRpcHost = prefs[Keys.LAST_RPC_HOST] ?: "http://127.0.0.1",
            lastRpcPort = prefs[Keys.LAST_RPC_PORT] ?: 8332,
            lastChainId = prefs[Keys.LAST_CHAIN_ID] ?: "main",
            customDomain = prefs[Keys.CUSTOM_DOMAIN] ?: "corelens.local",
            senderEmail = prefs[Keys.SENDER_EMAIL] ?: "dispatch@corelens.local",
            recipientEmail = prefs[Keys.RECIPIENT_EMAIL] ?: "",
            autoSaveHistory = prefs[Keys.AUTO_SAVE_HISTORY] ?: true
        )
    }

    suspend fun saveRpcEndpoint(host: String, port: Int, chainId: String) {
        context.coreLensDataStore.edit { prefs ->
            prefs[Keys.LAST_RPC_HOST] = host
            prefs[Keys.LAST_RPC_PORT] = port
            prefs[Keys.LAST_CHAIN_ID] = chainId
        }
    }

    suspend fun saveDispatchRouting(domain: String, sender: String, recipient: String) {
        context.coreLensDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_DOMAIN] = domain
            prefs[Keys.SENDER_EMAIL] = sender
            prefs[Keys.RECIPIENT_EMAIL] = recipient
        }
    }

    suspend fun setAutoSaveHistory(enabled: Boolean) {
        context.coreLensDataStore.edit { prefs ->
            prefs[Keys.AUTO_SAVE_HISTORY] = enabled
        }
    }
}
