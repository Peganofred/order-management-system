package com.portfolio.orderms.exception;

/**
 * Thrown for BOTH "no such email" and "wrong password". Deliberately generic:
 * telling the client which one it was lets an attacker enumerate registered
 * emails one guess at a time.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
