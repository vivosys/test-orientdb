package com.vivosys.test.persistence.ops;

/**
 * Docs
 */
public class OptimisticLockingFailureException extends RuntimeException {

    public OptimisticLockingFailureException(String msg, Exception ex) {

        super(msg, ex);

    }

}
