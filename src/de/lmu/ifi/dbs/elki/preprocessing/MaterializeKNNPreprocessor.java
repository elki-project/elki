package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> implements Preprocessor<O, List<DistanceResultPair<D>>>, Parameterizable {
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * materialized. must be an integer greater than 1.
   * <p>
   * Key: {@code -materialize.k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(1));

  /**
   * Holds the value of {@link #K_PARAM}.
   */
  protected int k;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Parameter to indicate the distance function to be used to ascertain the
   * nearest neighbors.
   * <p/>
   * <p>
   * Default value: {@link EuclideanDistanceFunction}
   * </p>
   * <p>
   * Key: {@code materialize.distance}
   * </p>
   */
  public final ObjectParameter<DistanceFunction<? super O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<? super O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Hold the distance function to be used.
   */
  protected DistanceFunction<? super O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MaterializeKNNPreprocessor(Parameterization config) {
    super();
    config = config.descend(this);
    // number of neighbors
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }

    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    Instance<T, D> instance = new Instance<T, D>(database, distanceFunction, k);
    instance.preprocess();
    return instance;
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> implements Preprocessor.Instance<List<DistanceResultPair<D>>>, DataStoreListener<O> {
    /**
     * Logger to use
     */
    private static final Logging logger = Logging.getLogger(MaterializeKNNPreprocessor.class);

    /**
     * Data storage
     */
    protected WritableDataStore<List<DistanceResultPair<D>>> materialized;

    /**
     * The query k value.
     */
    final protected int k;

    /**
     * The distance function to be used.
     */
    final protected DistanceFunction<? super O, D> distanceFunction;

    /**
     * The database to preprocess.
     */
    final protected Database<O> database;

    /**
     * Constructor, adds this instance as database listener to the specified
     * database.
     * 
     * @param database database to preprocess
     * @param distanceFunction the distance function to use
     * @param k query k
     */
    public Instance(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
      this.database = database;
      this.distanceFunction = distanceFunction;
      this.k = k;

      // todo woanders hin
      database.addDataStoreListener(this);
    }

    /**
     * The actual preprocessing step.
     * 
     * @param database Database to preprocess.
     * @param distanceFunction The distance function to use.
     */
    protected void preprocess() {
      materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
      materializeKNNs(DBIDUtil.ensureArray(database.getIDs()));
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      return materialized.get(id);
    }

    /**
     * Materializes the kNNs of the specified object ids.
     * 
     * @param ids the ids of the objects
     */
    private void materializeKNNs(ArrayDBIDs ids) {
      // todo datastore braucht sowas wie einfuegen, loeschen und updaten auf einmal
      DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distanceFunction);
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), logger) : null;

      // try a bulk knn query
      try {
        List<List<DistanceResultPair<D>>> kNNList = database.bulkKNNQueryForID(ids, k, distanceQuery);
        for(int i = 0; i < ids.size(); i++) {
          materialized.put(ids.get(i), kNNList.get(i));
          if(progress != null) {
            progress.incrementProcessed(logger);
          }
        }
      }
      // bulk not supported -> perform a sequential one
      catch(UnsupportedOperationException e) {
        for(DBID id : ids) {
          List<DistanceResultPair<D>> kNN = database.kNNQueryForID(id, k, distanceQuery);
          materialized.put(id, kNN);
          if(progress != null) {
            progress.incrementProcessed(logger);
          }
        }
      }

      if(progress != null) {
        progress.ensureCompleted(logger);
      }
    }

    //private void insert(ArrayDBIDs ids) {
      /*
      StepProgress stepprog = logger.isVerbose() ? new StepProgress(3) : null;
      
      // compute k nearest neighbors of each new object
      if(stepprog != null) {
        stepprog.beginStep(1, "New Insertions ocurred, compute their kNNs.", logger);
      }
      materializeKNNs(ids);

      if(stepprog != null) {
        stepprog.beginStep(2, "New Insertions ocurred, get their reverse kNNs and update them.", logger);
      }

      // get reverse k nearest neighbors of each new object
      // and update their k nearest neighbors
      DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(distanceFunction);
      List<List<DistanceResultPair<D>>> rkNNs = database.bulkReverseKNNQueryForID(ids, k, distanceQuery);
      ArrayModifiableDBIDs rkNN_ids = DBIDUtil.mergeIDs(rkNNs, DBIDUtil.EMPTYDBIDS);

      updateKNNs(database, ids);
      
      if(stepprog != null) {
        stepprog.beginStep(3, "New Insertions ocurred, inform listeners.", logger);
      }
      // todo

      if(stepprog != null) {
        stepprog.ensureCompleted(logger);
      }
      */
    //}

    /**
     * Updates the kNNs of the
     * 
     * @param database
     * @param ids
     */
    //private void updateKNNs(Database<O> database, ArrayDBIDs ids) {
      /*
       * // get k nearest neighbors of each rknn
       * List<List<DistanceResultPair<D>>> kNNs =
       * database.bulkKNNQueryForID(rkNN_ids, k, distanceQuery);
       * 
       * StringBuffer msg = new StringBuffer(); Map<DBID,
       * List<DistanceResultPair<D>>> newKNNs = new HashMap<DBID,
       * List<DistanceResultPair<D>>>(); for(int i = 0; i < rkNN_ids.size();
       * i++) { DBID id = rkNN_ids.get(i); newKNNs.put(id, kNNs.get(i));
       * 
       * if(logger.isDebugging()) { msg.append("\n");
       * List<DistanceResultPair<D>> old = materialized.get(id); if(old != null)
       * { msg.append("\nknn_old[" + id + "]" + old); } msg.append("\nknn_new["
       * + id + "]" + kNNs.get(i)); } } materialized.putAll(newKNNs);
       * 
       * if(logger.isDebugging()) { logger.debug(msg.toString()); }
       */
    //}

    @Override
    public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<O> e) {
      // todo
      //DBIDs insertions = e.getInsertionsIDs();
      //insert(DBIDUtil.ensureArray(insertions));
      
      //DBIDs updates = e.getUpdateIDs();
      //DBIDs deletions = e.getDeletionsIDs();
    }
  }
}