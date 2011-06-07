package de.lmu.ifi.dbs.elki.persistent;

/**
 * Page file statistic utility functions.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @apiviz.uses PageFileStatistics oneway - - analyses
 */
public final class PageFileUtil {
  /**
   * Append the page file statistics to the output buffer.
   * 
   * @param buffer Buffer to append to
   */
  public static void appendPageFileStatistics(StringBuffer buffer, PageFileStatistics statistics) {
    buffer.append("Page File Layer: ").append(statistics.getClass()).append("\n");
    buffer.append("Read Operations: ").append(statistics.getReadOperations()).append("\n");
    buffer.append("Write Operations: ").append(statistics.getWriteOperations()).append("\n");
    PageFileStatistics inner = statistics.getInnerStatistics();
    if(inner != null) {
      appendPageFileStatistics(buffer, inner);
    }
  }
  
  /**
   * Get the number of (logical) read operations (without caching).
   * 
   * @param statistics Statistics.
   * @return logical read operations.
   */
  public static long getLogicalReadOperations(PageFileStatistics statistics) {
    return statistics.getReadOperations();
  }
  
  /**
   * Get the number of (logical) write operations (without caching).
   * 
   * @param statistics Statistics.
   * @return logical write operations.
   */
  public static long getLogicalWriteOperations(PageFileStatistics statistics) {
    return statistics.getWriteOperations();
  }
  
  /**
   * Get the number of (physical) read operations (with caching).
   * 
   * @param statistics Statistics.
   * @return physical read operations.
   */
  public static long getPhysicalReadOperations(PageFileStatistics statistics) {
    PageFileStatistics inner = statistics.getInnerStatistics();
    if(inner != null) {
      return getPhysicalReadOperations(inner);
    }
    return statistics.getReadOperations();
  }
  
  /**
   * Get the number of (physical) write operations (with caching).
   * 
   * @param statistics Statistics.
   * @return physical write operations.
   */
  public static long getPhysicalWriteOperations(PageFileStatistics statistics) {
    PageFileStatistics inner = statistics.getInnerStatistics();
    if(inner != null) {
      return getPhysicalWriteOperations(inner);
    }
    return statistics.getWriteOperations();
  }
}