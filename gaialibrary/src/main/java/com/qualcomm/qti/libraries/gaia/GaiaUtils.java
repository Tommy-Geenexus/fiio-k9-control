/*
 **************************************************************************************************
 * Copyright 2018 Qualcomm Technologies International, Ltd.                                       *
 **************************************************************************************************
 */

package com.qualcomm.qti.libraries.gaia;

/**
 * <p>This class contains all generic methods which can be re-used.</p>
 */
@SuppressWarnings("SameParameterValue")
public final class GaiaUtils {

    /**
     * <p>The number of bytes contained in a int.</p>
     */
    private static final int BYTES_IN_INT = 4;
    /**
     * <p>The number of bits contained in a byte.</p>
     */
    private static final int BITS_IN_BYTE = 8;

    /**
     * <p>This method allows retrieval of a human readable representation of an hexadecimal value contained in a
     * <code>int</code>.</p>
     *
     * @param i
     *         The <code>int</code> value.
     *
     * @return The hexadecimal value as a <code>String</code>.
     */
    public static String getHexadecimalStringFromInt(int i) {
        return String.format("%04X", i & 0xFFFF);
    }

    /**
     * Convert a byte array to a human readable String.
     *
     * @param value
     *         The byte array.
     *
     * @return String object containing values in byte array formatted as hex.
     */
    public static String getHexadecimalStringFromBytes(byte[] value) {
        if (value == null)
            return "null";
        final StringBuilder stringBuilder = new StringBuilder(value.length * 2);
        //noinspection ForLoopReplaceableByForEach // the for loop used less ressources than the foreach one.
        for (int i = 0; i < value.length; i++) {
            stringBuilder.append(String.format("0x%02x ", value[i]));
        }
        return stringBuilder.toString();
    }

    /**
     * <p>Extract an <code>int</code> value from a <code>bytes</code> array.</p>
     *
     * @param source
     *         The array to extract from.
     * @param offset
     *         Offset within source array.
     * @param length
     *         Number of bytes to use (maximum 4).
     * @param reverse
     *         True if bytes should be interpreted in reverse (little endian) order.
     *
     * @return The extracted <code>int</code>.
     */
    public static int extractIntFromByteArray(byte[] source, int offset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_INT)
            throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        int result = 0;
        int shift = (length - 1) * BITS_IN_BYTE;

