package de.lmu.ifi.dbs.elki.logging.progress;

import java.util.concurrent.atomic.AtomicInteger;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Abstract base class for FiniteProgress objects.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractProgress implements Progress {
  /**
   * The number of items already processed at a time being.
   * 
   * We use AtomicInteger to allow threaded use without synchronization.
   */
  private AtomicInteger processed = new AtomicInteger(0);

  /**
   * The task name.
   */
  private String task;

  /**
   * For logging rate control.
   */
  private long lastLogged = Long.MIN_VALUE;

  /**
   * Default constructor.
   * 
   * @param task Task name.
   */
  public AbstractProgress(String task) {
    super();
    this.task = task;
  }

  /**
   * Provides the name of the task.
   * 
   * @return the name of the task
   */
  public String getTask() {
    return task;
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @throws IllegalArgumentException if an invalid value was passed.
   */
  public void setProcessed(int processed) throws IllegalArgumentException {
    this.processed.set(processed);
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @param logger Logger to report to
   * @throws IllegalArgumentException if an invalid value was passed.
   */
  public void setProcessed(int processed, Logging logger) throws IllegalArgumentException {
    setProcessed(processed);
    if(testLoggingRate()) {
      logger.progress(this);
    }
  }

  /**
   * Get the number of items already processed at a time being.
   * 
   * @return number of processed items
   */
  public int getProcessed() {
    return processed.get();
  }

  /**
   * Serialize a description into a String buffer.
   * 
   * @param buf Buffer to serialize to
   * @return Buffer the data was serialized to.
   */
  @Override
  public abstract StringBuffer appendToBuffer(StringBuffer buf);

  /**
   * Returns a String representation of the progress suitable as a message for
   * printing to the command line interface.
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuffer message = new StringBuffer();
    appendToBuffer(message);
    return message.toString();
  }

  /**
   * Increment the processed counter.
   */
  public void incrementProcessed() {
    this.processed.incrementAndGet();
  }

  /**
   * Increment the processed counter.
   * 
   * @param logger Logger to report to.
   */
  public void incrementProcessed(Logging logger) {
    incrementProcessed();
    if(testLoggingRate()) {
      logger.progress(this);
    }
  }

  /**
   * Logging rate control.
   * 
   * @return true when logging is sensible
   */
  protected boolean testLoggingRate() {
    if(isComplete() || getProcessed() < 10) {
      return true;
    }
    final long now = System.nanoTime();
    if(lastLogged > now - 1E8) {
      return false;
    }
    lastLogged = now;
    return true;
  }
}