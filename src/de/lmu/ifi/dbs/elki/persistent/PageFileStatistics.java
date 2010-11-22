package de.lmu.ifi.dbs.elki.persistent;

/**
 * Statistics API for a Page File
 * @author Erich Schubert
 *
 */
public interface PageFileStatistics {
  /**
   * Returns the physical read I/O-Accesses of this file.
   * @return Number of physical read I/O accesses
   */
  public long getPhysicalReadAccess();

  /**
   * Returns the physical write I/O-Accesses of this file.
   * @return Number of physical write I/O accesses
   */
  public long getPhysicalWriteAccess();
  
  /**
   * Returns the logical read I/O-Accesses of this file.
   * @return Number of logical I/O accesses
   */
  public long getLogicalPageAccess();

  /**
   * Resets the counters for page accesses of this file and flushes the cache.
   */
  public void resetPageAccess();
}
