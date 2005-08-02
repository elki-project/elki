package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rtree.RTree;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Hashtable;
import java.util.Map;
import java.util.Arrays;

/**
 * RTreeDatabase is a database implementation which is supported by a
 * RTree index structure.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RTreeDatabase extends SpatialIndexDatabase {
  /**
   * Option string for parameter fileName.
   */
  public static final String FILE_NAME_P = "filename";

  /**
   * Description for parameter fileName.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index";

  /**
   * Option string for parameter fileName.
   */
  public static final String PAGE_SIZE_P = "pagesize";

  /**
   * Description for parameter filename.
   */
  public static final String PAGE_SIZE_D = "<pagesize>an integer value specifying the size of a page in bytes (default is 4 kByte";

  /**
   * Option string for parameter fileName.
   */
  public static final String CACHE_SIZE_P = "cachesize";

  /**
   * Description for parameter filename.
   */
  public static final String CACHE_SIZE_D = "<cachesize>an integer value specifying the size of the cache in bytes (default is 1 MByte";

  /**
   * Option string for parameter fileName.
   */
  public static final String FLAT_DIRECTORY_P = "flat";

  /**
   * Description for parameter filename.
   */
  public static final String FLAT_DIRECTORY_D = "<flat>a boolean value specifying a flat directory (default is false)";


  /**
   * OptionHandler for handling options.
   */
  private OptionHandler optionHandler;

  /**
   * The name of the file for storing the RTree.
   */
  private String fileName;

  /**
   * The size of a page in bytes.
   */
  private int pageSize = 4000;

  /**
   * Tthe size of the cache.
   */
  private int cacheSize = 8000;
//  private int cacheSize = 1000000;

  /**
   * If true, the RTree will have a flat directory
   */
  private boolean flatDirectory = false;

  /**
   * Empty constructor, creates a new RTreeDatabase.
   */
  public RTreeDatabase() {
    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);
    parameterToDescription.put(FLAT_DIRECTORY_P + OptionHandler.EXPECTS_VALUE, FLAT_DIRECTORY_D);
    optionHandler = new OptionHandler(parameterToDescription, "");
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex createSpatialIndex(final RealVector[] objects, final int[] ids) {
    return new RTree(objects, ids, fileName, pageSize, cacheSize, flatDirectory);
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex createSpatialIndex(int dimensionality) {
    return new RTree(dimensionality, fileName, pageSize, cacheSize, flatDirectory);
  }

  /**
   * @see de.lmu.ifi.dbs.database.Database#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(RTreeDatabase.class.getName());
    description.append(" holds all the data in a RTree index structure.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingOptions = optionHandler.grabOptions(args);
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

    if (optionHandler.isSet(PAGE_SIZE_P)) {
      try {
        pageSize = Integer.parseInt(optionHandler.getOptionValue(PAGE_SIZE_P));
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

    if (optionHandler.isSet(CACHE_SIZE_P)) {
      try {
        cacheSize = Integer.parseInt(optionHandler.getOptionValue(CACHE_SIZE_P));
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

    if (optionHandler.isSet(FLAT_DIRECTORY_P)) {
      try {
        flatDirectory = Boolean.parseBoolean(optionHandler.getOptionValue(FLAT_DIRECTORY_P));
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }

    return remainingOptions;
  }


}
