package com.owenjg.regexsynthesiser.exceptions;

public class RegexSynthesisException extends Exception {
    public RegexSynthesisException(String message) {
        super(message);
    }

    public RegexSynthesisException(String message, Throwable cause) {
        super(message, cause);
    }
}