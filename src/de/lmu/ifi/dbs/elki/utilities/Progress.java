package de.lmu.ifi.dbs.elki.utilities;

/**
 * A progress object for a given overall number of items to process. The number
 * of already processed items at a point in time can be updated.
 * 
 * The main feature of this class is to provide a String representation of the
 * progress suitable as a message for printing to the command line interface.
 * 
 * @author Arthur Zimek
 */
public class Progress {
  /**
   * The overall number of items to process.
   */
  private final int total;

  /**
   * Holds the length of a String describing the total number.
   */
  private final int totalLength;

  /**
   * The number of items already processed at a time being.
   */
  private int processed;

  /**
   * The task name.
   */
  private String task;
  
  /**
   * Auxiliary information.
   * 
   * FIXME: remove this, and use subclasses.
   */
  private String aux;

  /**
   * A progress object for a given overall number of items to process.
   * 
   * @param task the name of the task
   * @param total the overall number of items to process
   */
  public Progress(String task, int total) {
    this.task = task;
    this.total = total;
    this.totalLength = Integer.toString(total).length();
  }

  /**
   * Sets the number of items already processed at a time being.
   * 
   * @param processed the number of items already processed at a time being
   * @throws IllegalArgumentException if the given number is negative or exceeds
   *         the overall number of items to process
   */
  public void setProcessed(int processed) throws IllegalArgumentException {
    if(processed > total) {
      throw new IllegalArgumentException(processed + " exceeds total: " + total);
    }
    if(processed < 0) {
      throw new IllegalArgumentException("Negative number of processed: " + processed);
    }
    this.processed = processed;
  }

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
   * Append a string representation of the progress to the given string buffer.
   * 
   * @param message
   */
  public void appendToBuffer(StringBuffer message) {
    String processedString = Integer.toString(processed);
    int percentage = (int) (processed * 100.0 / total);
    message.append("Processed: ");
    for(int i = 0; i < totalLength - processedString.length(); i++) {
      message.append(' ');
    }
    message.append(processed);
    message.append(" [");
    if(percentage < 100) {
      message.append(' ');
    }
    if(percentage < 10) {
      message.append(' ');
    }
    message.append(percentage);
    message.append("%]");
    if (aux != null) {
      message.append(" ");
      message.append(aux);
    }
  }

  /**
   * Provides the name of the task.
   * 
   * 
   * @return the name of the task
   */
  public String getTask() {
    return this.task;
  }

  /**
   * Return the auxiliary information string.
   * 
   * @return the auxiliary string
   */
  public String getAuxiliary() {
    return aux;
  }

  /**
   * Set the auxiliary information string.
   * 
   * @param aux the auxiliary string
   */
  public void setAuxiliary(String aux) {
    this.aux = aux;
  }
}
