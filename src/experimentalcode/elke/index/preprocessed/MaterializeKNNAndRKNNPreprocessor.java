package experimentalcode.elke.index.preprocessed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent.Type;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.progress.StepProgress;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.elke.database.query.rknn.PreprocessorRKNNQuery;

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
public class MaterializeKNNAndRKNNPreprocessor<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MaterializeKNNAndRKNNPreprocessor(Parameterization config) {
    super(config);
    config = config.descend(this);
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
   * The actual preprocessor instance.
   * 
   * @author Elke Achtert
   * 
   * @param <O> The object type
   * @param <D> The distance type
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends MaterializeKNNPreprocessor.Instance<O, D> implements RKNNIndex<O>, Preprocessor.Instance<List<DistanceResultPair<D>>> {
    /**
     * Logger to use
     */
    private static final Logging logger = Logging.getLogger(MaterializeKNNAndRKNNPreprocessor.class);

    /**
     * Data storage for RkNN.
     */
    private WritableDataStore<SortedSet<DistanceResultPair<D>>> materialized_RkNN;

    /**
     * Constructor, adds this instance as database listener to the specified
     * database.
     * 
     * @param database database to preprocess
     * @param distanceFunction the distance function to use
     * @param k query k
     */
    public Instance(Database<O> database, DistanceFunction<? super O, D> distanceFunction, int k) {
      super(database, distanceFunction, k);
    }

    @Override
    protected void preprocess() {
      materialized = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT, List.class);
      materialized_RkNN = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT, Set.class);
      materializeKNNAndRKNNs(DBIDUtil.ensureArray(database.getIDs()));
    }

    /**
     * Materializes the kNNs and RkNNs of the specified object IDs.
     * 
     * @param ids the IDs of the objects
     */
    private void materializeKNNAndRKNNs(ArrayDBIDs ids) {
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Materializing k nearest neighbors and reverse k nearest neighbors (k=" + k + ")", ids.size(), logger) : null;

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
        materialized.put(id, kNNs);
        for(DistanceResultPair<D> kNN : kNNs) {
          Set<DistanceResultPair<D>> rknns = materialized_RkNN.get(kNN.second);
          rknns.add(new DistanceResultPair<D>(kNN.first, id));
        }
        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }

      if(progress != null) {
        progress.ensureCompleted(logger);
      }
    }

    @Override
    protected void objectsInserted(Collection<O> objects) {
      StepProgress stepprog = logger.isVerbose() ? new StepProgress(2) : null;

      if(stepprog != null) {
        stepprog.beginStep(1, "New insertions ocurred, update their reverse kNNs.", logger);
      }

      ArrayDBIDs ids = DBIDUtil.newArray(objects.size());
      for(O o : objects) {
        ids.add(o.getID());
      }

      // materialize the new kNNs and RkNNs
      materializeKNNAndRKNNs(ids);

      // update the old kNNs and RkNNs
      ArrayDBIDs rkNN_ids = updateKNNsAndRkNNs(ids);

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
     * 
     * @param ids
     * @return
     */
    private ArrayDBIDs updateKNNsAndRkNNs(DBIDs ids) {
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
    protected void objectsRemoved(Collection<O> objects) {
      if (true) {
        super.objectsRemoved(objects);
        return;
      }
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

      // take a linear scan to ensure that the query is "up to date" in case of
      // dynamic updates
      RKNNQuery<O, D> rkNNQuery = new LinearScanRKNNQuery<O, D>(database, distanceQuery, k);
      List<List<DistanceResultPair<D>>> rkNNs = rkNNQuery.getRKNNForBulkDBIDs(ids, k);
      System.out.println("XXX rkkns_lin(" + ids + ")" + rkNNs.get(0));
      System.out.println("XXX rkkns_pre(" + ids + ")" + materialized_RkNN.get(objects.iterator().next().getID()));
      
      
      /**
      ArrayDBIDs rkNN_ids = extractIDs(rkNNs);
      materializeKNNs(rkNN_ids);

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
      */
    }

    public List<DistanceResultPair<D>> getKNN(DBID id) {
      return materialized.get(id);
    }

    public List<DistanceResultPair<D>> getRKNN(DBID id) {
      return new ArrayList<DistanceResultPair<D>>(materialized_RkNN.get(id));
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
      return new PreprocessorRKNNQuery<O, S>(database, (Instance<O, S>) this);
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
      return new PreprocessorRKNNQuery<O, S>(database, (Instance<O, S>) this);
    }

    @Override
    public String getLongName() {
      return "kNN and RkNN Preprocessor";
    }

    @Override
    public String getShortName() {
      return "knn and rknn preprocessor";
    }

  }
}