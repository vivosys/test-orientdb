package com.vivosys.test.persistence.ops.orientdb;

/**
 * An Exception occurring during I/O with OrientDB.
 */
public class OrientException extends RuntimeException {

    public OrientException(String message) {

        super(message);

    }

    public OrientException(String message, Throwable cause) {

        super(message, cause);

    }

}
