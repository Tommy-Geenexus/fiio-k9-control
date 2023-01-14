/*
 * Copyright (c) 2021-2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tomg.fiiok9control.profile.data

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.tomg.fiiok9control.Empty
import com.tomg.fiiok9control.INTENT_ACTION_SHORTCUT_PROFILE
import com.tomg.fiiok9control.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: ProfileDao
) {

    private companion object {

        const val PROFILES_MAX = 100
    }

    fun getProfiles() = profileDao.getProfiles()

    suspend fun insertProfile(profile: Profile): Boolean {
        return runCatching {
            if (profileDao.getProfileCount() < PROFILES_MAX) {
                profileDao.insert(profile)
                true
            } else {
                false
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            false
        }
    }

    suspend fun deleteProfile(profile: Profile): Boolean {
        return runCatching {
            profileDao.delete(profile)
            true
        }.getOrElse { exception ->
            Timber.e(exception)
            false
        }
    }

    suspend fun addProfileShortcut(profile: Profile): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                val intent = context
                    .packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        putExtra(INTENT_ACTION_SHORTCUT_PROFILE, profile.toPersistableBundle())
                    }
                    ?: return@runCatching false
                val shortcut = ShortcutInfoCompat.Builder(context, profile.id.toString())
                    .setShortLabel(profile.name)
                    .setLongLabel(profile.name)
                    .setIcon(
                        IconCompat.createWithResource(context, R.drawable.ic_shortcut_profile)
                    )
                    .setIntent(intent)
                    .build()
                ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    suspend fun removeProfileShortcut(profile: Profile): Boolean {
        return withContext(Dispatchers.IO) {
            val shortcut = listOf(profile.id.toString())
            runCatching {
                ShortcutManagerCompat.removeDynamicShortcuts(context, shortcut)
                ShortcutManagerCompat.disableShortcuts(context, shortcut, String.Empty)
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }
}
