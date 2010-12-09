package de.lmu.ifi.dbs.elki.result;

/**
 * Basic class for a result. Much like AbstractHierarchicalResult, except it
 * stores the required short and long result names.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
// TODO: getter, setter for result names? Merge with AbstractHierarchicalResult?
public class BasicResult extends AbstractHierarchicalResult {
  /**
   * Result name, for presentation
   */
  private String name;

  /**
   * Result name, for output
   */
  private String shortname;

  /**
   * Result constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public BasicResult(String name, String shortname) {
    super();
    this.name = name;
    this.shortname = shortname;
  }

  @Override
  public final String getLongName() {
    return name;
  }

  @Override
  public final String getShortName() {
    return shortname;
  }
}