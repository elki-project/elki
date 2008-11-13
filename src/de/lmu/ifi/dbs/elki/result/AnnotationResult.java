package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.utilities.pairs.SimplePair;

/**
 * The AnnotationResult interface.
 * Allows random-access queries for annotation data.
 * 
 * @author Erich Schubert
 *
 * @param <T> Data type of annotations returned
 */
public interface AnnotationResult<T> extends Result {
  /**
   * Query annotations for a database object.
   * Note that each AnnotationResult can return multiple values, but only of the given type T.
   * 
   * @param objID
   * @return an array of pairs (label, value)
   */
  public SimplePair<String, T>[] getAnnotations(Integer objID);
}
