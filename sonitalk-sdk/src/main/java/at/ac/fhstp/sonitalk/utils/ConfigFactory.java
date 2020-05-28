/*
 * Copyright (c) 2019. Alexis Ringot, Florian Taurer, Matthias Zeppelzauer.
 *
 * This file is part of SoniTalk Android SDK.
 *
 * SoniTalk Android SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SoniTalk Android SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SoniTalk Android SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.ac.fhstp.sonitalk.utils;

import android.content.Context;
import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import at.ac.fhstp.sonitalk.SoniTalkConfig;
import at.ac.fhstp.sonitalk.exceptions.ConfigException;

/**
 * Utility class for building SoniTalkConfig objects from JSON files.
 */
public final class ConfigFactory {
    private static final String TAG = ConfigFactory.class.getSimpleName();

    private static final String DEFAULT_PROFILE_FILENAME = "default_config.json";

    public static SoniTalkConfig getDefaultConfig(Context context) throws IOException, ConfigException {
        return loadFromJson(DEFAULT_PROFILE_FILENAME, context);
    }

    public static SoniTalkConfig loadFromJson(String filename, Context context) throws IOException, ConfigException {
        InputStream is = context.getAssets().open("configs/"+filename);
        try (JsonReader reader = new JsonReader(new InputStreamReader(is, "UTF-8"))) {
            return readConfig(reader);
        }
    }
    private static SoniTalkConfig readConfig(JsonReader reader) throws IOException, ConfigException {
        int f0 = -1;
        int bitperiod = -1;
        int pauseperiod = -1;
        int nMessageBlocks = -1;
        int nFrequencies = -1;
        int frequencySpace = -1;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case ConfigConstants.FREQUENCY_ZERO:
                    f0 = reader.nextInt();
                    break;
                case ConfigConstants.BIT_PERIOD:
                    bitperiod = reader.nextInt();
                    break;
                case ConfigConstants.PAUSE_PERIOD:
                    pauseperiod = reader.nextInt();
                    break;
                case ConfigConstants.NUMBER_OF_MESSAGE_BLOCKS:
                    nMessageBlocks = reader.nextInt();
                    break;
                case ConfigConstants.NUMBER_OF_FREQUENCIES:
                    nFrequencies = reader.nextInt();
                    break;
                case ConfigConstants.SPACE_BETWEEN_FREQUENCIES:
                    frequencySpace = reader.nextInt();
                    break;
                default:
                    Log.d(TAG, "Config file contains an unknown field: " + name);
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();

        if (f0 == -1 || bitperiod == -1 || pauseperiod == -1 || nMessageBlocks == -1
                || nFrequencies == -1 || frequencySpace == -1) {
            throw new ConfigException("The configuration file does not match the required format.");
        }

        return new SoniTalkConfig(f0, bitperiod, pauseperiod, nMessageBlocks, nFrequencies, frequencySpace);
    }
}
