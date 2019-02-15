package org.aion.zero.impl.sync;

/**
 * Used for tracking different type of requests made to peers.
 *
 * @author Alexandra Roatis
 */
public enum RequestType {
    STATUS,
    HEADERS,
    BODIES
}
