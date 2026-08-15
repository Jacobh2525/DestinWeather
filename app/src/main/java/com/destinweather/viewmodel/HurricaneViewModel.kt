package com.destinweather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.destinweather.data.NhcClient
import com.destinweather.data.api.RetrofitClient
import com.destinweather.data.model.NhcStorm
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HurricaneState {
    object Loading : HurricaneState()
    data class Success(
        val storms: List<NhcStorm>,   // empty when the tropics are quiet
        val outlook: String?          // Atlantic Tropical Weather Outlook text; null if unavailable
    ) : HurricaneState()
    data class Error(val message: String) : HurricaneState()
}

class HurricaneViewModel : ViewModel() {

    private val _state = MutableStateFlow<HurricaneState>(HurricaneState.Loading)
    val state: StateFlow<HurricaneState> = _state

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            // Keep showing current content during pull-to-refresh
            if (_state.value !is HurricaneState.Success) {
                _state.value = HurricaneState.Loading
            }
            try {
                // Storms are essential; the outlook is best-effort
                val stormsDeferred = async { NhcClient.getActiveStorms() }
                val outlookDeferred = async { runCatching { fetchAtlanticOutlook() }.getOrNull() }

                _state.value = HurricaneState.Success(
                    storms = stormsDeferred.await(),
                    outlook = outlookDeferred.await()
                )
            } catch (e: Exception) {
                _state.value = HurricaneState.Error(e.message ?: "Unknown error")
            }
        }
    }

    // Latest Atlantic Tropical Weather Outlook (WMO collective ABNT20, issued by NHC)
    // via the NWS text-products API.
    private suspend fun fetchAtlanticOutlook(): String? {
        val list = RetrofitClient.noaaApi.getProductsByType("TWO")
        val ref = list.products?.firstOrNull {
            it.wmoCollectiveId == "ABNT20" && it.issuingOffice == "KNHC"
        } ?: return null
        val id = ref.id ?: return null
        return RetrofitClient.noaaApi.getProduct(id).productText
    }
}
