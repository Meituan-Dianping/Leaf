package com.sankuai.inf.leaf.snowflake.exception;

/**
 * @author mickle
 */
public class ClockGoBackException extends RuntimeException {
    public ClockGoBackException(String message) {
        super(message);
    }
}
