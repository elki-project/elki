package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject as element of the database
 */
public abstract class AbstractDatabase<O extends DatabaseObject> extends AbstractLoggable implements Database<O> {
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
   * Map to hold the object labels
   */
  private Map<Integer, String> objectlabels = null;

  /**
   * Map to hold the class labels
   */
  private Map<Integer, ClassLabel> classlabels = null;

  /**
   * Map to hold the external ids
   */
  private Map<Integer, String> externalids = null;

  /**
   * Holds the listener of this database.
   */
  protected List<DatabaseListener<O>> listenerList = new ArrayList<DatabaseListener<O>>();

  /**
   * Provides an abstract database including a mapping for associations based on
   * a {@link #Hashtable} and functions to get the next usable ID for insertion,
   * making IDs reusable after deletion of the entry.
   */
  protected AbstractDatabase() {
    super();
    content = new Hashtable<Integer, O>();
    counter = 0;
    reusableIDs = new ArrayList<Integer>();
  }

  /**
   * Calls for each object {@link #doInsert(Pair)} and notifies the listeners
   * about the new insertions.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  public void insert(List<Pair<O, DatabaseObjectMetadata>> objectsAndAssociationsList) throws UnableToComplyException {
    List<Integer> ids = new ArrayList<Integer>(objectsAndAssociationsList.size());
    for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectsAndAssociationsList) {
      Integer id = doInsert(objectAndAssociations);
      ids.add(id);
    }
    // notify listeners
    fireObjectsInserted(ids);
  }

  /**
   * Calls {@link #doInsert(Pair)} and notifies the listeners about the new
   * insertion.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  public Integer insert(Pair<O, DatabaseObjectMetadata> objectAndAssociations) throws UnableToComplyException {
    Integer id = doInsert(objectAndAssociations);
    // notify listeners
    fireObjectInserted(id);

    return id;
  }

  /**
   * Inserts the given object into the database.
   * 
   * @param objectAndAssociations the object and its associations to be inserted
   * @return the ID assigned to the inserted object
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  private Integer doInsert(Pair<O, DatabaseObjectMetadata> objectAndAssociations) throws UnableToComplyException {
    O object = objectAndAssociations.getFirst();
    // insert object
    Integer id = setNewID(object);
    content.put(id, object);
    // insert associations
    DatabaseObjectMetadata associations = objectAndAssociations.getSecond();
    if(associations != null) {
      if(associations.objectlabel != null) {
        setObjectLabel(id, associations.objectlabel);
      }
      if(associations.classlabel != null) {
        setClassLabel(id, associations.classlabel);
      }
      if(associations.externalid != null) {
        setExternalID(id, associations.externalid);
      }
    }

    return id;
  }

  /**
   * Searches for all equal objects in the database and calls
   * {@link #delete(Integer)} for each.
   */
  public void delete(O object) {
    for(Integer id : content.keySet()) {
      if(content.get(id).equals(object)) {
        delete(id);
      }
    }
  }

  /**
   * Calls {@link #doDelete(Integer)} and notifies the listeners about the
   * deletion.
   */
  public O delete(Integer id) {
    O object = doDelete(id);
    // notify listeners
    fireObjectRemoved(id);
    return object;
  }

  /**
   * Removes and returns the object with the given id from the database.
   * 
   * @param id the id of the object to be removed from the database
   * @return the object that has been removed
   */
  private O doDelete(Integer id) {
    O object = content.remove(id);
    restoreID(id);
    if(objectlabels != null) {
      objectlabels.remove(id);
    }
    if(classlabels != null) {
      classlabels.remove(id);
    }
    if(externalids != null) {
      externalids.remove(id);
    }

    return object;
  }

  public final int size() {
    return content.size();
  }

  public final O get(Integer id) {
    try {
      return content.get(id);
    }
    catch(RuntimeException e) {
      if(id == null) {
        throw new UnsupportedOperationException("AbstractDatabase.get(null) called!");
      }
      // throw e upwards.
      throw e;
    }
  }

  /**
   * Returns an iterator iterating over all keys of the database.
   * 
   * @return an iterator iterating over all keys of the database
   */
  public final Iterator<Integer> iterator() {
    if(debug) {
      // This is an ugly hack to get more stable results.
      ArrayList<Integer> ids = new ArrayList<Integer>(content.keySet());
      Collections.sort(ids);
      return ids.iterator();
    }
    else {
      return content.keySet().iterator();
    }
  }

  /** {@inheritDoc} */
  @Override
  public ClassLabel getClassLabel(Integer id) {
    if(classlabels == null) {
      return null;
    }
    return classlabels.get(id);
  }

  /** {@inheritDoc} */
  @Override
  public String getExternalID(Integer id) {
    if(externalids == null) {
      return null;
    }
    return externalids.get(id);
  }

