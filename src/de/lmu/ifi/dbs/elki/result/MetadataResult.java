package de.lmu.ifi.dbs.elki.result;

import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;

/**
 * Result to store meta data.
 * 
 * @author Erich Schubert
 */
public class MetadataResult implements Result {
  /**
   * Data store.
   */
  private AnyMap<String> data;
  
  /**
   * Constructor.
   */
  public MetadataResult() {
    data = new AnyMap<String>();
  }

  /**
   * Put meta data.
   * 
   * @param <M> data class
   * @param meta key
   * @param value data
   */
  public <M> void setAssociation(AssociationID<M> meta, M value) {
    data.put(meta.getName(), value);
  }
  
  /**
   * Get a meta data object.
   * 
   * @param <M> data class
   * @param meta Key
   * @return stored meta data or null
   */
  public <M> M getAssociation(AssociationID<M> meta) {
    return data.get(meta.getName(), meta.getType());
  }
  
  /**
   * Get a meta data object, with weaker compile time type checking.
   * 
   * Note that in this version, it is not verified that the restriction class
   * actually satisfies the return type M.
   * 
   * @param <M> data class
   * @param meta Key
   * @return stored meta data or null
   */
  public <M> M getAssociationGenerics(AssociationID<M> meta) {
    return data.getGenerics(meta.getName(), meta.getType());
  }
}
