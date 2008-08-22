package de.lmu.ifi.dbs.elki.utilities.optionhandling;

/**
 * Abstract super class for all exceptions thrown during parmeterization.
 *
 * @author Elke Achtert
 */
public abstract class ParameterException extends Exception {
    protected ParameterException(String message) {
        super(message);
    }

    protected ParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
