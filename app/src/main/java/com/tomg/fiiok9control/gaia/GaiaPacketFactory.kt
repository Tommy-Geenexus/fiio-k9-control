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

package com.tomg.fiiok9control.gaia

import com.qualcomm.qti.libraries.gaia.packets.GaiaPacketBLE

object GaiaPacketFactory {

    const val VENDOR_ID_FIIO = 0x0a

    const val CMD_ID_AUDIO_FMT_GET = 0x044c
    const val CMD_ID_CHANNEL_BAL_GET = 0x0413
    const val CMD_ID_CHANNEL_BAL_SET = 0x0403
    const val CMD_ID_CODEC_BIT_GET = 0x0416
    const val CMD_ID_CODEC_EN_GET = 0x0417
    const val CMD_ID_CODEC_EN_SET = 0x0407
    const val CMD_ID_EQ_EN_GET = 0x0419
    const val CMD_ID_EQ_EN_SET = 0x0408
    const val CMD_ID_EQ_PRE_GET = 0x0435
    const val CMD_ID_EQ_PRE_SET = 0x0423
    const val CMD_ID_INDICATOR_RGB_LIGHTING_GET = 0x043d
    const val CMD_ID_INDICATOR_RGB_LIGHTING_SET = 0x043e
    const val CMD_ID_INPUT_SRC_GET = 0x0448
    const val CMD_ID_INPUT_SRC_SET = 0x0449
    const val CMD_ID_LOW_PASS_FILTER_GET = 0x0411
    const val CMD_ID_LOW_PASS_FILTER_SET = 0x0401
    const val CMD_ID_MQA_EN_GET = 0x044F
    const val CMD_ID_MQA_EN_SET = 0x0450
    const val CMD_ID_MUTE_EN_GET = 0x044a
    const val CMD_ID_MUTE_EN_SET = 0x044b
    const val CMD_ID_RESTORE_SET = 0x0404
    const val CMD_ID_STANDBY_SET = 0x0425
    const val CMD_ID_VERSION_GET = 0x0418
    const val CMD_ID_VOLUME_GET = 0x0412

    fun createGaiaPacket(
        vendorId: Int = VENDOR_ID_FIIO,
        commandId: Int,
        payload: ByteArray = byteArrayOf()
    ) = GaiaPacketBLE(vendorId, commandId, payload)
}

fun GaiaPacketBLE.isFiioPacket() = vendorId == GaiaPacketFactory.VENDOR_ID_FIIO
