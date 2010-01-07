package de.lmu.ifi.dbs.elki.logging.progress;

/**
 * Generic Progress logging interface.
 * 
 * @author Erich Schubert
 *
 */
public interface Progress {
  /**
   * Serialize a description into a String buffer.
   * 
   * @param buf Buffer to serialize to
   * @return Buffer the data was serialized to.
   */
  public StringBuffer appendToBuffer(StringBuffer buf);

  /**
   * Test whether a progress is complete (and thus doesn't need to be shown anymore)
   * 
   * @return Whether the progress was completed.
   */
  public boolean isComplete();
  
  /**
   * Returns a String representation of the progress suitable as a message for
   * printing to the command line interface.
   * 
   * @see java.lang.Object#toString()
   */
  public String toString();
}