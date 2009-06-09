package de.lmu.ifi.dbs.elki.result;

import java.util.Collection;

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
  private AnyMap<AssociationID<?>> data;
  
  /**
   * Constructor.
   */
  public MetadataResult() {
    data = new AnyMap<AssociationID<?>>();
  }

  /**
   * Put meta data.
   * 
   * @param <M> data class
   * @param meta key
   * @param value data
   */
  public <M> void setAssociation(AssociationID<M> meta, M value) {
    data.put(meta, value);
  }
  
  /**
   * Get a meta data object.
   * 
   * @param <M> data class
   * @param meta Key
   * @return stored meta data or null
   */
  public <M> M getAssociation(AssociationID<M> meta) {
    return data.get(meta, meta.getType());
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
  @SuppressWarnings("unchecked")
  public <M> M getAssociationGenerics(AssociationID<?> meta) {
    return (M) data.getGenerics(meta, meta.getType());
  }
  
  /**
   * Get stored metadata associations.
   * 
   * @return Stored keys
   */
  public Collection<AssociationID<?>> getAssociations() {
    return data.keySet();
  }
}
