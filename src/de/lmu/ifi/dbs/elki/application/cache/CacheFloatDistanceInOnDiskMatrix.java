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
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedFloatDistanceFunction;
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
 */
public class CacheFloatDistanceInOnDiskMatrix<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CacheFloatDistanceInOnDiskMatrix.class);

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.diskcache}
   * </p>
   */
  public static final OptionID CACHE_ID = OptionID.getOrCreateOptionID("loader.diskcache", "File name of the disk cache to create.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.distance}
   * </p>
   */
  public static final OptionID DISTANCE_ID = OptionID.getOrCreateOptionID("loader.distance", "Distance function to cache.");

  /**
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckSymmetry = false;

  /**
   * Storage size: 4 bytes floats
   */
  private static final int FLOAT_SIZE = 4;

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
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param databaseConnection Database connection
   * @param normalization Normalization
   * @param distance Distance function
   * @param out Matrix output file
   */
  public CacheFloatDistanceInOnDiskMatrix(boolean verbose, DatabaseConnection<O> databaseConnection, Normalization<O> normalization, DistanceFunction<O, D> distance, File out) {
    super(verbose);
    this.databaseConnection = databaseConnection;
    this.normalization = normalization;
    this.distance = distance;
    this.out = out;
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
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedFloatDistanceFunction.FLOAT_CACHE_MAGIC, 0, FLOAT_SIZE, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix.", e);
    }

    for(DBID id1 : database) {
      for(DBID id2 : database) {
        if(id2.getIntegerID() >= id1.getIntegerID()) {
          float d = distanceQuery.distance(id1, id2).floatValue();
          if(debugExtraCheckSymmetry) {
            float d2 = distanceQuery.distance(id2, id1).floatValue();
            if(Math.abs(d - d2) > 0.0000001) {
              logger.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          try {
            matrix.getRecordBuffer(id1.getIntegerID(), id2.getIntegerID()).putFloat(d);
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + id1 + "," + id2 + " to matrix.", e);
          }
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
    /**
     * Holds the database connection to have the algorithm run with.
     */
    private DatabaseConnection<O> databaseConnection = null;

    /**
     * A normalization - per default no normalization is used.
     */
    private Normalization<O> normalization = null;

    /**
     * Distance function that is to be cached.
     */
    private DistanceFunction<O, D> distance = null;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Database connection parameter
      final ObjectParameter<DatabaseConnection<O>> dcpar = new ObjectParameter<DatabaseConnection<O>>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dcpar)) {
        databaseConnection = dcpar.instantiateClass(config);
      }
      // Normalization parameter
      final ObjectParameter<Normalization<O>> npar = new ObjectParameter<Normalization<O>>(OptionID.NORMALIZATION, Normalization.class, true);
      if(config.grab(npar)) {
        normalization = npar.instantiateClass(config);
      }
      // Distance function parameter
      final ObjectParameter<DistanceFunction<O, D>> dpar = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_ID, DistanceFunction.class);
      if(config.grab(dpar)) {
        distance = dpar.instantiateClass(config);
      }
      // Output file parameter
      final FileParameter cpar = new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE);
      if(config.grab(cpar)) {
        out = cpar.getValue();
      }

    }

    @Override
    protected CacheFloatDistanceInOnDiskMatrix<O, D> makeInstance() {
      return new CacheFloatDistanceInOnDiskMatrix<O, D>(verbose, databaseConnection, normalization, distance, out);
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
