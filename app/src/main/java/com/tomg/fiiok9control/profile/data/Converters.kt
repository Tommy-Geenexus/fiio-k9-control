/*
 * Copyright (c) 2021-2022, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import androidx.room.TypeConverter
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource

class Converters {

    @TypeConverter
    fun fromInputSourceId(id: Int?): InputSource {
        return InputSource.findById(id ?: InputSource.Usb.id) ?: InputSource.Usb
    }

    @TypeConverter
    fun toInputSourceId(inputSource: InputSource) = inputSource.id

    @TypeConverter
    fun fromIndicatorStateId(id: Int?): IndicatorState {
        return IndicatorState.findById(id ?: IndicatorState.EnabledDefault.id)
            ?: IndicatorState.EnabledDefault
    }

    @TypeConverter
    fun toIndicatorStateId(indicatorState: IndicatorState) = indicatorState.id
}
