package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry. Make sure to delete any associations when deleting an entry (e.g. by
 * calling {@link #deleteAssociations(Integer) deleteAssociations(id)}).
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDatabase<O extends DatabaseObject> extends AbstractParameterizable implements Database<O> {
  /**
   * Map to hold global associations.
   */
  private final Map<AssociationID, Object> globalAssociations;

  /**
   * Map to hold association maps.
   */
  private final Map<AssociationID, Map<Integer, Object>> associations;

  /**
   * Counter to provide a new Integer id.
   */
  private int counter;

  /**
   * Provides a list of reusable ids.
   */
  private List<Integer> reusableIDs;

  /**
   * Map to hold the objects of the database.
   */
  private Map<Integer, O> content;

  /**
   * Holds the listener of this database.
   */
  protected List<DatabaseListener> listenerList = new ArrayList<DatabaseListener>();

  /**
   * Provides an abstract database including a mapping for associations based
   * on a Hashtable and functions to get the next usable ID for insertion,
   * making IDs reusable after deletion of the entry. Make sure to delete any
   * associations when deleting an entry (e.g. by calling
   * {@link #deleteAssociations(Integer) deleteAssociations(id)}).
   */
  protected AbstractDatabase() {
    super();
    content = new Hashtable<Integer, O>();
    associations = new Hashtable<AssociationID, Map<Integer, Object>>();
    globalAssociations = new Hashtable<AssociationID, Object>();
    counter = 0;
    reusableIDs = new ArrayList<Integer>();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#insert(java.util.List)
   */
  public void insert(List<ObjectAndAssociations<O>> objectsAndAssociationsList) throws UnableToComplyException {
    for (ObjectAndAssociations<O> objectAndAssociations : objectsAndAssociationsList) {
      insert(objectAndAssociations);
    }
  }

  /**
   * @throws UnableToComplyException if database reached limit of storage capacity
   * @see Database#insert(ObjectAndAssociations)
   */
  public Integer insert(ObjectAndAssociations<O> objectAndAssociations) throws UnableToComplyException {
    O object = objectAndAssociations.getObject();
    // insert object
    Integer id = setNewID(object);
    content.put(id, object);
    // insert associations
    Map<AssociationID, Object> associations = objectAndAssociations.getAssociations();
    setAssociations(id, associations);
    // notify listeners
    fireObjectInserted(id);

    return id;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#delete(de.lmu.ifi.dbs.data.DatabaseObject)
   */
  public void delete(O object) {
    for (Integer id : content.keySet()) {
      if (content.get(id).equals(object)) {
        delete(id);
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#delete(java.lang.Integer)
   */
  public O delete(Integer id) {
    O object = content.remove(id);
    restoreID(id);
    deleteAssociations(id);
    // notify listeners
    fireObjectRemoved(id);

    return object;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#size()
   */
  public final int size() {
    return content.size();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#get(java.lang.Integer)
   */
  public final O get(Integer id) {
    return content.get(id);
  }

  /**
   * Returns an iterator iterating over all keys of the database.
   *
   * @return an iterator iterating over all keys of the database
   */
  public final Iterator<Integer> iterator() {
    return content.keySet().iterator();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#associate(AssociationID, Integer,
   *      Object)
   */
  public void associate(final AssociationID associationID, final Integer objectID, final Object association) {
    try {
      associationID.getType().cast(association);
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("Expected class: " + associationID.getType() + ", found " + association.getClass());
    }

    if (!associations.containsKey(associationID)) {
      associations.put(associationID, new Hashtable<Integer, Object>());
    }
    associations.get(associationID).put(objectID, association);
  }

  /**
   * Associates a global association in a certain relation to the database.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @param association   the association to be associated with the database
   * @throws ClassCastException if the association cannot be cast as the class that is specified by the associationID
   */
  public void associateGlobally(AssociationID associationID, Object association) throws ClassCastException {
    try {
      associationID.getType().cast(association);
    }
    catch (ClassCastException e) {
      throw new IllegalArgumentException("Expected class: " + associationID.getType() + ", found " + association.getClass());
    }

    globalAssociations.put(associationID, association);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#getAssociation(AssociationID,
   *      Integer)
   */
  public Object getAssociation(final AssociationID associationID, final Integer objectID) {
    if (associations.containsKey(associationID)) {
      return associations.get(associationID).get(objectID);
    }
    else {
      return null;
    }
  }

  /**
   * Returns the global association specified by the given associationID.
   *
   * @param associationID the id of the association, respectively the name of the
   *                      relation
   * @return Object the association or null, if there is no association with the specified
   *         associationID
   */
  public Object getGlobalAssociation(AssociationID associationID) {
    return globalAssociations.get(associationID);
  }

  /**
   * Provides a new id for the specified database object suitable as key for a
   * new insertion and sets this id in the specified database object.
   *
   * @return a new id suitable as key for a new insertion
   * @throws UnableToComplyException if the database has reached the limit and, therefore, new
   *                                 insertions are not possible
   */
  protected Integer setNewID(O object) throws UnableToComplyException {
    if (object.getID() != null) {
      if (content.containsKey(object.getID()))
        throw new UnableToComplyException("ID " + object.getID() + " is already in use!");
      return object.getID();
    }

    if (content.size() == Integer.MAX_VALUE) {
      throw new UnableToComplyException("Database reached limit of storage.");
    }

    else {
      Integer id;
      if (reusableIDs.size() != 0) {
        id = reusableIDs.remove(0);
      }
      else {
        if (counter == Integer.MAX_VALUE) {
          throw new UnableToComplyException("Database reached limit of storage.");
        }
        else {
          counter++;
          while (content.containsKey(counter)) {
            if (counter == Integer.MAX_VALUE) {
              throw new UnableToComplyException("Database reached limit of storage.");
            }
            counter++;
          }
          id = counter;
        }
      }
      object.setID(id);
      return id;
    }
  }

  /**
   * Makes the given id reusable for new insertion operations.
   *
   * @param id the id to become reusable
   */
  protected void restoreID(final Integer id) {
    {
      reusableIDs.add(id);
    }
  }

  /**
   * Deletes associations for the given id if there are any.
   *
   * @param id id of which all associations are to be deleted
   */
  protected void deleteAssociations(final Integer id) {
    for (AssociationID a : associations.keySet()) {
      associations.get(a).remove(id);
    }
  }

  /**
   * Returns all associations for a given ID.
   *
   * @param id the id for which the associations are to be returned
   * @return all associations for a given ID
   */
  public Map<AssociationID, Object> getAssociations(final Integer id) {
    Map<AssociationID, Object> idAssociations = new Hashtable<AssociationID, Object>();
    for (AssociationID associationID : associations.keySet()) {
      if (associations.get(associationID).containsKey(id)) {
        idAssociations.put(associationID, associations.get(associationID).get(id));
      }
    }
    return idAssociations;
  }

  /**
   * Sets the specified association to the specified id.
   *
   * @param id             the id which is to associate with specified associations
   * @param idAssociations the associations to be associated with the specified id
   */
  protected void setAssociations(final Integer id, final Map<AssociationID, Object> idAssociations) {
    for (AssociationID associationID : idAssociations.keySet()) {
      associate(associationID, id, idAssociations.get(associationID));
    }
  }

  /**
   * @see Database#partition(java.util.Map)
   */
  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException {
    return partition(partitions, null, null);
  }

  /**
   * @see Database#partition(java.util.Map, Class, String[])
   */
  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions,
                                             Class dbClass, String[] dbParameters) throws UnableToComplyException {
    if (dbClass == null) {
      dbClass = getClass();
      dbParameters = getParameters();
    }

    Map<Integer, Database<O>> databases = new Hashtable<Integer, Database<O>>();
    for (Integer partitionID : partitions.keySet()) {
      List<ObjectAndAssociations<O>> objectAndAssociationsList = new ArrayList<ObjectAndAssociations<O>>();
      List<Integer> ids = partitions.get(partitionID);
      for (Integer id : ids) {
        O object = get(id);
        Map<AssociationID, Object> associations = getAssociations(id);
        objectAndAssociationsList.add(new ObjectAndAssociations<O>(object, associations));
      }

      Database<O> database;
      try {
        //noinspection unchecked
        database = Util.instantiate(Database.class, dbClass.getName());
        database.setParameters(dbParameters);
        database.insert(objectAndAssociationsList);
        databases.put(partitionID, database);
      }
      catch (ParameterException e) {
        throw new UnableToComplyException(e);
      }
    }
    return databases;
  }

  /**
   * SequentialDatabase does not require any parameters. Thus, this method
   * returns the given parameters unchanged.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingOptions = optionHandler.grabOptions(args);

    setParameters(args, remainingOptions);
    return remainingOptions;
  }

//  /**
//   * Sets the difference of the first array minus the second array
//   * as the currently set parameter array.
//   *
//   * @param complete the complete array
//   * @param part     an array that contains only elements of the first array
//   */
//  protected void setParameters(String[] complete, String[] part) {
//    currentParameterArray = Util.parameterDifference(complete, part);
//  }

//  /**
//   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
//   */
//  public String[] getParameters() {
//    String[] param = new String[currentParameterArray.length];
//    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
//    return param;
//  }

  /**
   * Checks whether an association is set for every id in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database,
   *         false otherwise
   */
  public boolean isSetForAllObjects(AssociationID associationID) {
    for (Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext();) {
      Integer id = dbIter.next();
      if (this.getAssociation(associationID, id) == null) return false;
    }
    return true;
  }

  /**
   * Checks whether an association is set for at least one id in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database,
   *         false otherwise
   */
  public boolean isSet(AssociationID associationID) {
    for (Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext();) {
      Integer id = dbIter.next();
      if (this.getAssociation(associationID, id) != null) return true;
    }
    return false;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#randomSample(int, long)
   */
  public final Set<Integer> randomSample(int k, long seed) {
    if (k < 0) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k);
    }

    Set<Integer> sample = new HashSet<Integer>(k);
    List<Integer> ids = getIDs();
    Random random = new Random(seed);
    while (sample.size() < k) {
      sample.add(ids.get(random.nextInt(ids.size())));
    }
    return sample;
  }

  /**
   * @see Database#getIDs()
   */
  public List<Integer> getIDs() {
    List<Integer> ids = new ArrayList<Integer>(this.size());
    for (Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext();) {
      ids.add(dbIter.next());
    }
    return ids;
  }

  /**
   * @see Database#dimensionality()
   */
  public int dimensionality() throws UnsupportedOperationException {
    Iterator<Integer> iter = this.iterator();
    if (iter.hasNext()) {
      O entry = this.get(iter.next());
      if (entry instanceof FeatureVector) {
        // noinspection unchecked
        return ((FeatureVector) entry).getDimensionality();
      }
      else {
        throw new UnsupportedOperationException("Database entries are not implementing interface " + FeatureVector.class.getName() + ".");
      }
    }
    else {
      throw new UnsupportedOperationException("Database is empty.");
    }
  }

  /**
   * Helper method to extract the list of database objects from the specified list of objects
   * and their associations.
   *
   * @param objectAndAssociationsList the list of objects and their associations
   * @return the list of database objects
   */
  protected List<O> getObjects(List<ObjectAndAssociations<O>> objectAndAssociationsList) {
    List<O> objects = new ArrayList<O>(objectAndAssociationsList.size());
    for (ObjectAndAssociations<O> objectAndAssociations : objectAndAssociationsList) {
      objects.add(objectAndAssociations.getObject());
    }
    return objects;
  }

  /**
   * Adds a listener for the <code>DatabaseEvent</code>
   * posted after the database changes.
   *
   * @param l the listener to add
   * @see #removeDatabaseListener
   */
  public void addDatabaseListener(DatabaseListener l) {
    listenerList.add(l);
  }

  /**
   * Removes a listener previously added with
   * <code>addTreeModelListener</code>.
   *
   * @param l the listener to remove
   * @see #addDatabaseListener
   */
  public void removeDatabaseListener(DatabaseListener l) {
    listenerList.remove(l);
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.
   *
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsChanged(List<Integer> objectIDs) {
    if (listenerList.isEmpty()) return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for (DatabaseListener listener : listenerList) {
      listener.objectsChanged(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.
   *
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsInserted(List<Integer>  objectIDs) {
    if (listenerList.isEmpty()) return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for (DatabaseListener listener : listenerList) {
      listener.objectsInserted(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.
   *
   * @param objectID the ids of the database object that has been removed
   */
  protected void fireObjectInserted(Integer objectID) {
    List<Integer> objectIDs = new ArrayList<Integer>();
    objectIDs.add(objectID);
    fireObjectsInserted(objectIDs);
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.
   *
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsRemoved(List<Integer> objectIDs) {
    if (listenerList.isEmpty()) return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for (DatabaseListener listener : listenerList) {
      listener.objectsRemoved(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for
   * notification on this event type.
   *
   * @param objectID the id of the database object that has been removed
   */
  protected void fireObjectRemoved(Integer objectID) {
    List<Integer> objectIDs = new ArrayList<Integer>();
    objectIDs.add(objectID);
    fireObjectsRemoved(objectIDs);
  }
}
