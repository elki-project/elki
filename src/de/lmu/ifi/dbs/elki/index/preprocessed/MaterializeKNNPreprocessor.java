package de.lmu.ifi.dbs.elki.index.preprocessed;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent.Type;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.AbstractIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> implements IndexFactory<O, KNNIndex<O>> {
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
  public Instance<O, D> instantiate(Database<O> database) {
    Instance<O, D> instance = new Instance<O, D>(database, distanceFunction, k);
    if(database.size() > 0) {
      instance.preprocess();
    }
    return instance;
  }

  /**
   * Get the distance function.
   * 
   * @return Distance function
   */
  // TODO: hide this?
  public DistanceFunction<? super O, D> getDistanceFunction() {
    return distanceFunction;
  }

  /**
   * Get the distance factory.
   * 
   * @return Distance factory
   */
  public D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndex<O> implements KNNIndex<O>, Preprocessor.Instance<List<DistanceResultPair<D>>>, DataStoreListener<O> {
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
     * The distance query we used.
     */
    // TODO: remove?
    protected final DistanceQuery<O, D> distanceQuery;

    /**
     * KNNQuery instance to use.
     */
    private final KNNQuery<O, D> knnQuery;

    /**
     * RkNNQuery instance to use.
     */
    private final RKNNQuery<O, D> rkNNQuery;

    /**
     * Holds the listener.
     */
    private EventListenerList listenerList = new EventListenerList();

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
      this.knnQuery = database.getKNNQuery(distanceFunction, k, DatabaseQuery.HINT_BULK, DatabaseQuery.HINT_EXACT, DatabaseQuery.HINT_HEAVY_USE);
      this.rkNNQuery = database.getRKNNQuery(distanceFunction, DatabaseQuery.HINT_BULK, DatabaseQuery.HINT_EXACT, DatabaseQuery.HINT_HEAVY_USE);
      this.distanceQuery = database.getDistanceQuery(distanceFunction);
    }

    /**
     * The actual preprocessing step. Adds this instance as listener to the
     * database after materializing the KNNs.
     */
    protected void preprocess() {
      materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
      materializeKNNs(DBIDUtil.ensureArray(database.getIDs()));
      database.addDataStoreListener(this);
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      return materialized.get(id);
    }

    @Override
    public void insert(@SuppressWarnings("unused") List<O> objects) {
      // Materialize the first run
      if(materialized == null) {
        preprocess();
      }
      else {
        throw new UnsupportedOperationException("The kNN preprocessor index currently does not allow dynamic updates!");
      }
    }

    /**
     * Materializes the kNNs of the specified object IDs.
     * 
     * @param ids the IDs of the objects
     */
    private void materializeKNNs(ArrayDBIDs ids) {

      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), logger) : null;

      // try a bulk knn query
      try {
        List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
        for(int i = 0; i < ids.size(); i++) {
          DBID id = ids.get(i);
          materialized.put(id, kNNList.get(i));
          if(progress != null) {
            progress.incrementProcessed(logger);
          }
        }
      }
      // bulk not supported -> perform a sequential one
      catch(UnsupportedOperationException e) {
        for(DBID id : ids) {
          List<DistanceResultPair<D>> kNN = knnQuery.getKNNForDBID(id, k);
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

    private void objectsInserted(Collection<O> objects) {
      StepProgress stepprog = logger.isVerbose() ? new StepProgress(2) : null;

      if(stepprog != null) {
        stepprog.beginStep(1, "New insertions ocurred, get their reverse kNNs and update them.", logger);
      }
      // get reverse k nearest neighbors of each new object
      // (includes also the new objcets)
      // and update their k nearest neighbors
      ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
      for(O o : objects) {
        ids.add(o.getID());
      }
      List<List<DistanceResultPair<D>>> rkNNs = rkNNQuery.getRKNNForBulkDBIDs(ids, k);
      ArrayDBIDs rkNN_ids = extractIDs(rkNNs);
      materializeKNNs(rkNN_ids);

      if(stepprog != null) {
        stepprog.beginStep(2, "New insertions ocurred, inform listeners.", logger);
      }

      Map<Type, Collection<DBID>> changed = new HashMap<Type, Collection<DBID>>();
      changed.put(Type.INSERT, ids);
      rkNN_ids.removeAll(ids);
      changed.put(Type.UPDATE, rkNN_ids);
      DataStoreEvent<DBID> e = new DataStoreEvent<DBID>(this, changed, DataStoreEvent.Type.INSERT_AND_UPDATE);
      fireDataStoreEvent(e);

      if(stepprog != null) {
        stepprog.ensureCompleted(logger);
      }
    }

    private void objectsRemoved(Collection<O> objects) {
      StepProgress stepprog = logger.isVerbose() ? new StepProgress(2) : null;

      if(stepprog != null) {
        stepprog.beginStep(1, "New deletions ocurred, get their reverse kNNs and update them.", logger);
      }
      // get reverse k nearest neighbors of each removed object
      // (includes also the removed objects)
      // and update their k nearest neighbors
      ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
      for(O o : objects) {
        ids.add(o.getID());
      }
      List<List<DistanceResultPair<D>>> rkNNs = rkNNQuery.getRKNNForBulkDBIDs(ids, k);
      ArrayDBIDs rkNN_ids = extractIDs(rkNNs);
      materializeKNNs(rkNN_ids);

      if(stepprog != null) {
        stepprog.beginStep(2, "New deletions ocurred, inform listeners.", logger);
      }

      Map<Type, Collection<DBID>> changed = new HashMap<Type, Collection<DBID>>();
      changed.put(Type.DELETE, ids);
      rkNN_ids.removeAll(ids);
      changed.put(Type.UPDATE, rkNN_ids);
      DataStoreEvent<DBID> e = new DataStoreEvent<DBID>(this, changed, DataStoreEvent.Type.DELETE_AND_UPDATE);
      fireDataStoreEvent(e);

      if(stepprog != null) {
        stepprog.ensureCompleted(logger);
      }
    }

    /**
     * Informs all registered DataStoreListener about the specified
     * DataStoreEvent.
     * 
     * @see DataStoreListener
     * @see DataStoreEvent
     */
    @SuppressWarnings("unchecked")
    private void fireDataStoreEvent(DataStoreEvent<DBID> e) {
      // inform listeners
      Object[] listeners = listenerList.getListenerList();
      for(int i = listeners.length - 2; i >= 0; i -= 2) {
        if(listeners[i] == DataStoreListener.class) {
          ((DataStoreListener<DBID>) listeners[i + 1]).contentChanged(e);
        }
      }
    }

    @Override
    public void contentChanged(DataStoreEvent<O> e) {
      if(e.getType().equals(Type.INSERT)) {
        objectsInserted(e.getObjects().get(Type.INSERT));
      }
      else if(e.getType().equals(Type.DELETE)) {
        objectsRemoved(e.getObjects().get(Type.DELETE));
      }

      else if(e.getType().equals(Type.UPDATE)) {
        // TODO
        throw new UnsupportedOperationException("Event type not supported: " + e.getType());
      }
      else {
        throw new UnsupportedOperationException("Event type not supported: " + e.getType());
      }
    }

    /**
     * Get the distance factory.
     * 
     * @return Distance factory
     */
    public D getDistanceFactory() {
      return distanceFunction.getDistanceFactory();
    }

    /**
     * The distance query we used.
     * 
     * @return Distance query
     */
    // TODO: remove?
    public DistanceQuery<O, D> getDistanceQuery() {
      return distanceQuery;
    }

    /**
     * Get the value of 'k' supported by this preprocessor.
     * 
     * @return k
     */
    public int getK() {
      return k;
    }

    /**
     * Extracts the DBIDs in the given collection.
     * 
     * @param drpsList a list of lists of DistanceResultPair
     * @return the DBIDs in the given collection
     */
    private ArrayDBIDs extractIDs(List<List<DistanceResultPair<D>>> drpsList) {
      TreeSetModifiableDBIDs ids = DBIDUtil.newTreeSet();
      for(List<DistanceResultPair<D>> drps : drpsList) {
        for(DistanceResultPair<D> drp : drps) {
          ids.add(drp.second);
        }
      }
      return DBIDUtil.ensureArray(ids);

    }

    /**
     * Adds a <code>DataStoreListener</code> for a <code>DataStoreEvent</code>
     * posted after the content of this datastore changes.
     * 
     * @param l the listener to add
     * @see #removeListener(DataStoreListener)
     * @see DataStoreListener
     * @see DataStoreEvent
     */
    public void addDataStoreListener(DataStoreListener<DBID> l) {
      listenerList.add(DataStoreListener.class, l);
    }

    /**
     * Removes a <code>DataStoreListener</code> previously added with
     * {@link #addListener(DataStoreListener)}.
     * 
     * @param l the listener to remove
     * @see #addListener(DataStoreListener)
     * @see DataStoreListener
     * @see DataStoreEvent
     */
    public void removeDataStoreListener(DataStoreListener<DBID> l) {
      listenerList.remove(DataStoreListener.class, l);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(Database<O> database, DistanceFunction<? super O, S> distanceFunction, Object... hints) {
      if(!this.distanceFunction.equals(distanceFunction)) {
        return null;
      }
      // k max supported?
      for(Object hint : hints) {
        if(hint instanceof Integer) {
          if(((Integer) hint) > k) {
            return null;
          }
        }
      }
      return new PreprocessorKNNQuery<O, S>(database, (Instance<O, S>) this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(Database<O> database, DistanceQuery<O, S> distanceQuery, Object... hints) {
      if(!this.distanceFunction.equals(distanceQuery.getDistanceFunction())) {
        return null;
      }
      // k max supported?
      for(Object hint : hints) {
        if(hint instanceof Integer) {
          if(((Integer) hint) > k) {
            return null;
          }
        }
      }
      return new PreprocessorKNNQuery<O, S>(database, (Instance<O, S>) this);
    }

    @Override
    public String getLongName() {
      return "kNN Preprocessor";
    }

    @Override
    public String getShortName() {
      return "kNN preprocessor";
    }
  }
}