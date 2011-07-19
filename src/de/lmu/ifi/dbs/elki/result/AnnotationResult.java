package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;

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
  public T getValueFor(DBID objID);
  
  /**
   * Get the DBIDs the annotation is defined for.
   * 
   * @return DBIDs
   */
  public DBIDs getDBIDs();
}
