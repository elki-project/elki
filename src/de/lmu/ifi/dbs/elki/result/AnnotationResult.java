package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;

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
}
