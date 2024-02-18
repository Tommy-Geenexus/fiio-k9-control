/*
 * Copyright (c) 2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package io.github.tommygeenexus.fiiok9control.core.ui.gaia

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.RemoteException
import io.github.tommygeenexus.fiiok9control.R
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_DOWN
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_MUTE
import io.github.tommygeenexus.fiiok9control.core.util.INTENT_ACTION_VOLUME_UP
import io.github.tommygeenexus.fiiok9control.core.util.TOP_LEVEL_PACKAGE_NAME
import timber.log.Timber

object GaiaGattServiceNotification {

    private const val REQUEST_CODE = 0
    private const val ID_NOTIFICATION = 1
    private const val ID_NOTIFICATION_CHANNEL = TOP_LEVEL_PACKAGE_NAME + "NOTIFICATION_CHANNEL"

    fun createNotificationChannel(context: Context): Result<Unit> {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return try {
            var channel = nm.getNotificationChannel(ID_NOTIFICATION_CHANNEL)
            if (channel != null) {
                return Result.success(Unit)
            }
            channel = NotificationChannel(
                ID_NOTIFICATION_CHANNEL,
                context.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
            Result.success(Unit)
        } catch (e: RemoteException) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    private fun build(
        context: Context,
        volume: Int,
        volumeRelative: Int,
        isMuteEnabled: Boolean
    ): Notification {
        return Notification
            .Builder(context, ID_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_k9)
            .setContentTitle(context.getString(R.string.fiio_k9))
            .setContentText(
                context.getString(
                    R.string.volume_level_value,
                    context.getString(R.string.volume_level, volume, volumeRelative)
                )
            )
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    REQUEST_CODE,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_volume_up),
                    context.getString(R.string.volume_up),
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_UP).apply {
                            setPackage(context.packageName)
                        },
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_volume_down),
                    context.getString(R.string.volume_down),
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_DOWN).apply {
                            setPackage(context.packageName)
                        },
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_volume_mute),
                    context.getString(
                        if (isMuteEnabled) {
                            R.string.volume_unmute
                        } else {
                            R.string.volume_mute
                        }
                    ),
                    PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        Intent(INTENT_ACTION_VOLUME_MUTE).apply {
                            setPackage(context.packageName)
                        },
                        PendingIntent.FLAG_IMMUTABLE
                    )
                ).build()
            )
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    fun GaiaGattService.startForeground(
        context: Context,
        volume: Int,
        volumeRelative: Int,
        isMuteEnabled: Boolean
    ) {
        startForeground(ID_NOTIFICATION, build(context, volume, volumeRelative, isMuteEnabled))
    }
}
