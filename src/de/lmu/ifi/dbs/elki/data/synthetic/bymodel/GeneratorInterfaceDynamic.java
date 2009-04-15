package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

/**
 * Interface for a dynamic cluster generator.
 * 
 * A cluster generator is considered dynamic when it allows "rejecting" points
 * and the generation of additional new points.
 * 
 * @author Erich Schubert
 */
public interface GeneratorInterfaceDynamic extends GeneratorInterface {
  /**
   * Get number of discarded points
   * 
   * @return number of discarded points
   */
  public int getDiscarded();

  /**
   * Indicate that points were discarded.
   * 
   * @param discarded number of points that were discarded.
   */
  public void addDiscarded(int discarded);
  
  /**
   * Retrieve remaining number of retries.
   * @return remaining number of retries
   */
  public int getRetries();
}
