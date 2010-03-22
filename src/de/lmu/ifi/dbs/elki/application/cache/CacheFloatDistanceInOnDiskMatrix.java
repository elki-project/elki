package de.lmu.ifi.dbs.elki.application.cache;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.NumberDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedFloatDistanceFunction;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Wrapper to convert a traditional text-serialized result into a on-disk matrix
 * for random access.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Number type
 */
public class CacheFloatDistanceInOnDiskMatrix<O extends DatabaseObject, D extends NumberDistance<D,N>, N extends Number> extends AbstractApplication {
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
  private final ObjectParameter<DatabaseConnection<O>> DATABASE_CONNECTION_PARAM = new ObjectParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
  
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
  private final ObjectParameter<DistanceFunction<O,D>> DISTANCE_PARAM = new ObjectParameter<DistanceFunction<O,D>>(DISTANCE_ID, DistanceFunction.class);

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O,D> distance;
  
  /**
   * Output file.
   */
  private File out;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public CacheFloatDistanceInOnDiskMatrix(Parameterization config) {
    super(config);
    if (config.grab(DATABASE_CONNECTION_PARAM)) {
      databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass(config);
    }
    if (config.grab(DISTANCE_PARAM)) {
      distance = DISTANCE_PARAM.instantiateClass(config);
    }
    if (config.grab(CACHE_PARAM)) {
      out = CACHE_PARAM.getValue();
    }
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

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(CacheFloatDistanceInOnDiskMatrix.class, args);
  }
}
