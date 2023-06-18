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

package com.tomg.fiiok9control

const val TOP_LEVEL_PACKAGE_NAME = "com.tomg.fiiok9control."

const val REQUEST_CODE = 0
const val ID_NOTIFICATION = 1
const val ID_NOTIFICATION_CHANNEL = TOP_LEVEL_PACKAGE_NAME + "NOTIFICATION_CHANNEL"

const val KEY_BLUETOOTH_ENABLED = TOP_LEVEL_PACKAGE_NAME + "BLUETOOTH_ENABLED"
const val KEY_SERVICE_CONNECTED = TOP_LEVEL_PACKAGE_NAME + "SERVICE_CONNECTED"
const val KEY_SHORTCUT_PROFILE = TOP_LEVEL_PACKAGE_NAME + "SHORTCUT_PROFILE"
const val KEY_PROFILE_NAME = TOP_LEVEL_PACKAGE_NAME + "PROFILE_NAME"
const val KEY_PROFILE_VOLUME_EXPORT = TOP_LEVEL_PACKAGE_NAME + "VOLUME_EXPORT"

const val INTENT_ACTION_SHORTCUT_PROFILE = TOP_LEVEL_PACKAGE_NAME + "SHORTCUT_PROFILE"
const val INTENT_ACTION_VOLUME_UP = TOP_LEVEL_PACKAGE_NAME + "VOLUME_UP"
const val INTENT_ACTION_VOLUME_DOWN = TOP_LEVEL_PACKAGE_NAME + "VOLUME_DOWN"
const val INTENT_ACTION_VOLUME_MUTE = TOP_LEVEL_PACKAGE_NAME + "VOLUME_MUTE"

const val INTENT_EXTRA_CONSUMED = TOP_LEVEL_PACKAGE_NAME + "EXTRA_CONSUMED"

const val DEVICE_K9_PRO_NAME = "FiiO K9 Pro"

const val GAIA_CMD_DELAY_MS = 200L

const val VOLUME_MIN = 0
const val VOLUME_MAX = 120

@Suppress("SameReturnValue")
val String.Companion.Empty: String get() = ""
