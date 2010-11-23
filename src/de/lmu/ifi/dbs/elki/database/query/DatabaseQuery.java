package de.lmu.ifi.dbs.elki.database.query;

/**
 * General interface for database queries.
 * Will only contain elemental stuff such as some hints.
 * 
 * @author Erich Schubert
 */
public interface DatabaseQuery {
  /**
   * Optimizer hint: request bulk support.
   */
  public static final String HINT_BULK = "need_bulk";
  
  /**
   * Optimizer hint: only a single request will be done - avoid expensive optimizations
   */
  public static final String HINT_SINGLE = "single_query";
  
  /**
   * Optimizer hint: no linear scans allowed - return null then!
   */
  public static final String HINT_OPTIMIZED_ONLY = "optimized";
  
  /**
   * Optimizer hint: heavy use - caching/preprocessing/approximation recommended
   */
  public static final String HINT_HEAVY_USE = "heavy";
  
  /**
   * Optimizer hint: exact - no approximations allowed!
   */
  public static final String HINT_EXACT = "exact";
  
  /**
   * Optimizer hint: no cache instances
   */
  public static final String HINT_NO_CACHE = "no-cache";
}