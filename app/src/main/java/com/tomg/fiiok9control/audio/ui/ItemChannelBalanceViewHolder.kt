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

package com.tomg.fiiok9control.audio.ui

import androidx.recyclerview.widget.RecyclerView
import com.tomg.fiiok9control.Empty
import com.tomg.fiiok9control.R
import com.tomg.fiiok9control.databinding.ItemChannelBalanceBinding
import kotlin.math.absoluteValue

class ItemChannelBalanceViewHolder(
    private val binding: ItemChannelBalanceBinding,
    private val listener: AudioAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.channelBalanceSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                listener.onChannelBalanceRequested(value.toInt())
            }
        }
    }

    @Suppress("KotlinConstantConditions")
    fun bindItemChannelBalance(channelBalance: Int) {
        binding.channelBalanceDb.text = when {
            channelBalance < 0 -> {
                itemView.context.getString(
                    R.string.channel_balance_offset,
                    channelBalance.absoluteValue.toFloat() / 2,
                    itemView.context.getString(R.string.left)
                )
            }
            channelBalance > 0 -> {
                itemView.context.getString(
                    R.string.channel_balance_offset,
                    channelBalance.toFloat() / 2,
                    itemView.context.getString(R.string.right)
                )
            }
            else -> {
                itemView.context.getString(
                    R.string.channel_balance_offset,
                    channelBalance.toFloat(),
                    String.Empty
                )
            }
        }
        binding.channelBalanceSlider.value = channelBalance.toFloat()
    }
}
