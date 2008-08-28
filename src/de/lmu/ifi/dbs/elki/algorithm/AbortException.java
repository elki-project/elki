package de.lmu.ifi.dbs.elki.algorithm;

/**
 * Exception for aborting some process and transporting a message.
 *
 * @author Arthur Zimek
 */
@SuppressWarnings("serial")
public class AbortException extends RuntimeException {
    /**
     * Exception for aborting some process and transporting a message.
     *
     * @param message message to be transported
     */
    public AbortException(String message) {
        super(message);
    }

}
