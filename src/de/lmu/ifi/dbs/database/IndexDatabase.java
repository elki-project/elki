package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * IndexDatabase is a database implementation which is supported by an index structure.
 *
 * @author Elke Achtert(<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class IndexDatabase<O extends DatabaseObject> extends AbstractDatabase<O> {

  /**
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index. " +
                                           "If this parameter is not set the index is hold in the main memory.";

  /**
   * The default pagesize.
   */
  public static final int DEFAULT_PAGE_SIZE = 4000;

  /**
   * Option string for parameter pagesize.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "<int>an integer value specifying the size of a page in bytes " +
                                           "(default is " + DEFAULT_PAGE_SIZE + " Byte)";

  /**
   * The default cachesize.
   */
  public static final int DEFAULT_CACHE_SIZE = 1000000;

  /**
   * Option string for parameter cachesize.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter cachesize.
   */
  public static final String CACHE_SIZE_D = "<int>an integer value specifying the size of the cache in bytes " +
                                            "(default is " + DEFAULT_CACHE_SIZE + " Byte)";

  /**
   * The name of the file for storing the index.
   */
  protected String fileName;

  /**
   * The size of a page in bytes.
   */
  protected int pageSize;

  /**
   * Tthe size of the cache.
   */
  protected int cacheSize;

  public IndexDatabase() {
    super();
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);

    this.content = new Hashtable<Integer, O>();
  }

  /**
   * @see Database#insert(de.lmu.ifi.dbs.data.DatabaseObject, java.util.Map)
   */
  public Integer insert(O object, Map<AssociationID, Object> associations) throws UnableToComplyException {
    Integer id = insert(object);
    setAssociations(id, associations);
    return id;
  }

  /**
   * Inserts the given object into the content map.
   *
   * @param object the object to be inserted
   * @return the ID assigned to the inserted object
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if insertion is not possible
   */
  protected Integer putToContent(O object) throws UnableToComplyException {
    Integer id = setNewID(object);
    content.put(id, object);
    return id;
  }

  /**
   * Removes the given object from the content map.
   *
   * @param id the id of the object to be removed
   */
  protected O removeFromContent(Integer id) {
    return content.remove(id);
  }

  /**
   * Removes all objects from the database that are equal to the given object.
   *
   * @param object the object to be removed from database
   */
  public final void delete(O object) {
    for (Integer id : content.keySet()) {
      if (content.get(id).equals(object)) {
        delete(id);
      }
    }
  }

  /**
   * Returns the number of objects contained in this Database.
   *
   * @return the number of objects in this Database
   */
  public final int size() {
    return content.size();
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param id               the ID of the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the
   *                         objects
   * @return a List of the query results
   */
  public <D extends Distance> List<QueryResult<D>> reverseKNNQuery(Integer id, int k, DistanceFunction<O, D> distanceFunction) {
    throw new UnsupportedOperationException("Not yet supported!");
  }

  /**
   * Returns the DatabaseObject represented by the specified id.
   *
   * @param id the id of the Object to be obtained from the Database
   * @return Object the Object represented by to the specified id in the
   *         Database
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
   * Sets the values for the parameter bulk.
   * If the parameters is not specified the default value is set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(super.setParameters(args));

    if (optionHandler.isSet(FILE_NAME_P)) {
      try {
        fileName = optionHandler.getOptionValue(FILE_NAME_P);
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      fileName = null;
    }

    if (optionHandler.isSet(PAGE_SIZE_P)) {
      try {
        pageSize = Integer.parseInt(optionHandler.getOptionValue(PAGE_SIZE_P));
        if (pageSize < 0)
          throw new IllegalArgumentException("RTreeDatabase: pagesize has to be greater than zero!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      pageSize = DEFAULT_PAGE_SIZE;
    }

    if (optionHandler.isSet(CACHE_SIZE_P)) {
      try {
        cacheSize = Integer.parseInt(optionHandler.getOptionValue(CACHE_SIZE_P));
        if (cacheSize < 0)
          throw new IllegalArgumentException("RTreeDatabase: cachesize has to be greater than zero!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      cacheSize = DEFAULT_CACHE_SIZE;
    }

    return remainingParameters;
  }

  /**
   * Returns the I/O-Access of this database.
   *
   * @return the I/O-Access of this database
   */
  public abstract long getIOAccess();

  /**
   * Resets the I/O-Access of this database.
   */
  public abstract void resetIOAccess();
}