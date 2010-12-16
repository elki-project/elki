package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.PreprocessorRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A preprocessor for annotation of the k nearest neighbors and the reverse k
 * nearest neighbors (and their distances) to each database object.
 * 
 * @author Elke Achtert
 * @param <O> the type of database objects the preprocessor can be applied to
 * @param <D> the type of distance the used distance function will return
 */
@Title("Materialize kNN and RkNN Neighborhood preprocessor")
@Description("Materializes the k nearest neighbors and the reverse k nearest neighbors of objects of a database.")
public class MaterializeKNNAndRKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor<O, D> implements RKNNIndex<O>, Preprocessor.Instance<List<DistanceResultPair<D>>> {
  /**
   * Logger to use.
   */
  private static final Logging logger = Logging.getLogger(MaterializeKNNAndRKNNPreprocessor.class);

  /**
   * Additional data storage for RkNN.
   */
  private WritableDataStore<SortedSet<DistanceResultPair<D>>> materialized_RkNN;

  /**
   * Constructor.
   * 
   * @param database database to preprocess
   * @param distanceFunction the distance function to use
   * @param k query k
   */
  public MaterializeKNNAndRKNNPreprocessor(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
    super(database, distanceFunction, k);
  }

  @Override
  protected void preprocess() {
    storage = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT, List.class);
    materialized_RkNN = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT, Set.class);
    FiniteProgress progress = getLogger().isVerbose() ? new FiniteProgress("Materializing k nearest neighbors and reverse k nearest neighbors (k=" + k + ")", database.size(), getLogger()) : null;
    materializeKNNAndRKNNs(DBIDUtil.ensureArray(database.getIDs()), progress);
  }

  /**
   * Materializes the kNNs and RkNNs of the specified object IDs.
   * 
   * @param ids the IDs of the objects
   */
  private void materializeKNNAndRKNNs(ArrayDBIDs ids, FiniteProgress progress) {

    // add an empty list to each rknn
    for(DBID id : ids) {
      if(materialized_RkNN.get(id) == null) {
        materialized_RkNN.put(id, new TreeSet<DistanceResultPair<D>>());
      }
    }

    // knn query
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
    for(int i = 0; i < ids.size(); i++) {
      DBID id = ids.get(i);
      List<DistanceResultPair<D>> kNNs = kNNList.get(i);
      storage.put(id, kNNs);
      for(DistanceResultPair<D> kNN : kNNs) {
        Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(kNN.second);
        rknns.add(new DistanceResultPair<D>(kNN.first, id));
      }
      if(progress != null) {
        progress.incrementProcessed(getLogger());
      }
    }

    if(progress != null) {
      progress.ensureCompleted(getLogger());
    }
  }

  @Override
  protected void objectsInserted(ArrayDBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    // materialize the new kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New insertions ocurred, materialize their new kNNs and RkNNs.", getLogger());
    }
    materializeKNNAndRKNNs(ids, null);

    // update the old kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New insertions ocurred, update the affected kNNs and RkNNs.", getLogger());
    }
    ArrayDBIDs rkNN_ids = updateKNNsAndRkNNs(ids);

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
   * Updates the kNNs and RkNNs after insertion of the specified ids.
   * 
   * @param ids the ids of newly inserted objects causing a change of
   *        materialized kNNs and RkNNs
   * @return the RkNNs of the specified ids, i.e. the kNNs which have been
   *         updated
   */
  private ArrayDBIDs updateKNNsAndRkNNs(DBIDs ids) {
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

        // get the difference
        int i = 0;
        int j = 0;
        List<DistanceResultPair<D>> added = new ArrayList<DistanceResultPair<D>>();
        List<DistanceResultPair<D>> removed = new ArrayList<DistanceResultPair<D>>();
        while(i < kNNs.size() && j < newKNNs.size()) {
          DistanceResultPair<D> drp1 = kNNs.get(i);
          DistanceResultPair<D> drp2 = newKNNs.get(j);
          if(!drp1.equals(drp2)) {
            added.add(drp2);
            j++;
          }
          else {
            i++;
            j++;
          }
        }
        if(i != j) {
          for(; i < kNNs.size(); i++)
            removed.add(kNNs.get(i));
        }
        // add new RkNN
        for(DistanceResultPair<D> drp : added) {
          Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(drp.second);
          rknns.add(new DistanceResultPair<D>(drp.first, id1));
        }
        // remove old RkNN
        for(DistanceResultPair<D> drp : removed) {
          Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(drp.second);
          rknns.remove(new DistanceResultPair<D>(drp.first, id1));
        }

        rkNN_ids.add(id1);
      }
    }
    return rkNN_ids;
  }

  @Override
  protected void objectsRemoved(ArrayDBIDs ids) {
    StepProgress stepprog = getLogger().isVerbose() ? new StepProgress(3) : null;

    // delete the materialized (old) kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(1, "New deletions ocurred, remove their materialized kNNs and RkNNs.", getLogger());
    }
    List<List<DistanceResultPair<D>>> kNNs = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    List<List<DistanceResultPair<D>>> rkNNs = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(DBID id : ids) {
      kNNs.add(storage.get(id));
      storage.delete(id);
      rkNNs.add(new ArrayList<DistanceResultPair<D>>(materialized_RkNN.get(id)));
      materialized_RkNN.delete(id);
    }
    ArrayDBIDs kNN_ids = extractAndRemoveIDs(kNNs, ids);
    ArrayDBIDs rkNN_ids = extractAndRemoveIDs(rkNNs, ids);

    // update the affected kNNs and RkNNs
    if(stepprog != null) {
      stepprog.beginStep(2, "New deletions ocurred, update the affected kNNs and RkNNs.", getLogger());
    }
    // update the kNNs of the RkNNs
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(rkNN_ids, k);
    for(int i = 0; i < rkNN_ids.size(); i++) {
      DBID id = rkNN_ids.get(i);
      storage.put(id, kNNList.get(i));
      for(DistanceResultPair<D> kNN : kNNList.get(i)) {
        Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(kNN.second);
        rknns.add(new DistanceResultPair<D>(kNN.first, id));
      }
    }
    // update the RkNNs of the kNNs
    TreeSetDBIDs idsSet = DBIDUtil.newTreeSet(ids);
    for(int i = 0; i < kNN_ids.size(); i++) {
      DBID id = kNN_ids.get(i);
      SortedSet<DistanceResultPair<D>> rkNN = materialized_RkNN.get(id);
      for(Iterator<DistanceResultPair<D>> it = rkNN.iterator(); it.hasNext();) {
        DistanceResultPair<D> drp = it.next();
        if(idsSet.contains(drp.second)) {
          it.remove();
        }
      }
    }

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
   * Returns the materialized kNNs of the specified id.
   * 
   * @param id the query id
   * @return the kNNs
   */
  public List<DistanceResultPair<D>> getKNN(DBID id) {
    return storage.get(id);
  }

  /**
   * Returns the materialized RkNNs of the specified id.
   * 
   * @param id the query id
   * @return the RkNNs
   * @param id
   * @return
   */
  public List<DistanceResultPair<D>> getRKNN(DBID id) {
    SortedSet<DistanceResultPair<D>> rKNN = materialized_RkNN.get(id);
    if(rKNN == null)
      return null;
    return new ArrayList<DistanceResultPair<D>>(rKNN);
  }

  @SuppressWarnings({ "unchecked" })
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(Database<O> database, DistanceFunction<? super O, S> distanceFunction, Object... hints) {
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
    return new PreprocessorRKNNQuery<O, S>(database, (MaterializeKNNAndRKNNPreprocessor<O, S>) this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(Database<O> database, DistanceQuery<O, S> distanceQuery, Object... hints) {
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
    return new PreprocessorRKNNQuery<O, S>(database, (MaterializeKNNAndRKNNPreprocessor<O, S>) this);
  }

  @Override
  public String getLongName() {
    return "kNN and RkNN Preprocessor";
  }

  @Override
  public String getShortName() {
    return "knn and rknn preprocessor";
  }
  
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The parameterizable factory.
   * 
   * @author Elke Achtert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Factory<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor.Factory<O, D> {

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     * 
     * @param config Parameterization
     */
    public Factory(Parameterization config) {
      super(config);
      config = config.descend(this);
    }

    @Override
    public MaterializeKNNAndRKNNPreprocessor<O, D> instantiate(Database<O> database) {
      MaterializeKNNAndRKNNPreprocessor<O, D> instance = new MaterializeKNNAndRKNNPreprocessor<O, D>(database, distanceFunction, k);
      if(database.size() > 0) {
        instance.preprocess();
      }
      return instance;
    }

  }
}