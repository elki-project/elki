package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * The AnnotationResult interface.
 * Allows random-access queries for annotation data.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf AssociationID
 *
 * @param <T> Data type of annotations returned
 */
public interface AnnotationResult<T> extends Relation<T> {
  /**
   * Return the association ID of this result.
   * 
   * @return Association ID
   */
  public AssociationID<T> getAssociationID();  
}