  /** {@inheritDoc} */
  @Override
  public String getObjectLabel(Integer id) {
    if(objectlabels == null) {
      return null;
    }
    return objectlabels.get(id);
  }

  /** {@inheritDoc} */
  @Override
  public void setClassLabel(Integer id, ClassLabel label) {
    if(classlabels == null) {
      classlabels = new HashMap<Integer, ClassLabel>(size());
    }
    classlabels.put(id, label);
  }

  /** {@inheritDoc} */
  @Override
  public void setExternalID(Integer id, String externalid) {
    if(externalids == null) {
      externalids = new HashMap<Integer, String>(size());
    }
    externalids.put(id, externalid);
  }

  /** {@inheritDoc} */
  @Override
  public void setObjectLabel(Integer id, String label) {
    if(objectlabels == null) {
      objectlabels = new HashMap<Integer, String>(size());
    }
    objectlabels.put(id, label);
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
    reusableIDs.add(id);
  }

  public Database<O> partition(List<Integer> ids) throws UnableToComplyException {
    Map<Integer, List<Integer>> partitions = new HashMap<Integer, List<Integer>>();
    partitions.put(0, ids);
    return partition(partitions, null, null).get(0);
  }

  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException {
    return partition(partitions, null, null);
  }

  @SuppressWarnings("unchecked")
  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions, Class<? extends Database<O>> dbClass, Collection<Pair<OptionID, Object>> dbParameters) throws UnableToComplyException {
    if(dbClass == null) {
      dbClass = ClassGenericsUtil.uglyCrossCast(this.getClass(), Database.class);
      dbParameters = getParameters();
    }

    Map<Integer, Database<O>> databases = new Hashtable<Integer, Database<O>>();
    for(Integer partitionID : partitions.keySet()) {
      List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList = new ArrayList<Pair<O, DatabaseObjectMetadata>>();
      List<Integer> ids = partitions.get(partitionID);
      for(Integer id : ids) {
        O object = get(id);
        DatabaseObjectMetadata associations = new DatabaseObjectMetadata(this, id);
        objectAndAssociationsList.add(new Pair<O, DatabaseObjectMetadata>(object, associations));
      }

      Database<O> database;
      ListParameterization config = new ListParameterization(dbParameters);
      try {
        database = ClassGenericsUtil.tryInstanciate(Database.class, dbClass, config);
      }
      catch(Exception e) {
        throw new UnableToComplyException(e);
      }

      database.insert(objectAndAssociationsList);
      databases.put(partitionID, database);
    }
    return databases;
  }

  protected abstract Collection<Pair<OptionID, Object>> getParameters();

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
   * The list is not affected of any changes made to the database in the future
   * nor vice versa.
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
      if(NumberVector.class.isInstance(entry)) {
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
  protected List<O> getObjects(List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList) {
    List<O> objects = new ArrayList<O>(objectAndAssociationsList.size());
    for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectAndAssociationsList) {
      objects.add(objectAndAssociations.getFirst());
    }
    return objects;
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> kNNQueryForID(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    return kNNQueryForObject(get(id), k, distanceFunction);
  }

  public <D extends Distance<D>> List<DistanceResultPair<D>> rangeQuery(Integer id, String epsilon, DistanceFunction<O, D> distanceFunction) {
    return rangeQuery(id, distanceFunction.valueOf(epsilon), distanceFunction);
  }

  /**
   * Adds a listener for the <code>DatabaseEvent</code> posted after the
   * database changes.
   * 
   * @param l the listener to add
   * @see #removeDatabaseListener
   */
  public void addDatabaseListener(DatabaseListener<O> l) {
    listenerList.add(l);
  }

  /**
   * Removes a listener previously added with <code>addTreeModelListener</code>.
   * 
   * @param l the listener to remove
   * @see #addDatabaseListener
   */
  public void removeDatabaseListener(DatabaseListener<O> l) {
    listenerList.remove(l);
  }

  /**
   * Notifies all listeners that have registered interest for notification on
   * this event type.
   * 
   * @param objectIDs the ids of the database objects that have been removed
   */
  protected void fireObjectsChanged(List<Integer> objectIDs) {
    if(listenerList.isEmpty()) {
      return;
    }
    DatabaseEvent<O> e = new DatabaseEvent<O>(this, objectIDs);
    for(DatabaseListener<O> listener : listenerList) {
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
    if(listenerList.isEmpty()) {
      return;
    }
    DatabaseEvent<O> e = new DatabaseEvent<O>(this, objectIDs);
    for(DatabaseListener<O> listener : listenerList) {
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
    if(listenerList.isEmpty()) {
      return;
    }
    DatabaseEvent<O> e = new DatabaseEvent<O>(this, objectIDs);
    for(DatabaseListener<O> listener : listenerList) {
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