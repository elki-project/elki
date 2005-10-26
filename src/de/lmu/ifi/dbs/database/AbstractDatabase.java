package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.IDPair;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

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
public abstract class AbstractDatabase<O extends MetricalObject> implements Database<O> {
  /**
   * Flag for caching distances.
   */
  public static final String CACHE_F = "distancecache";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_D = "flag to allow caching of distance values";

  /**
   * Map to hold association maps.
   */
  private final Map<AssociationID, Map<Integer, Object>> associations;

  /**
   * The cache for caching distances.
   */
  /**
   * The map holding the objects of this cache.
   */
  private Map<Class, Map<IDPair, Distance>> caches;

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
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler to handle options, optionHandler should be initialized in any non-abstract class
   * extending this class.
   */
  protected OptionHandler optionHandler;


  /**
   * Provides an abstract database including a mapping for associations based
   * on a Hashtable and functions to get the next usable ID for insertion,
   * making IDs reusable after deletion of the entry. Make sure to delete any
   * associations when deleting an entry (e.g. by calling
   * {@link #deleteAssociations(Integer) deleteAssociations(id)}).
   */
  protected AbstractDatabase() {
    associations = new Hashtable<AssociationID, Map<Integer, Object>>();
    counter = 1;
    reachedLimit = false;
    reusableIDs = new ArrayList<Integer>();

    parameterToDescription.put(CACHE_F, CACHE_D);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#associate(AssociationID, Integer, Object)
   */
  public void associate(final AssociationID associationID, final Integer objectID, final Object association) {
    if (!associations.containsKey(associationID)) {
      associations.put(associationID, new Hashtable<Integer, Object>());
    }
    associations.get(associationID).put(objectID, association);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#getAssociation(AssociationID, Integer)
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
   * Provides a new id for the specified metrical object suitable as key for a
   * new insertion and sets this id in the specified metrical object.
   *
   * @return a new id suitable as key for a new insertion
   * @throws UnableToComplyException if the database has reached the limit and, therefore, new
   *                                 insertions are not possible
   */
  protected Integer setNewID(O object) throws UnableToComplyException {
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
  protected Map<AssociationID, Object> getAssociations(final Integer id) {
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
   * @see Database#partition(Map)
   */
  @SuppressWarnings("unchecked")
  public Map<Integer, Database<O>> partition(Map<Integer, List<Integer>> partitions) throws UnableToComplyException {
    Map<Integer, Database<O>> databases = new Hashtable<Integer, Database<O>>();
    for (Integer partitionID : partitions.keySet()) {
      List<Map<AssociationID, Object>> associations = new ArrayList<Map<AssociationID, Object>>();
      List<O> objects = new ArrayList<O>();
      List<Integer> ids = partitions.get(partitionID);
      for (Integer id : ids) {
        objects.add((O) get(id).copy());
        associations.add(getAssociations(id));
      }

      Database<O> database;
      try {
        database = (Database<O>) getClass().newInstance();
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
    String[] remainingOptions = optionHandler.grabOptions(args);

    if (optionHandler.isSet(CACHE_F)) {
      caches = new HashMap<Class, Map<IDPair, Distance>>();
    }

    return remainingOptions;

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
   * Checks whether an association is set for every id in the database.
   *
   * @param associationID an association id to be checked
   * @return true, if the association is set for every id in the database,
   *         false otherwise
   */
  public boolean isSet(AssociationID associationID) {
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
   * Returns the cached distance between the two objcts specified by their obejct ids
   * if caching is enabled, null otherwise.
   *
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objcts specified by their obejct ids
   */
  public <D extends Distance> D cachedDistance(DistanceFunction<O,D> distanceFunction, Integer id1, Integer id2) {
    if (caches == null)
      return null;

    Map<IDPair, Distance> cache = caches.get(distanceFunction.getClass());
    if (cache == null) {
      cache = new HashMap<IDPair, Distance>();
      caches.put(distanceFunction.getClass(), cache);
    }

    //noinspection unchecked
    D distance = (D) cache.get(new IDPair(id1, id2));
    if (distance != null) {
      return distance;
    }
    else {
      distance = distanceFunction.distance(get(id1), get(id2));
      cache.put(new IDPair(id1, id2), distance);
      return distance;
    }
  }

}
