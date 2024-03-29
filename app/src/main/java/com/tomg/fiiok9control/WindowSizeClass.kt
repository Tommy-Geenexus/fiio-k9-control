/*
 * Copyright (c) 2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class WindowSizeClass : Parcelable {

    companion object {

        private const val WINDOW_MAX_HEIGHT_COMPACT = 480f
        private const val WINDOW_MAX_HEIGHT_MEDIUM = 900f
        private const val WINDOW_MAX_WIDTH_COMPACT = 600f
        private const val WINDOW_MAX_WIDTH_MEDIUM = 840f

        fun calculate(
            dp: Float,
            height: Boolean
        ): WindowSizeClass {
            return if (height) {
                when {
                    dp < WINDOW_MAX_HEIGHT_COMPACT -> Compact
                    dp < WINDOW_MAX_HEIGHT_MEDIUM -> Medium
                    else -> Expanded
                }
            } else {
                when {
                    dp < WINDOW_MAX_WIDTH_COMPACT -> Compact
                    dp < WINDOW_MAX_WIDTH_MEDIUM -> Medium
                    else -> Expanded
                }
            }
        }
    }

    @Parcelize
    data object Unspecified : WindowSizeClass()

    @Parcelize
    data object Compact : WindowSizeClass()

    @Parcelize
    data object Medium : WindowSizeClass()

    @Parcelize
    data object Expanded : WindowSizeClass()
}
