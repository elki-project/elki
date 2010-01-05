package de.lmu.ifi.dbs.elki.logging.progress;

/**
 * Abstract base class for FiniteProgress objects.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractProgress implements Progress {
  /**
   * The number of items already processed at a time being.
   */
  private int processed;

  /**
   * The task name.
   */
  private String task;
  
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
    this.processed = processed;
  }

  /**
   * Get the number of items already processed at a time being.
   * 
   * @return number of processed items
   */
  public int getProcessed() {
    return processed;
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

}