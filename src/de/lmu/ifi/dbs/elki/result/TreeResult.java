package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;
import java.util.Collections;

/**
 * Generic tree result, which has primary results (including previous results)
 * and derived results. This forms a simple tree/DAG structure.
 * 
 * @author Erich Schubert
 */
public class TreeResult implements Result {
  /**
   * Result name, for presentation
   */
  protected String name;

  /**
   * Result name, for output
   */
  protected String shortname;

  /**
   * Collection of primary results.
   */
  Collection<AnyResult> primaryResults;

  /**
   * Collection of derived results.
   */
  Collection<AnyResult> derivedResults;

  /**
   * Result constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   */
  public TreeResult(String name, String shortname) {
    super();
    this.name = name;
    this.shortname = shortname;
    this.primaryResults = new java.util.Vector<AnyResult>();
    this.derivedResults = new java.util.Vector<AnyResult>();
  }

  /**
   * Result constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param primary existing primary results
   */
  public TreeResult(String name, String shortname, Collection<AnyResult> primary) {
    super();
    this.name = name;
    this.shortname = shortname;
    this.primaryResults = primary;
  }

  @Override
  public Collection<AnyResult> getPrimary() {
    return Collections.unmodifiableCollection(primaryResults);
  }

  @Override
  public Collection<AnyResult> getDerived() {
    return Collections.unmodifiableCollection(derivedResults);
  }

  /**
   * Add a new primary result to the object
   * 
   * @param r new result
   */
  public void addPrimaryResult(AnyResult r) {
    this.primaryResults.add(r);
  }

  @Override
  public void addDerivedResult(AnyResult r) {
    derivedResults.add(r);
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
