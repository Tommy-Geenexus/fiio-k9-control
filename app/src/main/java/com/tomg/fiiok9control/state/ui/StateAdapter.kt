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

package com.tomg.fiiok9control.state.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.databinding.ItemIndicatorBinding
import com.tomg.fiiok9control.databinding.ItemInputBinding
import com.tomg.fiiok9control.databinding.ItemMiscBinding
import com.tomg.fiiok9control.databinding.ItemVolumeBinding
import com.tomg.fiiok9control.state.IndicatorState
import com.tomg.fiiok9control.state.InputSource
import com.tomg.fiiok9control.toVolumePercent

class StateAdapter(
    private val listener: Listener,
    private val currentAudioFormat: () -> String,
    private val currentFirmwareVersion: () -> String,
    private val currentIndicatorBrightness: () -> Int,
    private val currentIndicatorState: () -> IndicatorState,
    private val currentInputSource: () -> InputSource,
    private val currentVolume: () -> Int,
    private val currentIsLoading: () -> Boolean,
    private val currentIsServiceConnected: () -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {

        fun onInputSourceRequested(inputSource: InputSource)
        fun onIndicatorStateRequested(indicatorState: IndicatorState)
        fun onIndicatorBrightnessRequested(indicatorBrightness: Int)
        fun onVolumeRequested(volume: Int)
    }

    private companion object {

        const val ITEM_VIEW_TYPE_MISC = 0
        const val ITEM_VIEW_TYPE_VOLUME = 1
        const val ITEM_VIEW_TYPE_INPUT = 2
        const val ITEM_CNT = 4
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_MISC -> {
                ItemMiscViewHolder(
                    binding = ItemMiscBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }
            ITEM_VIEW_TYPE_VOLUME -> {
                ItemVolumeViewHolder(
                    binding = ItemVolumeBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            ITEM_VIEW_TYPE_INPUT -> {
                ItemInputViewHolder(
                    binding = ItemInputBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            else -> {
                ItemIndicatorViewHolder(
                    binding = ItemIndicatorBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (position) {
            ITEM_VIEW_TYPE_MISC -> {
                (holder as? ItemMiscViewHolder)?.bindItemMisc(
                    fwVersion = currentFirmwareVersion(),
                    audioFmt = currentAudioFormat()
                )
            }
            ITEM_VIEW_TYPE_VOLUME -> {
                (holder as? ItemVolumeViewHolder)?.bindItemVolume(
                    volume = currentVolume(),
                    volumePercent = currentVolume().toVolumePercent(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
            ITEM_VIEW_TYPE_INPUT -> {
                (holder as? ItemInputViewHolder)?.bindItemInput(
                    inputSource = currentInputSource(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
            else -> {
                (holder as? ItemIndicatorViewHolder)?.bindItemIndicator(
                    indicatorState = currentIndicatorState(),
                    indicatorBrightness = currentIndicatorBrightness(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = ITEM_CNT
}
