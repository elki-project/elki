package de.lmu.ifi.dbs.elki.application.cache;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.FloatDistance;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedFloatDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Wrapper to convert a traditional text-serialized result into a on-disk matrix
 * for random access.
 * 
 * @author Erich Schubert
 * 
 */
public class CacheFloatDistanceInOnDiskMatrix<O extends DatabaseObject, N extends NumberDistance<N,D>, D extends Number> extends AbstractApplication {
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
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckSymmetry = false;

  /**
   * OptionID for {@link #DISTANCE_PARAM}
   */
  public static final OptionID DISTANCE_ID = OptionID.getOrCreateOptionID("loader.distance", "Distance function to cache.");

  /**
   * Storage size: 4 bytes floats
   */
  private static final int FLOAT_SIZE = 4;

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.distance}
   * </p>
   */
  private final ClassParameter<DistanceFunction<O,N>> DISTANCE_PARAM = new ClassParameter<DistanceFunction<O,N>>(DISTANCE_ID, DistanceFunction.class);

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O,N> distance;

  /**
   * Constructor.
   */
  public CacheFloatDistanceInOnDiskMatrix() {
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
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedFloatDistanceFunction.FLOAT_CACHE_MAGIC, 0, FLOAT_SIZE, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix.", e);
    }

    for(Integer id1 : database) {
      for(Integer id2 : database) {
        if(id2 >= id1) {
          byte[] data = new byte[8];
          float d = distance.distance(id1, id2).floatValue();
          ByteArrayUtil.writeFloat(data, 0, d);
          if(debugExtraCheckSymmetry) {
            float d2 = distance.distance(id2, id1).floatValue();
            if(Math.abs(d-d2) > 0.0000001) {
              logger.warning("Distance function doesn't appear to be symmetric!");
            }            
          }
          try {
            matrix.writeRecord(id1, id2, data);
            if(debugExtraCheckWrites) {
              byte[] data2 = matrix.readRecord(id1, id2);
              float test = ByteArrayUtil.readFloat(data2, 0);
              if(test != d) {
                logger.warning("Distance read from file differs: " + test + " vs. " + d);
              }
            }
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + id1 + "," + id2 + " to matrix.", e);
          }
        }
      }
    }
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // Setup database connection.
    databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass();
    addParameterizable(databaseConnection);
    remainingParameters = databaseConnection.setParameters(remainingParameters);

    // Pass on parameters to distance function.
    distance = DISTANCE_PARAM.instantiateClass();
    addParameterizable(distance);
    remainingParameters = distance.setParameters(remainingParameters);
    
    if(remainingParameters.size() != 0) {
      LoggingUtil.warning("Unnecessary parameters specified: " + remainingParameters);
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
    new CacheFloatDistanceInOnDiskMatrix<DatabaseObject,FloatDistance,Float>().runCLIApplication(args);
  }
}
