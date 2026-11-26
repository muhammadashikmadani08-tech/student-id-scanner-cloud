package com.institution.idscanner.cloud.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Per-user settings (institution name + Admin PIN) read from and written
 * to the user's Firestore profile document. Replaces the old on-device
 * settings storage: settings now follow the signed-in account to
 * every phone instead of living only on one device.
 */
class CloudSettings(private val repo: CloudRepository) {

    val institutionName: Flow<String> = repo.profile.map { it.institutionName }
    val adminPin: Flow<String> = repo.profile.map { it.adminPin }

    suspend fun setInstitutionName(name: String) = repo.setInstitutionName(name)
    suspend fun setAdminPin(pin: String) = repo.setAdminPin(pin)
}
