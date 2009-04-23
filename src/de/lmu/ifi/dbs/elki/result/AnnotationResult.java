package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

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
   * Return the association ID of this result.
   * 
   * @return Association ID
   */
  public AssociationID<T> getAssociationID();
  
  /**
   * Get associated value for an object.
   * 
   * @param objID Object ID
   * @return Associated value
   */
  public T getValueFor(Integer objID);
  
  /**
   * Query annotations for a database object.
   * Note that each AnnotationResult can return multiple values, but only of the given type T.
   * 
   * @param objID
   * @return an array of pairs (label, value)
   */
  @Deprecated
  public Pair<String, T>[] getAnnotations(Integer objID);
}
