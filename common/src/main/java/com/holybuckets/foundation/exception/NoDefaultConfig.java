package com.holybuckets.foundation.exception;

/**
 * Particularly for loading configs, if no default config is found, we want
 * to halt processing of all types of this config.
 */
public class NoDefaultConfig extends Exception {
    public NoDefaultConfig(String message) {
        super(message);
    }

    public NoDefaultConfig(String message, Exception cause) {
        super(message, cause);
    }

}
