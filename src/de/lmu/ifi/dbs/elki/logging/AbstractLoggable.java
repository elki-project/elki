package de.lmu.ifi.dbs.elki.logging;


/**
 * Abstract superclass for classes being loggable, i.e. classes intending to log
 * messages.
 * <p/>
 * 
 * @author Steffi Wanka
 */
public abstract class AbstractLoggable {
  /**
   * The logger of the class.
   */
  protected final Logging logger;

  /**
   * Initializes the logger and sets the debug status to the given value.
   */
  protected AbstractLoggable() {
    this.logger = Logging.getLogger(this.getClass());
  }
}