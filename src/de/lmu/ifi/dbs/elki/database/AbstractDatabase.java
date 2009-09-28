package de.lmu.ifi.dbs.elki.database;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject as element of the database
 */
public abstract class AbstractDatabase<O extends DatabaseObject> extends AbstractParameterizable implements Database<O> {
  /**
   * Map to hold global associations.
   */
  private final Associations globalAssociations;

  /**
   * Map to hold association maps.
   */
  private final AssociationMaps associations;

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
   * Provides an abstract database including a mapping for associations based on
   * a Hashtable and functions to get the next usable ID for insertion, making
   * IDs reusable after deletion of the entry. Make sure to delete any
   * associations when deleting an entry (e.g. by calling
   * {@link #deleteAssociations(Integer) deleteAssociations(id)}).
   */
  protected AbstractDatabase() {
    super();
    content = new Hashtable<Integer, O>();
    associations = new AssociationMaps();
    globalAssociations = new Associations();
    counter = 0;
    reusableIDs = new ArrayList<Integer>();
  }

  public void insert(List<Pair<O, Associations>> objectsAndAssociationsList) throws UnableToComplyException {
    for(Pair<O, Associations> objectAndAssociations : objectsAndAssociationsList) {
      insert(objectAndAssociations);
    }
  }

  /**
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  public Integer insert(Pair<O, Associations> objectAndAssociations) throws UnableToComplyException {
    O object = objectAndAssociations.getFirst();
    // insert object
    Integer id = setNewID(object);
    content.put(id, object);
    // insert associations
    Associations associations = objectAndAssociations.getSecond();
    setAssociations(id, associations);
    // notify listeners
    fireObjectInserted(id);

    return id;
  }

  public void delete(O object) {
    for(Integer id : content.keySet()) {
      if(content.get(id).equals(object)) {
        delete(id);
      }
    }
  }

  public O delete(Integer id) {
    O object = content.remove(id);
    restoreID(id);
    deleteAssociations(id);
    // notify listeners
    fireObjectRemoved(id);

    return object;
  }

  public final int size() {
    return content.size();
  }

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

  public <T> void associate(final AssociationID<T> associationID, final Integer objectID, final T association) {
    try {
      associationID.getType().cast(association);
    }
    catch(ClassCastException e) {
      throw new IllegalArgumentException("Expected class: " + associationID.getType() + ", found " + association.getClass());
    }

    if(!associations.containsKey(associationID)) {
      associations.put(associationID, new Hashtable<Integer, T>());
    }
    associations.get(associationID).put(objectID, association);
  }

  /**
   * Associates a global association in a certain relation to the database.
   * 
   * @param associationID the id of the association, respectively the name of
   *        the relation
   * @param association the association to be associated with the database
   * @throws ClassCastException if the association cannot be cast as the class
   *         that is specified by the associationID
   */
  public <T> void associateGlobally(AssociationID<T> associationID, T association) throws ClassCastException {
    try {
      associationID.getType().cast(association);
    }
    catch(ClassCastException e) {
      throw new IllegalArgumentException("Expected class: " + associationID.getType() + ", found " + association.getClass());
    }

    globalAssociations.put(associationID, association);
  }

  public <T> T getAssociation(final AssociationID<T> associationID, final Integer objectID) {
    if(associations.containsKey(associationID)) {
      return associations.get(associationID).get(objectID);
    }
    else {
      return null;
    }
  }

  /**
   * Returns the global association specified by the given associationID.
   * 
   * @param associationID the id of the association, respectively the name of
   *        the relation
   * @return Object the association or null, if there is no association with the
   *         specified associationID
   */
  public <T> T getGlobalAssociation(AssociationID<T> associationID) {
    return globalAssociations.get(associationID);
  }

  /**
   * Provides a new id for the specified database object suitable as key for a
   * new insertion and sets this id in the specified database object.
   * 
   * @param object the object for which a new id should be provided
   * @return a new id suitable as key for a new insertion
   * @throws UnableToComplyException if the database has reached the limit and,
   *         therefore, new insertions are not possible
   */
  protected Integer setNewID(O object) throws UnableToComplyException {
    if(object.getID() != null) {
      if(content.containsKey(object.getID())) {
        throw new UnableToComplyException("ID " + object.getID() + " is already in use!");
      }
      return object.getID();
    }

    if(content.size() == Integer.MAX_VALUE) {
      throw new UnableToComplyException("Database reached limit of storage.");
    }

    else {
      Integer id;
      if(reusableIDs.size() != 0) {
        id = reusableIDs.remove(0);
      }
      else {
        if(counter == Integer.MAX_VALUE) {
          throw new UnableToComplyException("Database reached limit of storage.");
        }
        else {
          counter++;
          while(content.containsKey(counter)) {
            if(counter == Integer.MAX_VALUE) {
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
    for(AssociationID<?> a : associations.keySet()) {
      associations.get(a).remove(id);
    }
  }

  /**
   * Returns all associations for a given ID.
   * 
   * @param id the id for which the associations are to be returned
   * @return all associations for a given ID
   */
  public Associations getAssociations(final Integer id) {
    Associations idAssociations = new Associations();
    for(AssociationID<?> associationID : associations.keySet()) {
      if(associations.get(associationID).containsKey(id)) {
        idAssociations.putUnchecked(associationID, associations.get(associationID).get(id));
      }
    }
    return idAssociations;
  }

  /**
   * Sets the specified association to the specified id.
   * 
   * @param id the id which is to associate with specified associations
   * @param idAssociations the associations to be associated with the specified
   *        id
   */
  @SuppressWarnings("unchecked")
  protected void setAssociations(final Integer id, final Associations idAssociations) {
    for(AssociationID<?> associationID : idAssociations.keySet()) {
      AssociationID<Object> aID = (AssociationID<Object>) associationID;
      associate(aID, id, idAssociations.get(aID));
    }
  }

  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException {
    return partition(partitions, null, null);
  }

  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions, Class<? extends Database<O>> dbClass, List<String> dbParameters) throws UnableToComplyException {
    if(dbClass == null) {
      dbClass = ClassGenericsUtil.uglyCrossCast(this.getClass(), Database.class);
      dbParameters = getParameters();
    }

    Map<Integer, Database<O>> databases = new Hashtable<Integer, Database<O>>();
    for(Integer partitionID : partitions.keySet()) {
      List<Pair<O, Associations>> objectAndAssociationsList = new ArrayList<Pair<O, Associations>>();
      List<Integer> ids = partitions.get(partitionID);
      for(Integer id : ids) {
        O object = get(id);
        Associations associations = getAssociations(id);
        objectAndAssociationsList.add(new Pair<O, Associations>(object, associations));
      }

      Database<O> database;
      try {
        database = ClassGenericsUtil.instantiateGenerics(Database.class, dbClass.getName());
        database.setParameters(dbParameters);
        database.insert(objectAndAssociationsList);
        databases.put(partitionID, database);
      }
      catch(ParameterException e) {
        throw new UnableToComplyException(e);
      }
    }
    return databases;
  }

  /**
   * Checks whether an association is set for every id in the database.
   * 
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database, false
   *         otherwise
   */
  public boolean isSetForAllObjects(AssociationID<?> associationID) {
    for(Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext();) {
      Integer id = dbIter.next();
      if(this.getAssociation(associationID, id) == null)
        return false;
    }
    return true;
  }

  /**
   * Checks whether an association is set for at least one id in the database.
   * 
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database, false
   *         otherwise
   */
  public boolean isSet(AssociationID<?> associationID) {
    for(Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext();) {
      Integer id = dbIter.next();
      if(this.getAssociation(associationID, id) != null)
        return true;
    }
    return false;
  }

  public boolean isSetGlobally(AssociationID<?> associationID) {
    return this.getGlobalAssociation(associationID) != null;
  }

  public final Set<Integer> randomSample(int k, long seed) {
    if(k < 0 || k > this.size()) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k);
    }

    Set<Integer> sample = new HashSet<Integer>(k);
    List<Integer> ids = getIDs();
    Random random = new Random(seed);
    while(sample.size() < k) {
      sample.add(ids.get(random.nextInt(ids.size())));
    }
    return sample;
  }

  /**
   * Returns a list of all ids currently in use in the database.
   * 
   * The list is not affected of any changes made to the database in the future nor vice versa.
   * 
   * @see de.lmu.ifi.dbs.elki.database.Database#getIDs()
   */
  public List<Integer> getIDs() {
    List<Integer> ids = new ArrayList<Integer>(this.size());
    for(Integer id : this) {
      ids.add(id);
    }
    return ids;
  }

  public int dimensionality() throws UnsupportedOperationException {
    Iterator<Integer> iter = this.iterator();
    if(iter.hasNext()) {
      O entry = this.get(iter.next());
      if(entry instanceof NumberVector<?,?>) {
        return ((NumberVector<?, ?>) entry).getDimensionality();
      }
      else {
        throw new UnsupportedOperationException("Database entries are not implementing interface " + NumberVector.class.getName() + ".");
      }
    }
    else {
      throw new UnsupportedOperationException(ExceptionMessages.DATABASE_EMPTY);
    }
  }

  /**
   * Helper method to extract the list of database objects from the specified
   * list of objects and their associations.
   * 
   * @param objectAndAssociationsList the list of objects and their associations
   * @return the list of database objects
   */
  protected List<O> getObjects(List<Pair<O, Associations>> objectAndAssociationsList) {
    List<O> objects = new ArrayList<O>(objectAndAssociationsList.size());
    for(Pair<O, Associations> objectAndAssociations : objectAndAssociationsList) {
      objects.add(objectAndAssociations.getFirst());
    }
    return objects;
  }

  /**
   * Adds a listener for the <code>DatabaseEvent</code> posted after the
   * database changes.
   * 
   * @param l the listener to add
   * @see #removeDatabaseListener
   */
  public void addDatabaseListener(DatabaseListener l) {
    listenerList.add(l);
  }

  /**
   * Removes a listener previously added with <code>addTreeModelListener</code>.
   * 
   * @param l the listener to remove
   * @see #addDatabaseListener
   */
  public void removeDatabaseListener(DatabaseListener l) {
    listenerList.remove(l);
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsChanged(List<Integer> objectIDs) {
    if(listenerList.isEmpty())
      return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for(DatabaseListener listener : listenerList) {
      listener.objectsChanged(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsInserted(List<Integer> objectIDs) {
    if(listenerList.isEmpty())
      return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for(DatabaseListener listener : listenerList) {
      listener.objectsInserted(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectID the ids of the database object that has been removed
   */
  protected void fireObjectInserted(Integer objectID) {
    List<Integer> objectIDs = new ArrayList<Integer>();
    objectIDs.add(objectID);
    fireObjectsInserted(objectIDs);
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsRemoved(List<Integer> objectIDs) {
    if(listenerList.isEmpty())
      return;
    DatabaseEvent e = new DatabaseEvent(this, objectIDs);
    for(DatabaseListener listener : listenerList) {
      listener.objectsRemoved(e);
    }
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectID the id of the database object that has been removed
   */
  protected void fireObjectRemoved(Integer objectID) {
    List<Integer> objectIDs = new ArrayList<Integer>();
    objectIDs.add(objectID);
    fireObjectsRemoved(objectIDs);
  }

  @Override
  public String getName() {
    return "database";
  }
}
