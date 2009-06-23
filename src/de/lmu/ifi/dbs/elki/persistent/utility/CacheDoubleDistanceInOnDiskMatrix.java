package de.lmu.ifi.dbs.elki.persistent.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.wrapper.AbstractWrapper;

/**
 * Wrapper to convert a traditional text-serialized result into a on-disk matrix
 * for random access.
 * 
 * @author Erich Schubert
 * 
 */
public class CacheDoubleDistanceInOnDiskMatrix<O extends DatabaseObject> extends AbstractWrapper {
  /**
   * Parameter to specify the database connection to be used, must extend
   * {@link de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection}.
   * <p>
   * Key: {@code -dbc}
   * </p>
   * <p>
   * Default value: {@link FileBasedDatabaseConnection}
   * </p>
   */
  private final ClassParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ClassParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class.getName());
  
  /**
   * OptionID for {@link #CACHE_PARAM}
   */
  public static final OptionID CACHE_ID = OptionID.getOrCreateOptionID("loader.diskcache", "File name of the disk cache to create.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.diskcache}
   * </p>
   */
  private final FileParameter CACHE_PARAM = new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE);

  /**
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckWrites = false;

  /**
   * OptionID for {@link #DISTANCE_PARAM}
   */
  public static final OptionID DISTANCE_ID = OptionID.getOrCreateOptionID("loader.distance", "Distance function to cache.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.distance}
   * </p>
   */
  private final ClassParameter<DistanceFunction<O,DoubleDistance>> DISTANCE_PARAM = new ClassParameter<DistanceFunction<O,DoubleDistance>>(DISTANCE_ID, DistanceFunction.class);

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O,DoubleDistance> distance;

  /**
   * Constructor.
   */
  public CacheDoubleDistanceInOnDiskMatrix() {
    super();
    addOption(DATABASE_CONNECTION_PARAM);
    addOption(CACHE_PARAM);
    addOption(DISTANCE_PARAM);
  }

  @Override
  public void run() {
    Database<O> database = databaseConnection.getDatabase(null);
    distance.setDatabase(database, false, false);
    
    Collection<Integer> ids = database.getIDs();
    int matrixsize = 0;
    for(Integer id : ids) {
      matrixsize = Math.max(matrixsize, id + 1);
    }

    File out;
    try {
      out = CACHE_PARAM.getValue();
    }
    catch(ParameterException e) {
      throw new AbortException("Output filename not given.", e);
    }
    OnDiskUpperTriangleMatrix matrix;
    try {
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedDoubleDistanceFunction.DOUBLE_CACHE_MAGIC, 0, 8, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix: " + e.getMessage(), e);
    }

    for(Integer id1 : database) {
      for(Integer id2 : database) {
        if(id2 >= id1) {
          byte[] data = new byte[8];
          double d = distance.distance(id1, id2).getValue();
          ByteArrayUtil.writeDouble(data, 0, d);
          try {
            matrix.writeRecord(id1, id2, data);
            if(debugExtraCheckWrites) {
              byte[] data2 = matrix.readRecord(id1, id2);
              double test = ByteArrayUtil.readDouble(data2, 0);
              if(test != d) {
                logger.warning("Distance read from file differs!" + test + " vs. " + d);
              }
            }
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + id1 + "," + id2 + " to matrix: " + e.getMessage(), e);
          }
        }
      }
    }
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    super.setParameters(args);
    String[] remainingParameters = super.getRemainingParameters().toArray(new String[0]);

    // Setup database connection.
    databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass();
    addParameterizable(databaseConnection);
    remainingParameters = databaseConnection.setParameters(remainingParameters);

    // Pass on parameters to distance function.
    distance = DISTANCE_PARAM.instantiateClass();
    addParameterizable(distance);
    remainingParameters = distance.setParameters(remainingParameters);
    
    if(remainingParameters.length != 0) {
      LoggingUtil.warning("Unnecessary parameters specified: " + Arrays.asList(remainingParameters));
    }

    super.rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args
   */
  public static void main(String[] args) {
    new CacheDoubleDistanceInOnDiskMatrix<DatabaseObject>().runCLIWrapper(args);
  }
}
