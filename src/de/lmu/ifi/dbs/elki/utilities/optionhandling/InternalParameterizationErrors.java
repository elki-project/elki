package de.lmu.ifi.dbs.elki.utilities.optionhandling;

import java.util.Collection;

/**
 * Pseudo error class that wraps multiple error reports into one.
 * 
 * This is meant for reporting re-parameterization errors.
 * 
 * @author Erich Schubert
 */
public class InternalParameterizationErrors extends ParameterException {
  /**
   * Serial version ID
   */
  private static final long serialVersionUID = 1L;

  /**
   * The errors that occurred.
   */
  private Collection<? extends Exception> internalErrors;

  /**
   * Constructor.
   * 
   * @param message Error message
   * @param internalErrors internal errors
   */
  public InternalParameterizationErrors(String message, Collection<? extends Exception> internalErrors) {
    super(message);
    this.internalErrors = internalErrors;
  }
  
  /**
   * @return the internalErrors
   */
  protected Collection<? extends Exception> getInternalErrors() {
    return internalErrors;
  }
}