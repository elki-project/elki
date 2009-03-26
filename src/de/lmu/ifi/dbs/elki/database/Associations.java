package de.lmu.ifi.dbs.elki.database;

import java.util.Set;

import de.lmu.ifi.dbs.elki.utilities.AnyMap;

/**
 * A helper class to facilitate setting of global associations in a database.
 * 
 * @author Arthur Zimek
 */
public class Associations {
  /**
   * Map to store the actual data.
   */
  private AnyMap<AssociationID<?>> associations = new AnyMap<AssociationID<?>>();

  /**
   * Provides an Associations object ready to set and get Objects for
   * association.
   */
  public Associations() {
    // nothing to do
  }

  /**
   * Sets the specified object under the given AssociationID.
   * 
   * @param <T> the type of the object to be associated and the AssociationID
   * @param associationID the AssociationID to point to the object
   * @param associationObject the object to be associated
   */
  public <T> void put(AssociationID<T> associationID, T associationObject) {
    this.associations.put(associationID, associationObject);
  }

  /**
   * Sets the specified object under the given AssociationID. Untyped version.
   * 
   * @param associationID the AssociationID to point to the object
   * @param associationObject the object to be associated
   */
  public void putUnchecked(AssociationID<?> associationID, Object associationObject) {
    this.associations.put(associationID, associationObject);
  }

  /**
   * Retrieves the object associated under the given association id.
   * 
   * @param <T> the type of the object to retrieve conforms to the type of the
   *        association id
   * @param associationID the association id pointing to the object to retrieve
   * @return the object associated under the given association id
   */
  @SuppressWarnings("unchecked")
  public <T> T get(AssociationID<T> associationID) {
    return (T) this.associations.get(associationID, Object.class);
  }

  /**
   * Provides the set of all association ids pointing to objects within this
   * Associations.
   * 
   * 
   * @return the set of all association ids pointing to objects within this
   *         Associations
   */
  public Set<AssociationID<?>> keySet() {
    return this.associations.keySet();
  }

  /**
   * Adds all pairs of association ids and objects currently set in the given
   * Associations object to this Associations object.
   * 
   * @param associations an Associations object to copy the contained
   *        associations from
   */
  public void putAll(Associations associations) {
    for(AssociationID<?> associationID : associations.keySet()) {
      this.associations.put(associationID, associations.get(associationID));
    }
  }
}
