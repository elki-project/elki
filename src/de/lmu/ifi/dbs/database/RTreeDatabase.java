package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.index.spatial.SpatialIndex;
import de.lmu.ifi.dbs.index.spatial.rtree.FlatRTree;
import de.lmu.ifi.dbs.index.spatial.rtree.RTree;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

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
   * Description for parameter filename.
   */
  public static final String FILE_NAME_D = "<name>a file name specifying the name of the file storing the index. " +
                                           "If this parameter is not set the RTree is hold in the main memory.";

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
   * Option string for parameter flat.
   */
  public static final String FLAT_DIRECTORY_F = "flat";

  /**
   * Description for parameter flat.
   */
  public static final String FLAT_DIRECTORY_D = "flag to specify a flat directory (default is a not flat directory)";

  /**
   * The name of the file for storing the RTree.
   */
  private String fileName;

  /**
   * The size of a page in bytes.
   */
  private int pageSize;

  /**
   * Tthe size of the cache.
   */
  private int cacheSize;

  /**
   * If true, the RTree will have a flat directory
   */
  private boolean flatDirectory;



  /**
   * Empty constructor, creates a new RTreeDatabase.
   */
  public RTreeDatabase() {
    super();
    parameterToDescription.put(FILE_NAME_P + OptionHandler.EXPECTS_VALUE, FILE_NAME_D);
    parameterToDescription.put(PAGE_SIZE_P + OptionHandler.EXPECTS_VALUE, PAGE_SIZE_D);
    parameterToDescription.put(CACHE_SIZE_P + OptionHandler.EXPECTS_VALUE, CACHE_SIZE_D);
    parameterToDescription.put(FLAT_DIRECTORY_F, FLAT_DIRECTORY_D);

    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Returns the specific spatial index object for this database.
   *
   * @return the spatial index for this database
   */
  public SpatialIndex createSpatialIndex(final FeatureVector[] objects) {
    if (flatDirectory) {
      return new FlatRTree(objects, fileName, pageSize, cacheSize);
    }
    else {
      return new RTree(objects, fileName, pageSize, cacheSize);
    }
  }

  /**
   * Returns the spatial index object with the specified parameters
   * for this database.
   *
   * @param dimensionality the dimensionality of the objects to be indexed
   */
  public SpatialIndex createSpatialIndex(int dimensionality) {
    if (flatDirectory) {
      return new FlatRTree(dimensionality, fileName, pageSize, cacheSize);
    }
    else {
      return new RTree(dimensionality, fileName, pageSize, cacheSize);
    }
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
   * Sets the values for the parameters filename, pagesize, cachesize and flat
   * if specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

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

    flatDirectory = optionHandler.isSet(FLAT_DIRECTORY_F);

    return remainingParameters;
  }


}
