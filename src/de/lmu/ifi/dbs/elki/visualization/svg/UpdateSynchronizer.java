package de.lmu.ifi.dbs.elki.visualization.svg;

/**
 * API to synchronize updates
 * 
 * @author Erich Schubert
 */
public interface UpdateSynchronizer {
  /**
   * This method is called whenever a new pending event was added.
   */
  void activate();

  /**
   * Set an update runner to use.
   *  
   * @param updateRunner
   */
  void addUpdateRunner(UpdateRunner updateRunner);
}