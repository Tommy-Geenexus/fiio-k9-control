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

import android.os.Parcelable
import android.os.PersistableBundle
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomg.fiiok9control.Empty
import com.tomg.fiiok9control.TOP_LEVEL_PACKAGE_NAME
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.state.orDefault
import kotlinx.parcelize.Parcelize

@Keep
@Entity
@Parcelize
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = String.Empty,
    @ColumnInfo(name = "input_source") val inputSource: InputSource = InputSource.Usb,
    @ColumnInfo(name = "indicator_state")
    val indicatorState: IndicatorState = IndicatorState.EnabledDefault,
    @ColumnInfo(name = "indicator_brightness") val indicatorBrightness: Int = 5,
    val volume: Int = 0
) : Parcelable {

    companion object {

        private const val KEY_ID = TOP_LEVEL_PACKAGE_NAME + "ID"
        private const val KEY_NAME = TOP_LEVEL_PACKAGE_NAME + "NAME"
        private const val KEY_INPUT_SRC = TOP_LEVEL_PACKAGE_NAME + "INPUT_SRC"
        private const val KEY_INDICATOR_STATE = TOP_LEVEL_PACKAGE_NAME + "INDICATOR_STATE"
        private const val KEY_INDICATOR_BRIGHTNESS = TOP_LEVEL_PACKAGE_NAME + "INDICATOR_BRIGHTNESS"
        private const val KEY_VOLUME = TOP_LEVEL_PACKAGE_NAME + "VOLUME"

        fun createFromPersistableBundle(bundle: PersistableBundle): Profile {
            return Profile(
                id = bundle.getLong(KEY_ID),
                name = bundle.getString(KEY_NAME).orEmpty(),
                inputSource = InputSource.findById(bundle.getInt(KEY_INPUT_SRC)).orDefault(),
                indicatorState = IndicatorState
                    .findById(bundle.getInt(KEY_INDICATOR_STATE))
                    .orDefault(),
                indicatorBrightness = bundle.getInt(KEY_INDICATOR_BRIGHTNESS),
                volume = bundle.getInt(KEY_VOLUME)
            )
        }
    }

    fun toPersistableBundle(): PersistableBundle {
        return PersistableBundle().apply {
            putLong(KEY_ID, id)
            putString(KEY_NAME, name)
            putInt(KEY_INPUT_SRC, inputSource.id)
            putInt(KEY_INDICATOR_STATE, indicatorState.id)
            putInt(KEY_INDICATOR_BRIGHTNESS, indicatorBrightness)
            putInt(KEY_VOLUME, volume)
        }
    }
}
