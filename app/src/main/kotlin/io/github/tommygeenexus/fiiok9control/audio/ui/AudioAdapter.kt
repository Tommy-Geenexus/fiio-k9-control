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

package io.github.tommygeenexus.fiiok9control.audio.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.BluetoothCodec
import io.github.tommygeenexus.fiiok9control.core.fiiok9.feature.LowPassFilter
import io.github.tommygeenexus.fiiok9control.databinding.ItemChannelBalanceBinding
import io.github.tommygeenexus.fiiok9control.databinding.ItemCodecsEnabledBinding
import io.github.tommygeenexus.fiiok9control.databinding.ItemLowPassFilterBinding

class AudioAdapter(
    private val listener: Listener,
    private val currentCodecsEnabled: () -> List<BluetoothCodec>,
    private val currentLowPassFilter: () -> LowPassFilter,
    private val currentChannelBalance: () -> Int,
    private val currentIsLoading: () -> Boolean,
    private val currentIsServiceConnected: () -> Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {

        fun onBluetoothCodecChanged(codec: BluetoothCodec, isEnabled: Boolean)
        fun onChannelBalanceRequested(channelBalance: Int)
        fun onLowPassFilterRequested(lowPassFilter: LowPassFilter)
    }

    private companion object {

        const val ITEM_VIEW_TYPE_CODECS_ENABLED = 0
        const val ITEM_VIEW_TYPE_FILTER = 1
        const val ITEM_CNT = 3
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_CODECS_ENABLED -> {
                ItemCodecsEnabledViewHolder(
                    binding = ItemCodecsEnabledBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            ITEM_VIEW_TYPE_FILTER -> {
                ItemLowPassFilterViewHolder(
                    binding = ItemLowPassFilterBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            else -> {
                ItemChannelBalanceViewHolder(
                    binding = ItemChannelBalanceBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (position) {
            ITEM_VIEW_TYPE_CODECS_ENABLED -> {
                (holder as? ItemCodecsEnabledViewHolder)?.bindItemCodecsEnabled(
                    codecsEnabled = currentCodecsEnabled(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
            ITEM_VIEW_TYPE_FILTER -> {
                (holder as? ItemLowPassFilterViewHolder)?.bindItemLowPassFilter(
                    lowPassFilter = currentLowPassFilter(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
            else -> {
                (holder as? ItemChannelBalanceViewHolder)?.bindItemChannelBalance(
                    channelBalance = currentChannelBalance(),
                    itemEnabled = currentIsServiceConnected() && !currentIsLoading()
                )
            }
        }
    }

    override fun getItemViewType(position: Int) = position

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = ITEM_CNT
}
