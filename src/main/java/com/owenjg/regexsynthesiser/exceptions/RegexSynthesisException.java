package com.owenjg.regexsynthesiser.exceptions;

/**
 * Custom exception for handling errors during the regular expression synthesis process.
 * Provides specific error information when issues occur during regex generation.
 */
public class RegexSynthesisException extends Exception {

    /**
     * Constructs a new RegexSynthesisException with the specified error message.
     *
     * @param message The detailed message explaining the nature of the exception
     */
    public RegexSynthesisException(String message) {
        super(message);
    }

    /**
     * Constructs a new RegexSynthesisException with the specified error message and cause.
     *
     * @param message The detailed message explaining the nature of the exception
     * @param cause The underlying exception that led to this exception
     */
    public RegexSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}