package de.lmu.ifi.dbs.elki.result;

/**
 * Interface for arbitrary result objects.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface Result {
  /**
   * A "pretty" name for the result, for use in titles, captions and menus.
   * 
   * @return result name
   */
  // TODO: turn this into an optional annotation? But: no inheritance?
  public String getLongName();

  /**
   * A short name for the result, useful for file names.
   * 
   * @return result name
   */
  // TODO: turn this into an optional annotation? But: no inheritance?
  public String getShortName();
}