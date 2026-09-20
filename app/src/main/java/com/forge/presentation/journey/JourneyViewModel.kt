package com.forge.presentation.journey

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.forge.data.local.dao.TransformationDao
import com.forge.data.local.dao.UserProfileDao
import com.forge.data.local.entity.TransformationCheckInEntity
import com.forge.data.local.entity.UserProfileEntity
import com.forge.domain.engine.TransformationVaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

data class JourneyUiState(
    val isVaultConfigured: Boolean = false,
    val isUnlocked: Boolean = false,
    val checkIns: List<TransformationCheckInEntity> = emptyList(),
    val unlockError: String? = null,
    val currentTransformationWeek: Int = 1,
    val isSlideshowPlaying: Boolean = false,
    val slideshowIndex: Int = 0
)

class JourneyViewModel(
    private val transformationDao: TransformationDao,
    private val userProfileDao: UserProfileDao,
    private val vaultManager: TransformationVaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                userProfileDao.getUserProfile(),
                transformationDao.getCheckIns(),
                vaultManager.isUnlocked
            ) { profile: UserProfileEntity?, checkIns: List<TransformationCheckInEntity>, isUnlocked: Boolean ->
                val hasPassword = !profile?.photoPasswordHash.isNullOrEmpty()
                val startDate = profile?.transformationStartDate ?: System.currentTimeMillis()
                val currentWeek = vaultManager.calculateTransformationWeek(startDate)

                val sorted = checkIns.sortedBy { it.weekNumber }

                JourneyUiState(
                    isVaultConfigured = hasPassword,
                    isUnlocked = if (!hasPassword) true else isUnlocked,
                    checkIns = sorted,
                    currentTransformationWeek = currentWeek,
                    slideshowIndex = _uiState.value.slideshowIndex.coerceIn(0, (sorted.size - 1).coerceAtLeast(0))
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun unlockVault(password: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val profile = userProfileDao.getUserProfileSync()
            val hash = profile?.photoPasswordHash
            val salt = profile?.photoPasswordSalt

            if (hash == null || salt == null) {
                vaultManager.unlockWithoutPassword()
                _uiState.value = _uiState.value.copy(isUnlocked = true, unlockError = null)
                onSuccess()
                return@launch
            }

            val isValid = vaultManager.verifyPassword(password, hash, salt)
            if (isValid) {
                vaultManager.unlock()
                _uiState.value = _uiState.value.copy(isUnlocked = true, unlockError = null)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(unlockError = "Incorrect vault password")
            }
        }
    }

    fun lockVault() {
        vaultManager.lock()
        _uiState.value = _uiState.value.copy(isUnlocked = false)
    }

    fun decryptPhoto(path: String): Bitmap? {
        return vaultManager.decryptPhoto(path)
    }

    fun addMilestonePhoto(
        bitmap: Bitmap,
        viewAngle: String,
        weightKg: Double?,
        notes: String?
    ) {
        viewModelScope.launch {
            val profile = userProfileDao.getUserProfileSync()
            val startDate = profile?.transformationStartDate ?: System.currentTimeMillis()
            val now = System.currentTimeMillis()
            val weekNum = vaultManager.calculateTransformationWeek(startDate, now)

            val photoId = UUID.randomUUID().toString()
            val encryptedPath = vaultManager.encryptAndSavePhoto(bitmap, "photo_$photoId")

            val checkIn = TransformationCheckInEntity(
                id = photoId,
                weekNumber = weekNum,
                date = java.time.LocalDate.now().toString(),
                frontEncryptedPath = if (viewAngle.equals("Front", ignoreCase = true)) encryptedPath else null,
                sideEncryptedPath = if (viewAngle.equals("Side", ignoreCase = true)) encryptedPath else null,
                backEncryptedPath = if (viewAngle.equals("Back", ignoreCase = true)) encryptedPath else null,
                weightKg = weightKg,
                notes = notes,
                createdAt = now
            )
            transformationDao.insertOrUpdate(checkIn)
        }
    }

    fun nextSlide() {
        val count = _uiState.value.checkIns.size
        if (count > 0) {
            val next = (_uiState.value.slideshowIndex + 1) % count
            _uiState.value = _uiState.value.copy(slideshowIndex = next)
        }
    }

    fun prevSlide() {
        val count = _uiState.value.checkIns.size
        if (count > 0) {
            val prev = if (_uiState.value.slideshowIndex > 0) _uiState.value.slideshowIndex - 1 else count - 1
            _uiState.value = _uiState.value.copy(slideshowIndex = prev)
        }
    }
}
