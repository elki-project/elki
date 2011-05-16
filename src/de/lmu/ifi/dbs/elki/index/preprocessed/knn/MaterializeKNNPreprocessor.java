package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.KNNChangeEvent.Type;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * A preprocessor for annotation of the k nearest neighbors (and their
 * distances) to each database object.
 * 
 * Used for example by {@link de.lmu.ifi.dbs.elki.algorithm.outlier.LOF}.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DistanceFunction
 * @apiviz.has KNNQuery
 * @apiviz.has KNNListener
 * 
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors of objects of a database.")
public class MaterializeKNNPreprocessor<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor<O, D> {
  /**
   * Logger to use.
   */
  private static final Logging logger = Logging.getLogger(MaterializeKNNPreprocessor.class);

  /**
   * KNNQuery instance to use.
   */
  protected final KNNQuery<O, D> knnQuery;

  /**
   * Constructor with preprocessing step.
   * 
   * @param relation Relation to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k) {
    this(relation, distanceFunction, k, true);
  }

  /**
   * Constructor.
   * 
   * @param relation Relation to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  protected MaterializeKNNPreprocessor(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, int k, boolean preprocess) {
    super(relation, distanceFunction, k);
    this.knnQuery = relation.getDatabase().getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_BULK, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_NO_CACHE);

    if(preprocess) {
      preprocess();
    }
  }

  /**
   * The actual preprocessing step.
   */
  @Override
  protected void preprocess() {
    storage = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, List.class);

    logger.warning("Using knnQuery: " + knnQuery);

    ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors (k=" + k + ")", ids.size(), getLogger()) : null;

    // Try bulk
    List<List<DistanceResultPair<D>>> kNNList = null; // knnQuery.getKNNForBulkDBIDs(ids, k);
    if (kNNList != null) {
      for(int i = 0; i < ids.size(); i++) {
        DBID id = ids.get(i);
        storage.put(id, kNNList.get(i));
        if(progress != null) {
          progress.incrementProcessed(getLogger());
        }
      }
    }
    else {
      for(DBID id : ids) {
        List<DistanceResultPair<D>> knn = knnQuery.getKNNForDBID(id, k);
        storage.put(id, knn);
        if(progress != null) {
          progress.incrementProcessed(getLogger());
        }
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
    return storage.get(objid);
  }

  @Override
  public final void insert(DBID id) {
    objectsInserted(id);
  }

  @Override
  public void insertAll(DBIDs ids) {
    objectsInserted(ids);
  }

  @Override
  public boolean delete(DBID id) {
    objectsRemoved(id);
    return true;
  }

  @Override
  public void deleteAll(DBIDs ids) {
    objectsRemoved(ids);
  }

  /**
   * Called after new objects have been inserted, updates the materialized
   * neighborhood.
   * 
   * @param ids the ids of the newly inserted objects
   */
  protected void objectsInserted(DBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    ArrayDBIDs aids = DBIDUtil.ensureArray(ids);
    // materialize the new kNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, materialize their new kNNs.", getLogger());
    }
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(aids, k);
    for(int i = 0; i < aids.size(); i++) {
      DBID id = aids.get(i);
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
    DBIDs oldids = DBIDUtil.difference(relation.getDBIDs(), ids);
    for(DBID id1 : oldids) {
      List<DistanceResultPair<D>> kNNs = storage.get(id1);
      D knnDist = kNNs.get(kNNs.size() - 1).getDistance();
      // look for new kNNs
      List<DistanceResultPair<D>> newKNNs = new ArrayList<DistanceResultPair<D>>();
      KNNHeap<D> heap = null;
      for(DBID id2 : ids) {
        D dist = distanceQuery.distance(id1, id2);
        if(dist.compareTo(knnDist) <= 0) {
          if(heap == null) {
            heap = new KNNHeap<D>(k);
            heap.addAll(kNNs);
          }
          heap.add(dist, id2);
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
    for(DBID id1 : relation.getDBIDs()) {
      List<DistanceResultPair<D>> kNNs = storage.get(id1);
      for(DistanceResultPair<D> kNN : kNNs) {
        if(idsSet.contains(kNN.getDBID())) {
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
  protected void objectsRemoved(DBIDs ids) {
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
   * @param removals the ids of the removed kNNs
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
        ids.add(drp.getDBID());
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
   * Removes a {@link KNNListener} previously added with {@link #addKNNListener}
   * .
   * 
   * @param l the listener to remove
   * @see #addKNNListener
   * @see KNNListener
   */
  public void removeKNNListener(KNNListener l) {
    listenerList.remove(KNNListener.class, l);
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
  public static class Factory<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory<O, D> {
    /**
     * Index factory.
     * 
     * @param k k parameter
     * @param distanceFunction distance function
     */
    public Factory(int k, DistanceFunction<? super O, D> distanceFunction) {
      super(k, distanceFunction);
    }

    @Override
    public MaterializeKNNPreprocessor<O, D> instantiate(Relation<O> relation) {
      MaterializeKNNPreprocessor<O, D> instance = new MaterializeKNNPreprocessor<O, D>(relation, distanceFunction, k);
      return instance;
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<O, D extends Distance<D>> extends AbstractMaterializeKNNPreprocessor.Factory.Parameterizer<O, D> {
      @Override
      protected Factory<O, D> makeInstance() {
        return new Factory<O, D>(k, distanceFunction);
      }
    }
  }
}