        if (reverse) {
            for (int i = offset + length - 1; i >= offset; i--) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        } else {
            for (int i = offset; i < offset + length; i++) {
                result |= ((source[i] & 0xFF) << shift);
                shift -= BITS_IN_BYTE;
            }
        }
        return result;
    }

    /**
     * <p>This method allows copy of an int value into a byte array from the specified <code>offset</code> location to
     * the <code>offset + length</code> location.</p>
     *
     * @param sourceValue
     *         The <code>int</code> value to copy in the array.
     * @param target
     *         The <code>byte</code> array to copy in the <code>int</code> value.
     * @param targetOffset
     *         The targeted offset in the array to copy the first byte of the <code>int</code> value.
     * @param length
     *         The number of bytes in the array to copy the <code>int</code> value.
     * @param reverse
     *         True if bytes should be interpreted in reverse (little endian) order.
     */
    public static void copyIntIntoByteArray(int sourceValue, byte[] target, int targetOffset, int length, boolean reverse) {
        if (length < 0 | length > BYTES_IN_INT) {
            throw new IndexOutOfBoundsException("Length must be between 0 and " + BYTES_IN_INT);
        } else if (target.length < targetOffset + length) {
            throw new IndexOutOfBoundsException("The targeted location must be contained in the target array.");
        }

        if (reverse) {
            int shift = 0;
            int j = 0;
            for (int i = length - 1; i >= 0; i--) {
                int mask = 0xFF << shift;
                target[j + targetOffset] = (byte) ((sourceValue & mask) >> shift);
                shift += BITS_IN_BYTE;
                j++;
            }
        } else {
            int shift = (length - 1) * BITS_IN_BYTE;
            for (int i = 0; i < length; i++) {
                int mask = 0xFF << shift;
                target[i + targetOffset] = (byte) ((sourceValue & mask) >> shift);
                shift -= BITS_IN_BYTE;
            }
        }
    }

    /**
     * <p>To get a String label which corresponds to the given GAIA command.</p>
     * <p>The label is built as follows:
     * <ol>
     *     <Li>The value of the GAIA command as an hexadecimal given by {@link #getHexadecimalStringFromInt(int)
     *     getHexadecimalStringFromInt}.</Li>
     *     <li>The name of the GAIA command as defined in the protocol or <code>UNKNOWN</code> if the value cannot be
     *     matched with the known ones.</li>
     *     <li><i>Optional</i>: "(deprecated)" is the command had been deprecated.</li>
     * </ol></p>
     * <p>For instance, for the given value <code>384</code> the method will return <code>"0x0180
     * COMMAND_GET_CONFIGURATION_VERSION"</code>.</p>
     *
     * @param command
     *          The command to obtain a label for.
     *
     * @return the label corresponding to the given command.
     */
    @SuppressWarnings("deprecation")
    public static String getGAIACommandToString(int command) {
        String name = "UNKNOWN";
        String deprecated = "(deprecated)";
        name = switch (command) {
            case GAIA.COMMAND_SET_RAW_CONFIGURATION -> "COMMAND_SET_RAW_CONFIGURATION" + deprecated;
            case GAIA.COMMAND_GET_CONFIGURATION_VERSION -> "COMMAND_GET_CONFIGURATION_VERSION";
            case GAIA.COMMAND_SET_LED_CONFIGURATION -> "COMMAND_SET_LED_CONFIGURATION";
            case GAIA.COMMAND_GET_LED_CONFIGURATION -> "COMMAND_GET_LED_CONFIGURATION";
            case GAIA.COMMAND_SET_TONE_CONFIGURATION -> "COMMAND_SET_TONE_CONFIGURATION";
            case GAIA.COMMAND_GET_TONE_CONFIGURATION -> "COMMAND_GET_TONE_CONFIGURATION";
            case GAIA.COMMAND_SET_DEFAULT_VOLUME -> "COMMAND_SET_DEFAULT_VOLUME";
            case GAIA.COMMAND_GET_DEFAULT_VOLUME -> "COMMAND_GET_DEFAULT_VOLUME";
            case GAIA.COMMAND_FACTORY_DEFAULT_RESET -> "COMMAND_FACTORY_DEFAULT_RESET";
            case GAIA.COMMAND_GET_CONFIGURATION_ID -> "COMMAND_GET_CONFIGURATION_ID" + deprecated;
            case GAIA.COMMAND_SET_VIBRATOR_CONFIGURATION -> "COMMAND_SET_VIBRATOR_CONFIGURATION";
            case GAIA.COMMAND_GET_VIBRATOR_CONFIGURATION -> "COMMAND_GET_VIBRATOR_CONFIGURATION";
            case GAIA.COMMAND_SET_VOICE_PROMPT_CONFIGURATION ->
                    "COMMAND_SET_VOICE_PROMPT_CONFIGURATION";
            case GAIA.COMMAND_GET_VOICE_PROMPT_CONFIGURATION ->
                    "COMMAND_GET_VOICE_PROMPT_CONFIGURATION";
            case GAIA.COMMAND_SET_FEATURE_CONFIGURATION -> "COMMAND_SET_FEATURE_CONFIGURATION";
            case GAIA.COMMAND_GET_FEATURE_CONFIGURATION -> "COMMAND_GET_FEATURE_CONFIGURATION";
            case GAIA.COMMAND_SET_USER_EVENT_CONFIGURATION ->
                    "COMMAND_SET_USER_EVENT_CONFIGURATION";
            case GAIA.COMMAND_GET_USER_EVENT_CONFIGURATION ->
                    "COMMAND_GET_USER_EVENT_CONFIGURATION";
            case GAIA.COMMAND_SET_TIMER_CONFIGURATION -> "COMMAND_SET_TIMER_CONFIGURATION";
            case GAIA.COMMAND_GET_TIMER_CONFIGURATION -> "COMMAND_GET_TIMER_CONFIGURATION";
            case GAIA.COMMAND_SET_AUDIO_GAIN_CONFIGURATION ->
                    "COMMAND_SET_AUDIO_GAIN_CONFIGURATION";
            case GAIA.COMMAND_GET_AUDIO_GAIN_CONFIGURATION ->
                    "COMMAND_GET_AUDIO_GAIN_CONFIGURATION";
            case GAIA.COMMAND_SET_VOLUME_CONFIGURATION -> "COMMAND_SET_VOLUME_CONFIGURATION";
            case GAIA.COMMAND_GET_VOLUME_CONFIGURATION -> "COMMAND_GET_VOLUME_CONFIGURATION";
            case GAIA.COMMAND_SET_POWER_CONFIGURATION -> "COMMAND_SET_POWER_CONFIGURATION";
            case GAIA.COMMAND_GET_POWER_CONFIGURATION -> "COMMAND_GET_POWER_CONFIGURATION";
            case GAIA.COMMAND_SET_USER_TONE_CONFIGURATION -> "COMMAND_SET_USER_TONE_CONFIGURATION";
            case GAIA.COMMAND_GET_USER_TONE_CONFIGURATION -> "COMMAND_GET_USER_TONE_CONFIGURATION";
            case GAIA.COMMAND_SET_DEVICE_NAME -> "COMMAND_SET_DEVICE_NAME";
            case GAIA.COMMAND_GET_DEVICE_NAME -> "COMMAND_GET_DEVICE_NAME";
            case GAIA.COMMAND_SET_WLAN_CREDENTIALS -> "COMMAND_SET_WLAN_CREDENTIALS";
            case GAIA.COMMAND_GET_WLAN_CREDENTIALS -> "COMMAND_GET_WLAN_CREDENTIALS";
            case GAIA.COMMAND_SET_PEER_PERMITTED_ROUTING -> "COMMAND_SET_PEER_PERMITTED_ROUTING";
            case GAIA.COMMAND_GET_PEER_PERMITTED_ROUTING -> "COMMAND_GET_PEER_PERMITTED_ROUTING";
            case GAIA.COMMAND_SET_PERMITTED_NEXT_AUDIO_SOURCE ->
                    "COMMAND_SET_PERMITTED_NEXT_AUDIO_SOURCE";
            case GAIA.COMMAND_GET_PERMITTED_NEXT_AUDIO_SOURCE ->
                    "COMMAND_GET_PERMITTED_NEXT_AUDIO_SOURCE";
            case GAIA.COMMAND_SET_ONE_TOUCH_DIAL_STRING -> "COMMAND_SET_ONE_TOUCH_DIAL_STRING";
            case GAIA.COMMAND_GET_ONE_TOUCH_DIAL_STRING -> "COMMAND_GET_ONE_TOUCH_DIAL_STRING";
            case GAIA.COMMAND_GET_MOUNTED_PARTITIONS -> "COMMAND_GET_MOUNTED_PARTITIONS";
            case GAIA.COMMAND_SET_DFU_PARTITION -> "COMMAND_SET_DFU_PARTITION";
            case GAIA.COMMAND_GET_DFU_PARTITION -> "COMMAND_GET_DFU_PARTITION";
            case GAIA.COMMAND_CHANGE_VOLUME -> "COMMAND_CHANGE_VOLUME";
            case GAIA.COMMAND_DEVICE_RESET -> "COMMAND_DEVICE_RESET";
            case GAIA.COMMAND_GET_BOOT_MODE -> "COMMAND_GET_BOOT_MODE";
            case GAIA.COMMAND_SET_PIO_CONTROL -> "COMMAND_SET_PIO_CONTROL";
            case GAIA.COMMAND_GET_PIO_CONTROL -> "COMMAND_GET_PIO_CONTROL";
            case GAIA.COMMAND_SET_POWER_STATE -> "COMMAND_SET_POWER_STATE";
            case GAIA.COMMAND_GET_POWER_STATE -> "COMMAND_GET_POWER_STATE";
            case GAIA.COMMAND_SET_VOLUME_ORIENTATION -> "COMMAND_SET_VOLUME_ORIENTATION";
            case GAIA.COMMAND_GET_VOLUME_ORIENTATION -> "COMMAND_GET_VOLUME_ORIENTATION";
            case GAIA.COMMAND_SET_VIBRATOR_CONTROL -> "COMMAND_SET_VIBRATOR_CONTROL";
            case GAIA.COMMAND_GET_VIBRATOR_CONTROL -> "COMMAND_GET_VIBRATOR_CONTROL";
            case GAIA.COMMAND_SET_LED_CONTROL -> "COMMAND_SET_LED_CONTROL";
            case GAIA.COMMAND_GET_LED_CONTROL -> "COMMAND_GET_LED_CONTROL";
            case GAIA.COMMAND_FM_CONTROL -> "COMMAND_FM_CONTROL";
            case GAIA.COMMAND_PLAY_TONE -> "COMMAND_PLAY_TONE";
            case GAIA.COMMAND_SET_VOICE_PROMPT_CONTROL -> "COMMAND_SET_VOICE_PROMPT_CONTROL";
            case GAIA.COMMAND_GET_VOICE_PROMPT_CONTROL -> "COMMAND_GET_VOICE_PROMPT_CONTROL";
            case GAIA.COMMAND_CHANGE_AUDIO_PROMPT_LANGUAGE ->
                    "COMMAND_CHANGE_AUDIO_PROMPT_LANGUAGE";
            case GAIA.COMMAND_SET_SPEECH_RECOGNITION_CONTROL ->
                    "COMMAND_SET_SPEECH_RECOGNITION_CONTROL";
            case GAIA.COMMAND_GET_SPEECH_RECOGNITION_CONTROL ->
                    "COMMAND_GET_SPEECH_RECOGNITION_CONTROL";
            case GAIA.COMMAND_ALERT_LEDS -> "COMMAND_ALERT_LEDS";
            case GAIA.COMMAND_ALERT_TONE -> "COMMAND_ALERT_TONE";
            case GAIA.COMMAND_ALERT_EVENT -> "COMMAND_ALERT_EVENT";
            case GAIA.COMMAND_ALERT_VOICE -> "COMMAND_ALERT_VOICE";
            case GAIA.COMMAND_SET_AUDIO_PROMPT_LANGUAGE -> "COMMAND_SET_AUDIO_PROMPT_LANGUAGE";
            case GAIA.COMMAND_GET_AUDIO_PROMPT_LANGUAGE -> "COMMAND_GET_AUDIO_PROMPT_LANGUAGE";
            case GAIA.COMMAND_START_SPEECH_RECOGNITION -> "COMMAND_START_SPEECH_RECOGNITION";
            case GAIA.COMMAND_SET_EQ_CONTROL -> "COMMAND_SET_EQ_CONTROL";
            case GAIA.COMMAND_GET_EQ_CONTROL -> "COMMAND_GET_EQ_CONTROL";
            case GAIA.COMMAND_SET_BASS_BOOST_CONTROL -> "COMMAND_SET_BASS_BOOST_CONTROL";
            case GAIA.COMMAND_GET_BASS_BOOST_CONTROL -> "COMMAND_GET_BASS_BOOST_CONTROL";
            case GAIA.COMMAND_SET_3D_ENHANCEMENT_CONTROL -> "COMMAND_SET_3D_ENHANCEMENT_CONTROL";
            case GAIA.COMMAND_GET_3D_ENHANCEMENT_CONTROL -> "COMMAND_GET_3D_ENHANCEMENT_CONTROL";
            case GAIA.COMMAND_SWITCH_EQ_CONTROL -> "COMMAND_SWITCH_EQ_CONTROL";
            case GAIA.COMMAND_TOGGLE_BASS_BOOST_CONTROL -> "COMMAND_TOGGLE_BASS_BOOST_CONTROL";
            case GAIA.COMMAND_TOGGLE_3D_ENHANCEMENT_CONTROL ->
                    "COMMAND_TOGGLE_3D_ENHANCEMENT_CONTROL";
            case GAIA.COMMAND_SET_EQ_PARAMETER -> "COMMAND_SET_EQ_PARAMETER";
            case GAIA.COMMAND_GET_EQ_PARAMETER -> "COMMAND_GET_EQ_PARAMETER";
            case GAIA.COMMAND_SET_EQ_GROUP_PARAMETER -> "COMMAND_SET_EQ_GROUP_PARAMETER";
            case GAIA.COMMAND_GET_EQ_GROUP_PARAMETER -> "COMMAND_GET_EQ_GROUP_PARAMETER";
            case GAIA.COMMAND_DISPLAY_CONTROL -> "COMMAND_DISPLAY_CONTROL";
            case GAIA.COMMAND_ENTER_BLUETOOTH_PAIRING_MODE ->
                    "COMMAND_ENTER_BLUETOOTH_PAIRING_MODE";
            case GAIA.COMMAND_SET_AUDIO_SOURCE -> "COMMAND_SET_AUDIO_SOURCE";
            case GAIA.COMMAND_GET_AUDIO_SOURCE -> "COMMAND_GET_AUDIO_SOURCE";
            case GAIA.COMMAND_AV_REMOTE_CONTROL -> "COMMAND_AV_REMOTE_CONTROL";
            case GAIA.COMMAND_SET_USER_EQ_CONTROL -> "COMMAND_SET_USER_EQ_CONTROL";
            case GAIA.COMMAND_GET_USER_EQ_CONTROL -> "COMMAND_GET_USER_EQ_CONTROL";
            case GAIA.COMMAND_TOGGLE_USER_EQ_CONTROL -> "COMMAND_TOGGLE_USER_EQ_CONTROL";
            case GAIA.COMMAND_SET_SPEAKER_EQ_CONTROL -> "COMMAND_SET_SPEAKER_EQ_CONTROL";
            case GAIA.COMMAND_GET_SPEAKER_EQ_CONTROL -> "COMMAND_GET_SPEAKER_EQ_CONTROL";
            case GAIA.COMMAND_TOGGLE_SPEAKER_EQ_CONTROL -> "COMMAND_TOGGLE_SPEAKER_EQ_CONTROL";
            case GAIA.COMMAND_SET_TWS_AUDIO_ROUTING -> "COMMAND_SET_TWS_AUDIO_ROUTING";
            case GAIA.COMMAND_GET_TWS_AUDIO_ROUTING -> "COMMAND_GET_TWS_AUDIO_ROUTING";
            case GAIA.COMMAND_SET_TWS_VOLUME -> "COMMAND_SET_TWS_VOLUME";
            case GAIA.COMMAND_GET_TWS_VOLUME -> "COMMAND_GET_TWS_VOLUME";
            case GAIA.COMMAND_TRIM_TWS_VOLUME -> "COMMAND_TRIM_TWS_VOLUME";
            case GAIA.COMMAND_SET_PEER_LINK_RESERVED -> "COMMAND_SET_PEER_LINK_RESERVED";
            case GAIA.COMMAND_GET_PEER_LINK_RESERVED -> "COMMAND_GET_PEER_LINK_RESERVED";
            case GAIA.COMMAND_TWS_PEER_START_ADVERTISING -> "COMMAND_TWS_PEER_START_ADVERTISING";
            case GAIA.COMMAND_FIND_MY_REMOTE -> "COMMAND_FIND_MY_REMOTE";
            case GAIA.COMMAND_SET_CODEC -> "COMMAND_SET_CODEC";
            case GAIA.COMMAND_GET_CODEC -> "COMMAND_GET_CODEC";
            case GAIA.COMMAND_SET_SUPPORTED_FEATURES -> "COMMAND_SET_SUPPORTED_FEATURES";
            case GAIA.COMMAND_DISCONNECT -> "COMMAND_DISCONNECT";
            case GAIA.COMMAND_GET_API_VERSION -> "COMMAND_GET_API_VERSION";
            case GAIA.COMMAND_GET_CURRENT_RSSI -> "COMMAND_GET_CURRENT_RSSI";
            case GAIA.COMMAND_GET_CURRENT_BATTERY_LEVEL -> "COMMAND_GET_CURRENT_BATTERY_LEVEL";
            case GAIA.COMMAND_GET_MODULE_ID -> "COMMAND_GET_MODULE_ID";
            case GAIA.COMMAND_GET_APPLICATION_VERSION -> "COMMAND_GET_APPLICATION_VERSION";
            case GAIA.COMMAND_GET_PIO_STATE -> "COMMAND_GET_PIO_STATE";
            case GAIA.COMMAND_READ_ADC -> "COMMAND_READ_ADC";
            case GAIA.COMMAND_GET_PEER_ADDRESS -> "COMMAND_GET_PEER_ADDRESS";
            case GAIA.COMMAND_GET_DFU_STATUS -> "COMMAND_GET_DFU_STATUS" + deprecated;
            case GAIA.COMMAND_GET_HOST_FEATURE_INFORMATION ->
                    "COMMAND_GET_HOST_FEATURE_INFORMATION";
            case GAIA.COMMAND_GET_AUTH_BITMAPS -> "COMMAND_GET_AUTH_BITMAPS";
            case GAIA.COMMAND_AUTHENTICATE_REQUEST -> "COMMAND_AUTHENTICATE_REQUEST";
            case GAIA.COMMAND_AUTHENTICATE_RESPONSE -> "COMMAND_AUTHENTICATE_RESPONSE";
            case GAIA.COMMAND_SET_FEATURE -> "COMMAND_SET_FEATURE";
            case GAIA.COMMAND_GET_FEATURE -> "COMMAND_GET_FEATURE";
            case GAIA.COMMAND_SET_SESSION_ENABLE -> "COMMAND_SET_SESSION_ENABLE";
            case GAIA.COMMAND_GET_SESSION_ENABLE -> "COMMAND_GET_SESSION_ENABLE";
            case GAIA.COMMAND_DATA_TRANSFER_SETUP -> "COMMAND_DATA_TRANSFER_SETUP";
            case GAIA.COMMAND_DATA_TRANSFER_CLOSE -> "COMMAND_DATA_TRANSFER_CLOSE";
            case GAIA.COMMAND_HOST_TO_DEVICE_DATA -> "COMMAND_HOST_TO_DEVICE_DATA";
            case GAIA.COMMAND_DEVICE_TO_HOST_DATA -> "COMMAND_DEVICE_TO_HOST_DATA";
            case GAIA.COMMAND_I2C_TRANSFER -> "COMMAND_I2C_TRANSFER";
            case GAIA.COMMAND_GET_STORAGE_PARTITION_STATUS ->
                    "COMMAND_GET_STORAGE_PARTITION_STATUS";
            case GAIA.COMMAND_OPEN_STORAGE_PARTITION -> "COMMAND_OPEN_STORAGE_PARTITION";
            case GAIA.COMMAND_OPEN_UART -> "COMMAND_OPEN_UART";
            case GAIA.COMMAND_WRITE_STORAGE_PARTITION -> "COMMAND_WRITE_STORAGE_PARTITION";
            case GAIA.COMMAND_WRITE_STREAM -> "COMMAND_WRITE_STREAM";
            case GAIA.COMMAND_CLOSE_STORAGE_PARTITION -> "COMMAND_CLOSE_STORAGE_PARTITION";
            case GAIA.COMMAND_MOUNT_STORAGE_PARTITION -> "COMMAND_MOUNT_STORAGE_PARTITION";
            case GAIA.COMMAND_GET_FILE_STATUS -> "COMMAND_GET_FILE_STATUS";
            case GAIA.COMMAND_OPEN_FILE -> "COMMAND_OPEN_FILE";
            case GAIA.COMMAND_READ_FILE -> "COMMAND_READ_FILE";
            case GAIA.COMMAND_CLOSE_FILE -> "COMMAND_CLOSE_FILE";
            case GAIA.COMMAND_DFU_REQUEST -> "COMMAND_DFU_REQUEST";
            case GAIA.COMMAND_DFU_BEGIN -> "COMMAND_DFU_BEGIN";
            case GAIA.COMMAND_DFU_WRITE -> "COMMAND_DFU_WRITE";
            case GAIA.COMMAND_DFU_COMMIT -> "COMMAND_DFU_COMMIT";
            case GAIA.COMMAND_DFU_GET_RESULT -> "COMMAND_DFU_GET_RESULT";
            case GAIA.COMMAND_VM_UPGRADE_CONNECT -> "COMMAND_VM_UPGRADE_CONNECT";
            case GAIA.COMMAND_VM_UPGRADE_DISCONNECT -> "COMMAND_VM_UPGRADE_DISCONNECT";
            case GAIA.COMMAND_VM_UPGRADE_CONTROL -> "COMMAND_VM_UPGRADE_CONTROL";
            case GAIA.COMMAND_VM_UPGRADE_DATA -> "COMMAND_VM_UPGRADE_DATA";
            case GAIA.COMMAND_NO_OPERATION -> "COMMAND_NO_OPERATION";
            case GAIA.COMMAND_GET_DEBUG_FLAGS -> "COMMAND_GET_DEBUG_FLAGS";
            case GAIA.COMMAND_SET_DEBUG_FLAGS -> "COMMAND_SET_DEBUG_FLAGS";
            case GAIA.COMMAND_RETRIEVE_PS_KEY -> "COMMAND_RETRIEVE_PS_KEY";
            case GAIA.COMMAND_RETRIEVE_FULL_PS_KEY -> "COMMAND_RETRIEVE_FULL_PS_KEY";
            case GAIA.COMMAND_STORE_PS_KEY -> "COMMAND_STORE_PS_KEY";
            case GAIA.COMMAND_FLOOD_PS -> "COMMAND_FLOOD_PS";
            case GAIA.COMMAND_STORE_FULL_PS_KEY -> "COMMAND_STORE_FULL_PS_KEY";
            case GAIA.COMMAND_SEND_DEBUG_MESSAGE -> "COMMAND_SEND_DEBUG_MESSAGE";
            case GAIA.COMMAND_SEND_APPLICATION_MESSAGE -> "COMMAND_SEND_APPLICATION_MESSAGE";
            case GAIA.COMMAND_SEND_KALIMBA_MESSAGE -> "COMMAND_SEND_KALIMBA_MESSAGE";
            case GAIA.COMMAND_GET_MEMORY_SLOTS -> "COMMAND_GET_MEMORY_SLOTS";
            case GAIA.COMMAND_GET_DEBUG_VARIABLE -> "COMMAND_GET_DEBUG_VARIABLE";
            case GAIA.COMMAND_SET_DEBUG_VARIABLE -> "COMMAND_SET_DEBUG_VARIABLE";
            case GAIA.COMMAND_DELETE_PDL -> "COMMAND_DELETE_PDL";
            case GAIA.COMMAND_SET_BLE_CONNECTION_PARAMETERS ->
                    "COMMAND_SET_BLE_CONNECTION_PARAMETERS";
            case GAIA.COMMAND_IVOR_ANSWER_END -> "COMMAND_IVOR_ANSWER_END";
            case GAIA.COMMAND_IVOR_ANSWER_START -> "COMMAND_IVOR_ANSWER_START";
            case GAIA.COMMAND_IVOR_CANCEL -> "COMMAND_IVOR_CANCEL";
            case GAIA.COMMAND_IVOR_CHECK_VERSION -> "COMMAND_IVOR_CHECK_VERSION";
            case GAIA.COMMAND_IVOR_PING -> "COMMAND_IVOR_PING";
            case GAIA.COMMAND_IVOR_START -> "COMMAND_IVOR_START";
            case GAIA.COMMAND_IVOR_VOICE_DATA -> "COMMAND_IVOR_VOICE_DATA";
            case GAIA.COMMAND_IVOR_VOICE_DATA_REQUEST -> "COMMAND_IVOR_VOICE_DATA_REQUEST";
            case GAIA.COMMAND_IVOR_VOICE_END -> "COMMAND_IVOR_VOICE_END";
            case GAIA.COMMAND_REGISTER_NOTIFICATION -> "COMMAND_REGISTER_NOTIFICATION";
            case GAIA.COMMAND_GET_NOTIFICATION -> "COMMAND_GET_NOTIFICATION";
            case GAIA.COMMAND_CANCEL_NOTIFICATION -> "COMMAND_CANCEL_NOTIFICATION";
            case GAIA.COMMAND_EVENT_NOTIFICATION -> "COMMAND_EVENT_NOTIFICATION";
            default -> name;
        };

        return getHexadecimalStringFromInt(command) + " " + name;
    }
}
