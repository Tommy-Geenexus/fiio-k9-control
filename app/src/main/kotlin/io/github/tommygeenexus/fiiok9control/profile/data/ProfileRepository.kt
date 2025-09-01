/*
 * Copyright (c) 2021-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.profile.data

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.db.Profile
import io.github.tommygeenexus.fiiok9control.core.db.ProfileDao
import io.github.tommygeenexus.fiiok9control.core.di.DispatcherIo
import io.github.tommygeenexus.fiiok9control.core.extension.suspendRunCatching
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_SHORTCUT_PROFILE
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class ProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher,
    private val profileDao: ProfileDao
) {

    private companion object {

        const val PROFILES_MAX = 100
    }

    suspend fun getProfiles(): List<Profile> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            profileDao.getProfiles()
        }
    }.getOrElse { exception ->
        Timber.e(exception)
        emptyList()
    }

    suspend fun insertProfile(profile: Profile): Boolean = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            if (profileDao.getProfileCount() < PROFILES_MAX) {
                profileDao.upsert(profile)
                true
            } else {
                false
            }
        }
    }.getOrElse { exception ->
        Timber.e(exception)
        false
    }

    suspend fun deleteProfile(profile: Profile): Boolean = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            profileDao.delete(profile)
            true
        }.getOrElse { exception ->
            Timber.e(exception)
            false
        }
    }

    suspend fun addProfileShortcut(profile: Profile): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val intent = context
                    .packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        putExtra(INTENT_ACTION_SHORTCUT_PROFILE, profile.toPersistableBundle())
                    }
                    ?: return@suspendRunCatching false
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

    suspend fun deleteProfileShortcut(profile: Profile): Boolean = withContext(dispatcherIo) {
        val shortcut = listOf(profile.id.toString())
        coroutineContext.suspendRunCatching {
            ShortcutManagerCompat.removeDynamicShortcuts(context, shortcut)
            ShortcutManagerCompat.disableShortcuts(context, shortcut, "")
            true
        }.getOrElse { exception ->
            Timber.e(exception)
            false
        }
    }
}
