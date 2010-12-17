package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
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
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.AbstractPreprocessorIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNChangeEvent.Type;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
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
 * @apiviz.has DistanceFunction
 * @apiviz.has KNNQuery
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends AbstractPreprocessorIndex<O, List<DistanceResultPair<D>>> implements KNNIndex<O> {
  /**
   * Logger to use.
   */
  private static final Logging logger = Logging.getLogger(MaterializeKNNPreprocessor.class);

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
   * Constructor.
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
    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, List.class);

    ArrayDBIDs ids = DBIDUtil.ensureArray(database.getIDs());
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), getLogger()) : null;

    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      storage.put(id, kNNList.get(i));
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  /**
   * Get the k nearest neighbors.
   * 
   * @param objid Object ID
   * @return Neighbors
   */
  public List<DistanceResultPair<D>> get(DBID objid) {
    if(storage == null) {
      preprocess();
    }
    return storage.get(objid);
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
    if(storage == null) {
      preprocess();
    }
    // update the materialized neighbors
    else {
      ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
      for(O o : objects) {
        ids.add(o.getID());
      }
      objectsInserted(ids);
    }
  }

  @Override
  public boolean delete(O object) {
    ArrayDBIDs ids = DBIDUtil.newArray(1);
    ids.add(object.getID());
    objectsRemoved(ids);
    return true;
  }

  @Override
  public void delete(List<O> objects) {
    ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
    for(O o : objects) {
      ids.add(o.getID());
    }
    objectsRemoved(ids);
  }

  /**
   * Called after new objects have been inserted, updates the materialized
   * neighborhood.
   * 
   * @param ids the ids of the newly inserted objects
   */
  protected void objectsInserted(ArrayDBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    // materialize the new kNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, materialize their new kNNs.", getLogger());
    }
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      storage.put(id, kNNList.get(i));
    }

    // update the affected kNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New insertions ocurred, update the affected kNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAfterInsertion(ids);

    // inform listener
    if(stepprog != null) {
      stepprog.beginStep(3, "New insertions ocurred, inform listeners.", getLogger());
    }
    fireKNNsInserted(ids, rkNN_ids);

    if(stepprog != null) {
      stepprog.ensureCompleted(getLogger());
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
      List<DistanceResultPair<D>> kNNs = storage.get(id1);
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
        storage.put(id1, newKNNs);
        rkNN_ids.add(id1);
      }
    }
    return rkNN_ids;
  }

  /**
   * Updates the kNNs of the RkNNs of the specified ids.
   * 
   * @param ids the ids of deleted objects causing a change of materialized kNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAfterDeletion(DBIDs ids) {
    TreeSetModifiableDBIDs idsSet = DBIDUtil.newTreeSet(ids);
    ArrayDBIDs rkNN_ids = DBIDUtil.newArray();
    for(DBID id1 : database.getIDs()) {
      List<DistanceResultPair<D>> kNNs = storage.get(id1);
      for(DistanceResultPair<D> kNN : kNNs) {
        if(idsSet.contains(kNN.second)) {
          rkNN_ids.add(id1);
          break;
        }
      }
    }

    // update the kNNs of the RkNNs
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
    for(int i = 0; i < rkNN_ids.size(); i++) {
      DBID id = rkNN_ids.get(i);
      storage.put(id, kNNList.get(i));
    }

    return rkNN_ids;
  }

  /**
   * Called after objects have been removed, updates the materialized
   * neighborhood.
   * 
   * @param ids the ids of the removed objects
   */
  protected void objectsRemoved(ArrayDBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    // delete the materialized (old) kNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New deletions ocurred, remove their materialized kNNs.", getLogger());
    }
    for(DBID id : ids) {
      storage.delete(id);
    }

    // update the affected kNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, update the affected kNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAfterDeletion(ids);

    // inform listener
    if(stepprog != null) {
      stepprog.beginStep(3, "New deletions ocurred, inform listeners.", getLogger());
    }
    fireKNNsRemoved(ids, rkNN_ids);

    if(stepprog != null) {
      stepprog.ensureCompleted(getLogger());
    }
  }

  /**
   * Informs all registered KNNListener that new kNNs have been inserted and as
   * a result some kNNs have been changed.
   * 
   * @param insertions the ids of the newly inserted kNNs
   * @param updates the ids of kNNs which have been changed due to the
   *        insertions
   * @see KNNListener
   */
  protected void fireKNNsInserted(DBIDs insertions, DBIDs updates) {
    KNNChangeEvent e = new KNNChangeEvent(this, Type.INSERT, insertions, updates);
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == KNNListener.class) {
        ((KNNListener) listeners[i + 1]).kNNsChanged(e);
      }
    }
  }

  /**
   * Informs all registered KNNListener that existing kNNs have been removed and
   * as a result some kNNs have been changed.
   * 
   * @removals the ids of the removed kNNs
   * @param updates the ids of kNNs which have been changed due to the removals
   * @see KNNListener
   */
  protected void fireKNNsRemoved(DBIDs removals, DBIDs updates) {
    KNNChangeEvent e = new KNNChangeEvent(this, Type.DELETE, removals, updates);
    Object[] listeners = listenerList.getListenerList();
    for(int i = listeners.length - 2; i >= 0; i -= 2) {
      if(listeners[i] == KNNListener.class) {
        ((KNNListener) listeners[i + 1]).kNNsChanged(e);
      }
    }
  }

  /**
   * Get the distance factory.
   * 
   * @return distance factory
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
   * Extracts and removes the DBIDs in the given collections.
   * 
   * @param extraxt a list of lists of DistanceResultPair to extract
   * @param remove the ids to remove
   * @return the DBIDs in the given collection
   */
  protected ArrayDBIDs extractAndRemoveIDs(List<List<DistanceResultPair<D>>> extraxt, ArrayDBIDs remove) {
    TreeSetModifiableDBIDs ids = DBIDUtil.newTreeSet();
    for(List<DistanceResultPair<D>> drps : extraxt) {
      for(DistanceResultPair<D> drp : drps) {
        ids.add(drp.second);
      }
    }
    ids.removeAll(remove);
    return DBIDUtil.ensureArray(ids);

  }

  /**
   * Adds a {@link KNNListener} which will be invoked when the kNNs of objects
   * are changing.
   * 
   * @param l the listener to add
   * @see #removeKNNListener
   * @see KNNListener
   */
  public void addKNNListener(KNNListener l) {
    listenerList.add(KNNListener.class, l);
  }

  /**
   * Removes a {@link KNNListener} previously added with
   * {@link #addDataStoreListener}.
   * 
   * @param l the listener to remove
   * @see #addKNNListener
   * @see KNNListener
   */
  public void removeKNNListener(KNNListener l) {
    listenerList.remove(KNNListener.class, l);
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

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The parameterizable factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * @apiviz.stereotype factory
   * @apiviz.uses MaterializeKNNPreprocessor oneway - - «create»
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
      instance.preprocess();
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