package experimentalcode.elke.index.preprocessed;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent.Type;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
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
    private WritableDataStore<List<DistanceResultPair<D>>> materialized_RkNN;

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
      materialized_RkNN = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT, List.class);
      materializeKNNAndRKNNs(DBIDUtil.ensureArray(database.getIDs()));
      database.addDataStoreListener(this);
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
        materialized_RkNN.put(id, new ArrayList<DistanceResultPair<D>>());
      }

      // try a bulk knn query
      try {
        List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(ids, k);
        for(int i = 0; i < ids.size(); i++) {
          DBID id = ids.get(i);
          List<DistanceResultPair<D>> kNNs = kNNList.get(i);
          putkNNsAndRkNNs(id, kNNs, progress);
        }
      }
      // bulk not supported -> perform sequential queries
      catch(UnsupportedOperationException e) {
        for(DBID id : ids) {
          List<DistanceResultPair<D>> kNNs = knnQuery.getKNNForDBID(id, k);
          putkNNsAndRkNNs(id, kNNs, progress);
        }
      }

      if(progress != null) {
        progress.ensureCompleted(logger);
      }
    }

    /**
     * Puts the kNNs and RkNNs to the data storage, helper method for
     * {@link #materializeKNNAndRKNNs(ArrayDBIDs)}.
     * 
     * @param id the object id
     * @param kNNs the kNNs of the object
     * @param progress the progress object
     */
    private void putkNNsAndRkNNs(DBID id, List<DistanceResultPair<D>> kNNs, FiniteProgress progress) {
      materialized.put(id, kNNs);
      for(DistanceResultPair<D> kNN : kNNs) {
        List<DistanceResultPair<D>> rknns = materialized_RkNN.get(kNN.second);
        rknns.add(new DistanceResultPair<D>(kNN.first, id));
      }
      if(progress != null) {
        progress.incrementProcessed(logger);
      }
    }

    public List<DistanceResultPair<D>> getKNN(DBID id) {
      return materialized.get(id);
    }

    public List<DistanceResultPair<D>> getRKNN(DBID id) {
      return materialized_RkNN.get(id);
    }
    
    @SuppressWarnings({"unchecked" })
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
      return new PreprocessorRKNNQuery<O,S>(database, (Instance<O, S>) this);
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
      return "kNN and rknn preprocessor";
    }
    
    @Override
    public void contentChanged(DataStoreEvent<O> e) {
      if(e.getType().equals(Type.INSERT)) {
        //objectsInserted(e.getObjects().get(Type.INSERT));
      }
      else if(e.getType().equals(Type.DELETE)) {
        //objectsRemoved(e.getObjects().get(Type.DELETE));
      }

      else if(e.getType().equals(Type.UPDATE)) {
        // TODO
        throw new UnsupportedOperationException("Event type not supported: " + e.getType());
      }
      else {
        throw new UnsupportedOperationException("Event type not supported: " + e.getType());
      }
    }
  }
}