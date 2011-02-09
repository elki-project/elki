package de.lmu.ifi.dbs.elki.application.cache;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
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
 * @apiviz.has OnDiskUpperTriangleMatrix
 * @apiviz.has DistanceFunction
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Number type
 */
public class CacheDoubleDistanceInOnDiskMatrix<O extends DatabaseObject, D extends NumberDistance<D, N>, N extends Number> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CacheDoubleDistanceInOnDiskMatrix.class);

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
  private static final boolean debugExtraCheckSymmetry = false;

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
  private final ObjectParameter<DistanceFunction<O, D>> DISTANCE_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_ID, DistanceFunction.class);

  /**
   * Database normalization
   */
  final ObjectParameter<Normalization<O>> NORMALIZATION_PARAM = new ObjectParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private DatabaseConnection<O> databaseConnection;

  /**
   * A normalization - per default no normalization is used.
   */
  private Normalization<O> normalization = null;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O, D> distance;

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
  public CacheDoubleDistanceInOnDiskMatrix(Parameterization config) {
    super(config);
    config = config.descend(this);
    if(config.grab(DATABASE_CONNECTION_PARAM)) {
      databaseConnection = DATABASE_CONNECTION_PARAM.instantiateClass(config);
    }
    if(config.grab(NORMALIZATION_PARAM)) {
      normalization = NORMALIZATION_PARAM.instantiateClass(config);
    }
    if(config.grab(DISTANCE_PARAM)) {
      distance = DISTANCE_PARAM.instantiateClass(config);
    }
    if(config.grab(CACHE_PARAM)) {
      out = CACHE_PARAM.getValue();
    }
  }

  @Override
  public void run() {
    Database<O> database = databaseConnection.getDatabase(normalization);
    DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distance);

    DBIDs ids = database.getIDs();
    int matrixsize = 0;
    for(DBID id : ids) {
      matrixsize = Math.max(matrixsize, id.getIntegerID() + 1);
      if(id.getIntegerID() < 0) {
        throw new AbortException("OnDiskMatrixCache does not allow negative DBIDs.");
      }
    }

    OnDiskUpperTriangleMatrix matrix;
    try {
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedDoubleDistanceFunction.DOUBLE_CACHE_MAGIC, 0, 8, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix.", e);
    }

    for(DBID id1 : database) {
      for(DBID id2 : database) {
        if(id2.getIntegerID() >= id1.getIntegerID()) {
          double d = distanceQuery.distance(id1, id2).doubleValue();
          if(debugExtraCheckSymmetry) {
            double d2 = distanceQuery.distance(id2, id1).doubleValue();
            if(Math.abs(d - d2) > 0.0000001) {
              logger.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          try {
            matrix.getRecordBuffer(id1.getIntegerID(), id2.getIntegerID()).putDouble(d);
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
    runCLIApplication(CacheDoubleDistanceInOnDiskMatrix.class, args);
  }
}
