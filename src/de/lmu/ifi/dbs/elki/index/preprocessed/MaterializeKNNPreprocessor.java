package de.lmu.ifi.dbs.elki.index.preprocessed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent.Type;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
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
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
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
 * 
 * @apiviz.landmark
 * @apiviz.has WritableDataStore
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractIndex<O> implements KNNIndex<O>, Preprocessor.Instance<List<DistanceResultPair<D>>> {
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
  protected final KNNQuery<O, D> knnQuery;

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
  public MaterializeKNNPreprocessor(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
    this.database = database;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = database.getDistanceQuery(distanceFunction);
    this.k = k;
    // take a linear scan to ensure that the query is "up to date" in case of
    // dynamic updates
    this.knnQuery = new LinearScanKNNQuery<O, D>(database, distanceQuery);
    // database.getKNNQuery(distanceFunction, k, DatabaseQuery.HINT_BULK,
    // DatabaseQuery.HINT_EXACT, DatabaseQuery.HINT_HEAVY_USE);

  }

  /**
   * The actual preprocessing step.
   */
  protected void preprocess() {
    materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);
    materializeKNNs(DBIDUtil.ensureArray(database.getIDs()));
  }

  @Override
  public List<DistanceResultPair<D>> get(DBID id) {
    return materialized.get(id);
  }

  @Override
  public final void insert(O object) {
    List<O> objects = new ArrayList<O>(1);
    objects.add(object);
    insert(objects);
  }

  @Override
  public void insert(List<O> objects) {
    // materialize the first run
    if(materialized == null) {
      preprocess();
    }
    // update the materialized neighbors
    else {
      objectsInserted(objects);
    }
  }

  @Override
  public boolean delete(O object) {
    List<O> objects = new ArrayList<O>(1);
    objects.add(object);
    objectsRemoved(objects);
    return true;
  }

  @Override
  public void delete(List<O> objects) {
    objectsRemoved(objects);
  }

  /**
   * Materializes the kNNs of the specified object IDs.
   * 
   * @param ids the IDs of the objects
   */
  private void materializeKNNs(ArrayDBIDs ids) {
    FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), logger) : null;

    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      materialized.put(id, kNNList.get(i));
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }

    if(progress != null) {
      progress.ensureCompleted(logger);
    }
  }

  /**
   * Called after new objects have been inserted, updates the materialized
   * neighborhood.
   * 
   * @param objects the new objects
   */
  protected void objectsInserted(Collection<O> objects) {
    StepProgress stepprog = logger.isVerbose() ? new StepProgress(2) : null;

    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, update their reverse kNNs.", logger);
    }

    ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
    for(O o : objects) {
      ids.add(o.getID());
    }

    // materialize the new knns
    materializeKNNs(ids);

    // update the old knns
    ArrayDBIDs rkNN_ids = updateKNNsAfterInsertion(ids);

    if(stepprog != null) {
      stepprog.beginStep(2, "New insertions ocurred, inform listeners.", logger);
    }

    Map<Type, Collection<DBID>> changed = new HashMap<Type, Collection<DBID>>();
    changed.put(Type.INSERT, ids);
    changed.put(Type.UPDATE, rkNN_ids);
    DataStoreEvent<DBID> e = new DataStoreEvent<DBID>(this, changed);
    fireDataStoreEvent(e);

    if(stepprog != null) {
      stepprog.ensureCompleted(logger);
    }
  }

  /**
   * Updates the kNNs of the RkNNs of the specified ids.
   * 
   * @param ids the ids of newly inserted objects causing a change of
   *        materialized kNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAfterInsertion(DBIDs ids) {
    ArrayDBIDs rkNN_ids = DBIDUtil.newArray();
    DBIDs oldids = DBIDUtil.difference(database.getIDs(), ids);
    for(DBID id1 : oldids) {
      List<DistanceResultPair<D>> kNNs = materialized.get(id1);
      D knnDist = kNNs.get(kNNs.size() - 1).getDistance();
      // look for new kNNs
      List<DistanceResultPair<D>> newKNNs = new ArrayList<DistanceResultPair<D>>();
      KNNHeap<D> heap = null;
      for(DBID id2 : ids) {
        D dist = database.getDistanceQuery(distanceFunction).distance(id1, id2);
        if(dist.compareTo(knnDist) <= 0) {
          if(heap == null) {
            heap = new KNNHeap<D>(k);
            heap.addAll(kNNs);
          }
          heap.add(new DistanceResultPair<D>(dist, id2));
        }
      }
      if(heap != null) {
        newKNNs = heap.toSortedArrayList();
        materialized.put(id1, newKNNs);
        rkNN_ids.add(id1);
      }
    }
    return rkNN_ids;
  }

  private void updateKNNsAfterDeletion(ArrayDBIDs ids) {
    // todo alte knns rausnehmen!
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k + 1);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      materialized.put(id, kNNList.get(i));
    }
  }

  /**
   * Called after objects have been removed, updates the materialized
   * neighborhood.
   * 
   * @param objects the removed objects
   */
  protected void objectsRemoved(Collection<O> objects) {
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

    // delete the materialized kNNs
    List<List<DistanceResultPair<D>>> rkNNs = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(DBID id : ids) {
      List<DistanceResultPair<D>> knns = materialized.get(id);
      materialized.delete(id);
    }
    ArrayDBIDs rkNN_ids = extractIDs(rkNNs);

    // update the kNNs of the RkNNs
    updateKNNsAfterDeletion(rkNN_ids);

    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, inform listeners.", logger);
    }

    Map<Type, Collection<DBID>> changed = new HashMap<Type, Collection<DBID>>();
    changed.put(Type.DELETE, ids);
    rkNN_ids.removeAll(ids);
    changed.put(Type.UPDATE, rkNN_ids);
    DataStoreEvent<DBID> e = new DataStoreEvent<DBID>(this, changed);
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
  protected void fireDataStoreEvent(DataStoreEvent<DBID> e) {
    // inform listeners
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == DataStoreListener.class) {
        ((DataStoreListener<DBID>) listeners[i + 1]).contentChanged(e);
      }
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
   * @see #removeDataStoreListener
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  public void addDataStoreListener(DataStoreListener<DBID> l) {
    listenerList.add(DataStoreListener.class, l);
  }

  /**
   * Removes a <code>DataStoreListener</code> previously added with
   * {@link #addDataStoreListener}.
   * 
   * @param l the listener to remove
   * @see #addDataStoreListener
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
    return new PreprocessorKNNQuery<O, S>(database, (MaterializeKNNPreprocessor<O, S>) this);
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
    return new PreprocessorKNNQuery<O, S>(database, (MaterializeKNNPreprocessor<O, S>) this);
  }

  @Override
  public String getLongName() {
    return "kNN Preprocessor";
  }

  @Override
  public String getShortName() {
    return "knn preprocessor";
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses Instance oneway - - «create»
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O extends DatabaseObject, D extends Distance<D>> implements IndexFactory<O, KNNIndex<O>> {
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
    public Factory(Parameterization config) {
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
    public MaterializeKNNPreprocessor<O, D> instantiate(Database<O> database) {
      MaterializeKNNPreprocessor<O, D> instance = new MaterializeKNNPreprocessor<O, D>(database, distanceFunction, k);
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

  }
}