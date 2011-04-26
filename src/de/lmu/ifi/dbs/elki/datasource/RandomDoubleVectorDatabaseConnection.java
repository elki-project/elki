package de.lmu.ifi.dbs.elki.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Produce a database of random double vectors with each dimension in [0:1]
 * 
 * @author Erich Schubert
 */
public class RandomDoubleVectorDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Dimensionality
   */
  protected int dim = -1;

  /**
   * Size of database to generate
   */
  protected int size = -1;

  /**
   * Seed to use
   */
  protected Long seed;

  /**
   * Constructor.
   * 
   * @param database
   * @param dim Dimensionality
   * @param size Database size
   * @param seed Random seed
   * @param filters
   */
  public RandomDoubleVectorDatabaseConnection(Database database, int dim, int size, Long seed, List<ObjectFilter> filters) {
    super(database, null, null, null, filters);
    this.dim = dim;
    this.size = size;
    this.seed = seed;
  }

  private static final Logging logger = Logging.getLogger(RandomDoubleVectorDatabaseConnection.class);

  @Override
  public Database getDatabase() {
    VectorFieldTypeInformation<DoubleVector> type = VectorFieldTypeInformation.get(DoubleVector.class, dim);
    List<Object> vectors = new ArrayList<Object>(size);

    // Setup random generator
    final Random rand;
    if(seed == null) {
      rand = new Random();
    }
    else {
      rand = new Random(seed);
    }

    // Produce random vectors
    DoubleVector factory = new DoubleVector(new double[dim]);
    for(int i = 0; i < size; i++) {
      vectors.add(factory.randomInstance(rand));
    }

    // Build a bundle
    BundleMeta meta = new BundleMeta();
    meta.add(type);
    List<List<?>> columns = new ArrayList<List<?>>(1);
    columns.add(vectors);
    try {
      database.insert(new MultipleObjectsBundle(meta, columns));
    }
    catch(UnableToComplyException e) {
      logger.exception(e);
    }
    return database;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * Random generator seed.
     * <p>
     * Key: {@code -dbc.seed}
     * </p>
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("dbc.genseed", "Seed for randomly generating vectors");

    /**
     * Database to specify the random vector dimensionality
     * <p>
     * Key: {@code -dbc.dim}
     * </p>
     */
    public static final OptionID DIM_ID = OptionID.getOrCreateOptionID("dbc.dim", "Dimensionality of the vectors to generate.");

    /**
     * Parameter to specify the database size to generate.
     * <p>
     * Key: {@code -dbc.size}
     * </p>
     */
    public static final OptionID SIZE_ID = OptionID.getOrCreateOptionID("dbc.size", "Database size to generate.");

    int dim = -1;

    int size = -1;

    Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configDatabase(config);
      configFilters(config);
      configDimensionality(config);
      configSize(config);
      configSeed(config);
    }

    protected void configSeed(Parameterization config) {
      LongParameter seedParam = new LongParameter(SEED_ID, true);
      if(config.grab(seedParam)) {
        seed = seedParam.getValue();
      }
    }

    protected void configDimensionality(Parameterization config) {
      IntParameter dimParam = new IntParameter(DIM_ID);
      if(config.grab(dimParam)) {
        dim = dimParam.getValue();
      }
    }

    protected void configSize(Parameterization config) {
      IntParameter sizeParam = new IntParameter(SIZE_ID);
      if(config.grab(sizeParam)) {
        size = sizeParam.getValue();
      }
    }

    @Override
    protected RandomDoubleVectorDatabaseConnection makeInstance() {
      return new RandomDoubleVectorDatabaseConnection(database, dim, size, seed, filters);
    }
  }
}