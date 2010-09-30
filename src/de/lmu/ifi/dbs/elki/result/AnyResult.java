package de.lmu.ifi.dbs.elki.result;

/**
 * General interface for results, that can both be primitive or allow nesting.
 * 
 * @author Erich Schubert
 */
public abstract interface AnyResult {
  /**
   * A "pretty" name for the result, for use in titles, captions and menus.
   * 
   * @return result name
   */
  public String getLongName();

  /**
   * A short name for the result, useful for file names.
   * 
   * @return result name
   */
  public String getShortName();
}
