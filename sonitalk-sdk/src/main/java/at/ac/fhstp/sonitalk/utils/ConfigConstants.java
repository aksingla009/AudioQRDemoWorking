package at.ac.fhstp.sonitalk.utils;

/**
 * Set of constants used e.g. in shared preferences, or as keys for the JSON configuration files.
 * Created by Otakusarea on 27.11.2018.
 */

public class ConfigConstants {
    /*
     * *** WARNING ***
     * The following String values are used as keys for the json configuration files.
     * Please do not modify, or if you do, keep everything in sync.
     */
    public static final String FREQUENCY_ZERO = "frequency-zero"; // Lowest frequency used in Hz
    public static final String BIT_PERIOD = "bit-period"; // Bit duration in ms
    public static final String PAUSE_PERIOD = "pause-period"; // Pause duration in ms
    public static final String NUMBER_OF_MESSAGE_BLOCKS = "number-of-message-blocks";
    public static final String NUMBER_OF_FREQUENCIES = "number-of-frequencies";
    public static final String SPACE_BETWEEN_FREQUENCIES = "space-between-frequencies"; // In Hz
    /*** ***/

    /* NOT used in this version.
    public static final String NUMBER_OF_MAX_CHARACTERS = "number-of-max-characters";
    public static final String NUMBER_OF_PARITY_BYTES = "number-of-parity-bytes";
    // Should these be moved to an app specific class (currently used from the MainActivity to store UI content in SharedPreferences) ?
    public static final String FREQUENCY_OFFSET_FOR_SPECTROGRAM = "frequency-offset-for-spectrogram"; //Used in Decoder demo MainActivity
    public static final String STEPFACTOR = "stepfactor"; //Used in Decoder demo MainActivity
    public static final String SILENT_MODE = "silent-mode"; //Used in Decoder demo MainActivity
    public static final String SIGNAL_TEXT = "signal-text"; //Used in Encoder demo MainActivity
    // Remove, always false
    public static final String IS_PAUSE_ACTIVE = "is-pause-active"; //Used in Encoder demo MainActivity
*/
    public static final byte[] GENERATOR_POLYNOM = new byte[] { 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1}; //CRC-17-CAN : 0x1685B
    public static final String CONTROL_FILLING_CHARACTER = "00011001";
}
