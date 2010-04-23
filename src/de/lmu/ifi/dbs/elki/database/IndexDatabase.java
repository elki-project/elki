package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * IndexDatabase is a database implementation which is supported by an index
 * structure.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject as element of the database
 */
public abstract class IndexDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {
  /**
   * Deletes the specified object from the underlying index structure and
   * afterwards calls the super method.
   */
  @Override
  public O delete(Integer id) {
    if(get(id) == null) {
      return null;
    }

    getIndex().delete(get(id));
    O object = super.delete(id);
    return object;
  }

  /**
   * Deletes the specified object from the underlying index structure and
   * afterwards calls the super method.
   */
  @Override
  public void delete(O object) {
    getIndex().delete(object);
    super.delete(object);
  }

  /**
   * Calls {@link #doInsert(Pair)}, inserts the object in the underlying index
   * structure and notifies the listeners about the new insertion.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  @Override
  public Integer insert(Pair<O, DatabaseObjectMetadata> objectAndAssociations) throws UnableToComplyException {
    // insert into db
    Integer id = doInsert(objectAndAssociations);
    // insert into index
    getIndex().insert(objectAndAssociations.getFirst());
    // notify listeners
    fireObjectInserted(id);

    return id;
  }

  /**
   * Calls for each object {@link #doInsert(Pair)}, inserts the objects in the
   * underlying index structure and notifies the listeners about the new
   * insertions.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  @Override
  public List<Integer> insert(List<Pair<O, DatabaseObjectMetadata>> objectsAndAssociationsList) throws UnableToComplyException {
    if(objectsAndAssociationsList.isEmpty()) {
      return new ArrayList<Integer>();
    }
    // insert into db
    List<Integer> ids = doInsert(objectsAndAssociationsList);
    // insert into index
    getIndex().insert(getObjects(objectsAndAssociationsList));
    // notify listeners
    fireObjectsInserted(ids);
    return ids;
  }

  /**
   * Returns the physical read access of this database.
   * 
   * @return the physical read access of this database.
   */
  public long getPhysicalReadAccess() {
    return getIndex().getPhysicalReadAccess();
  }

  /**
   * Returns the physical write access of this database.
   * 
   * @return the physical write access of this database.
   */
  public long getPhysicalWriteReadAccess() {
    return getIndex().getPhysicalWriteAccess();
  }

  /**
   * Returns the logical page access of this database.
   * 
   * @return the logical page access of this database.
   */
  public long getLogicalPageAccess() {
    return getIndex().getLogicalPageAccess();
  }

  /**
   * Resets the page -access of this database.
   */
  public void resetPageAccess() {
    getIndex().resetPageAccess();
  }

  /**
   * Returns a string representation of this database.
   * 
   * @return a string representation of this database.
   */
  @Override
  public String toString() {
    return getIndex().toString();
  }

  /**
   * Returns the underlying index structure.
   * 
   * @return the underlying index structure
   */
  public abstract Index<O> getIndex();
}