package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry. Make sure to delete any associations when deleting an entry (e.g. by
 * calling {@link #deleteAssociations(Integer) deleteAssociations(id)}).
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class AbstractDatabase<T extends MetricalObject> implements Database<T> {

  /**
   * Map to hold association maps.
   */
  private final Map<String, Map<Integer, Object>> associations;

  /**
   * Counter to provide a new Integer id.
   */
  private int counter;

  /**
   * Provides a list of reusable ids.
   */
  private List<Integer> reusableIDs;

  /**
   * Whether the limit is reached.
   */
  private boolean reachedLimit;

  /**
   * Holds the parameter settings.
   */
  private String[] parameters;

  /**
   * Provides an abstract database including a mapping for associations based
   * on a Hashtable and functions to get the next usable ID for insertion,
   * making IDs reusable after deletion of the entry. Make sure to delete any
   * associations when deleting an entry (e.g. by calling
   * {@link #deleteAssociations(Integer) deleteAssociations(id)}).
   */
  protected AbstractDatabase() {
    associations = new Hashtable<String, Map<Integer, Object>>();
    counter = 1;
    reachedLimit = false;
    reusableIDs = new ArrayList<Integer>();
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#associate(java.lang.String,
   *      java.lang.Integer, java.lang.Object)
   */
  public void associate(String associationID, Integer objectID, Object association) {
    if (!associations.containsKey(associationID)) {
      associations.put(associationID, new Hashtable<Integer, Object>());
    }
    associations.get(associationID).put(objectID, association);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#getAssociation(java.lang.String,
   *      java.lang.Integer)
   */
  public Object getAssociation(String associationID, Integer objectID) {
    if (associations.containsKey(associationID)) {
      return associations.get(associationID).get(objectID);
    }
    else {
      return null;
    }
  }

  /**
   * Provides a new id for the specified metrical object suitable as key for a
   * new insertion and sets this id in the specified metrical object.
   *
   * @return a new id suitable as key for a new insertion
   * @throws UnableToComplyException if the database has reached the limit and, therefore, new
   *                                 insertions are not possible
   */
  protected Integer setNewID(T object) throws UnableToComplyException {
    if (reachedLimit && reusableIDs.size() == 0) {
      throw new UnableToComplyException("Database reached limit of storage.");
    }
    else {
      Integer id = counter;
      if (counter < Integer.MAX_VALUE && !reachedLimit) {
        counter++;
      }
      else {
        if (reusableIDs.size() > 0) {
          counter = reusableIDs.remove(0);
        }
        else {
          reachedLimit = true;
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
  protected void restoreID(Integer id) {
    {
      reusableIDs.add(id);
    }
  }

  /**
   * Deletes associations for the given id if there are any.
   *
   * @param id id of which all associations are to be deleted
   */
  protected void deleteAssociations(Integer id) {
    for (String s : associations.keySet()) {
      associations.get(s).remove(id);
    }
  }

  /**
   * Returns all associations for a given ID.
   *
   * @param id the id for which the associations are to be returned
   * @return all associations for a given ID
   */
  protected Map<String, Object> getAssociations(Integer id) {
    Map<String, Object> idAssociations = new Hashtable<String, Object>();
    for (String label : associations.keySet()) {
      if (associations.get(label).containsKey(id)) {
        idAssociations.put(label, associations.get(label).get(id));
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
  protected void setAssociations(Integer id, Map<String, Object> idAssociations) {
    for (String label : idAssociations.keySet()) {
      associate(label, id, idAssociations.get(label));
    }
  }

  /**
   * @see Database#partition(java.util.Map)
   */
  @SuppressWarnings("unchecked")
  public Map<Integer, Database<T>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException {
    Map<Integer, Database<T>> databases = new Hashtable<Integer, Database<T>>();
    for (Integer partitionID : partitions.keySet()) {
      List<Map<String, Object>> associations = new ArrayList<Map<String, Object>>();
      List<T> objects = new ArrayList<T>();
      List<Integer> ids = partitions.get(partitionID);
      for (Integer id : ids) {
        objects.add((T) get(id).copy());
        associations.add(getAssociations(id));
      }

      Database<T> database;
      try {
        database = (Database<T>) getClass().newInstance();
        database.setParameters(getParameters());
        database.insert(objects, associations);
        databases.put(partitionID, database);
      }
      catch (InstantiationException e) {
        throw new UnableToComplyException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new UnableToComplyException(e.getMessage());
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
  public String[] setParameters(String[] args) {
    this.parameters = Util.copy(args);
    return args;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getParameterSettings() {
    return new ArrayList<AttributeSettings>();
  }

  /**
   * Returns a copy of the parameter array as it has been given in the
   * setParameters method.
   *
   * @return a copy of the parameter array as it has been given in the
   *         setParameters method
   */
  public String[] getParameters() {
    return Util.copy(this.parameters);
  }

  /**
   * Checks whether an association is set for every id
   * in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database, false otherwise
   */
  public boolean isSet(String associationID) {
    boolean isSet = true;
    for (Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext() && isSet;) {
      Integer id = dbIter.next();
      isSet = isSet && this.getAssociation(associationID, id) != null;
    }
    return isSet;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#randomSample(int, long)
   */
  public List<Integer> randomSample(int k, long seed) {
    if (k < 0) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k);
    }

    List<Integer> sample = new ArrayList<Integer>(k);
    Integer[] ids = new Integer[this.size()];
    // get all ids
    {
      int i = 0;
      for (Iterator<Integer> dbIter = this.iterator(); dbIter.hasNext(); i++) {
        ids[i] = dbIter.next();
      }
    }
    Random random = new Random(seed);
    for (int i = 0; i < k; i++) {
      sample.add(ids[random.nextInt(ids.length)]);
    }
    return sample;
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#kNNQuery(java.lang.Integer, int, de.lmu.ifi.dbs.distance.DistanceFunction)
   */
  public <D extends Distance> List<QueryResult<D>> kNNQuery(Integer id, int k, DistanceFunction<T,D> distanceFunction) {
    T queryObject = this.get(id);
    return kNNQuery(queryObject, k, distanceFunction);
  }

  /**
   * @see Database#dimensionality()
   */
  public int dimensionality() throws UnsupportedOperationException {
    Iterator<Integer> iter = this.iterator();
    if (iter.hasNext()) {
      T entry = this.get(iter.next());
      if (entry instanceof FeatureVector) {
        //noinspection unchecked
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


}
