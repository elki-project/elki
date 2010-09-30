package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;


/**
 * Result used in passing the reference points to the visualizers.
 * 
 * @author Erich Schubert
 * 
 * @param <O> data type
 */
public class ReferencePointsResult<O> extends CollectionResult<O> {
  /**
   * Constructor with collection only.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Reference Points
   */
  public ReferencePointsResult(String name, String shortname, Collection<O> col) {
    super(name, shortname, col);
  }

  /**
   * Full constructor.
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Reference Points
   * @param header Header
   */
  public ReferencePointsResult(String name, String shortname, Collection<O> col, Collection<String> header) {
    super(name, shortname, col, header);
  }
}