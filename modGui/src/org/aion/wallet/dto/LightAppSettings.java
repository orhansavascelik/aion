/*
 * Copyright (c) 2017-2018 Aion foundation.
 *
 *     This file is part of the aion network project.
 *
 *     The aion network project is free software: you can redistribute it
 *     and/or modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     The aion network project is distributed in the hope that it will
 *     be useful, but WITHOUT ANY WARRANTY; without even the implied
 *     warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *     See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the aion network project source files.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Aion foundation.
 */
package org.aion.wallet.dto;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import org.aion.gui.model.ApiType;
import org.aion.wallet.exception.ValidationException;

public class LightAppSettings {
    private static final Integer DEFAULT_LOCK_TIMEOUT = 3;
    private static final String DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT = "minutes";

    private static final String ADDRESS = ".address";
    private static final String PORT = ".port";
    private static final String PROTOCOL = ".protocol";
    private static final String ACCOUNTS = "accounts";

    private static final String DEFAULT_IP = "127.0.0.1";
    private static final String DEFAULT_PORT = "8547";
    private static final String DEFAULT_PROTOCOL = "tcp";
    private static final String LOCK_TIMEOUT = ".lock_timeout";
    private static final String LOCK_TIMEOUT_MEASUREMENT_UNIT = ".lock_timeout_measurement_unit";

    private final ApiType type;
    private final String address;
    private final String port;
    private final String protocol;
    private final Integer lockTimeout;
    private final String lockTimeoutMeasurementUnit;

    public LightAppSettings(final Properties lightSettingsProps, final ApiType type) {
        this.type = type;
        address =
                Optional.ofNullable(lightSettingsProps.getProperty(type + ADDRESS))
                        .orElse(DEFAULT_IP);
        port =
                Optional.ofNullable(lightSettingsProps.getProperty(type + PORT))
                        .orElse(DEFAULT_PORT);
        protocol =
                Optional.ofNullable(lightSettingsProps.getProperty(type + PROTOCOL))
                        .orElse(DEFAULT_PROTOCOL);
        lockTimeout =
                Integer.parseInt(
                        Optional.ofNullable(lightSettingsProps.getProperty(ACCOUNTS + LOCK_TIMEOUT))
                                .orElse(DEFAULT_LOCK_TIMEOUT.toString()));
        lockTimeoutMeasurementUnit =
                Optional.ofNullable(
                                lightSettingsProps.getProperty(
                                        ACCOUNTS + LOCK_TIMEOUT_MEASUREMENT_UNIT))
                        .orElse(DEFAULT_LOCK_TIMEOUT_MEASUREMENT_UNIT);
    }

    public LightAppSettings(
            final String address,
            final String port,
            final String protocol,
            final ApiType type,
            final Integer timeout,
            final String lockTimeoutMeasurementUnit)
            throws ValidationException {
        this.type = type;
        this.address = address;
        this.port = port;
        this.protocol = protocol;
        this.lockTimeout = timeout;
        this.lockTimeoutMeasurementUnit = lockTimeoutMeasurementUnit;
    }

    public final String getAddress() {
        return address;
    }

    public final Properties getSettingsProperties() {
        final Properties properties = new Properties();
        properties.setProperty(type + ADDRESS, address);
        properties.setProperty(type + PORT, port);
        properties.setProperty(type + PROTOCOL, protocol);
        properties.setProperty(ACCOUNTS + LOCK_TIMEOUT, lockTimeout.toString());
        properties.setProperty(
                ACCOUNTS + LOCK_TIMEOUT_MEASUREMENT_UNIT, lockTimeoutMeasurementUnit);
        return properties;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        LightAppSettings that = (LightAppSettings) other;
        return type == that.type
                && Objects.equals(address, that.address)
                && Objects.equals(port, that.port)
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(lockTimeout, that.lockTimeout)
                && Objects.equals(lockTimeoutMeasurementUnit, that.lockTimeoutMeasurementUnit);
    }

    @Override
    public int hashCode() {

        return Objects.hash(type, address, port, protocol, lockTimeout, lockTimeoutMeasurementUnit);
    }
}
