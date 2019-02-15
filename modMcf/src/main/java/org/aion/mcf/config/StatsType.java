package org.aion.mcf.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumerates the different type of statistics gathered by the kernel. Used for determining which
 * statistics to display.
 *
 * @author Alexandra Roatis
 */
public enum StatsType {
    ALL,
    PEER_STATES,
    REQUESTS,
    SEEDS,
    LEECHES,
    RESPONSES,
    NONE; // used as default for invalid settings

    private static final List<StatsType> allSpecificTypes =
        List.of(PEER_STATES, REQUESTS, SEEDS, LEECHES, RESPONSES);

    /**
     * List of all the specific type of statistics that can be displayed, i.e. excluding the {@link
     * #ALL} and {@link #NONE}.
     */
    public static List<StatsType> getAllSpecificTypes() {
        return allSpecificTypes;
    }
}
