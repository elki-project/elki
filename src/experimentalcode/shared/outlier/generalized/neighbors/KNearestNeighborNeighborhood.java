package experimentalcode.shared.outlier.generalized.neighbors;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * 
 * @author Ahmed Hettab
 *
 * @param <D> Distance to use
 */
public class KNearestNeighborNeighborhood<D extends Distance<D>> implements NeighborSetPredicate , Result{
  
  /**
   * Parameter k
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("neighborhood.k","the number of neighbors");
  
  /**
   * Parameter to specify the distance function to use
   */
  public static final OptionID DISTANCEFUNCTION_ID = OptionID.getOrCreateOptionID("neighborhood.distancefunction","the distance function to use");
  
  /**
   * Data store for result.
   */
  DataStore<List<DistanceResultPair<D>>> store;
  
  /**
   * Data store for result.
   */
  DataStore<DBIDs> dbIDsStore ;
    
  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public KNearestNeighborNeighborhood(DataStore<List<DistanceResultPair<D>>> store, DataStore<DBIDs> dbIDsStore ) {
    this.store = store;
    this.dbIDsStore = dbIDsStore ;
   }
  
  
  /**
   * 
   */
  @Override
  public DBIDs getNeighborDBIDs(DBID reference) {
    return dbIDsStore.get(reference);
  }
    
   /**
    * 
    */
  public static class Factory<D extends Distance<D>> implements NeighborSetPredicate.Factory<Object>{
    
    /**
     * parameter k
     */
    private int k ;
    
    /**
     * distance function to use
     */
    private DistanceFunction<Object,D> distFunc ;
    
    /**
     * Factory Constructor
     */
     public Factory(int k , DistanceFunction<Object,D> distFunc){
       super();
       this.k = k ;
       this.distFunc = distFunc ;
     }
     
    /**
     * instantiate Neighbor Predicate
     */
    public NeighborSetPredicate instantiate(Relation<?> database) {
      DataStore<List<DistanceResultPair<D>>> store = getKNNNeighbors(database);
      DataStore<DBIDs> s = getNeighbors(database) ;
      return new KNearestNeighborNeighborhood<D>(store,s);
    }
    
    /**
     * method to get the K-Neighborhood
     * 
     */ 
    private DataStore<List<DistanceResultPair<D>>> getKNNNeighbors(Relation<?> database){
      WritableDataStore<List<DistanceResultPair<D>>> store = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, List.class);   
      KNNQuery<?,D> knnQuery = QueryUtil.getKNNQuery(database, distFunc, KNNQuery.HINT_EXACT);
      for(DBID id : database.iterDBIDs()){
         List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k);       
         store.put(id, neighbors);
      }
      return store ;
    }
    
    /**
     * method to get the K-Neighborhood
     */
    private DataStore<DBIDs> getNeighbors(Relation<?> database){
      WritableDataStore<DBIDs> s = DataStoreUtil.makeStorage(database.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);
      KNNQuery<?,D> knnQuery = QueryUtil.getKNNQuery(database, distFunc, KNNQuery.HINT_EXACT);
      for(DBID id : database.iterDBIDs()){
         List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k);
         ArrayModifiableDBIDs neighbours = DBIDUtil.newArray();
         for(DistanceResultPair<D> dpair : neighbors){
           neighbours.add(dpair.getDBID());
         }
         s.put(id, neighbours);
         
      }  
      return s ;
    }
    
    /**
     * Factory method for {@link Parameterizable}
     * 
     * @param config Parameterization
     * @return instance
     */
   
    public static <D extends Distance<D>> KNearestNeighborNeighborhood.Factory<D> parameterize(Parameterization config) {
      int k = getParameterK(config);
      DistanceFunction<Object,D> distFunc = getParameterDistanceFunction(config);
      if(config.hasErrors()) {
        return null;
      }
      return new KNearestNeighborNeighborhood.Factory<D>(k,distFunc);
    }

    /**
     * Get the k parameter.
     * 
     * @param config Parameterization
     * @return k or -1
     */
    public static int getParameterK(Parameterization config) {
      final IntParameter param = new IntParameter(K_ID);
      if(config.grab(param)) {
        return param.getValue();
      }
      return -1 ;
    }
   }
  
  /**
   * Grab the distance configuration option.
   * 
   * @param <F> distance function type
   * @param config Parameterization
   * @return Parameter value or null.
   */
  protected static <F extends DistanceFunction<?,?>> F getParameterDistanceFunction(Parameterization config) {
    final ObjectParameter<F> param = new ObjectParameter<F>(DISTANCEFUNCTION_ID, DistanceFunction.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }


  @Override
  public String getLongName() {   
    return "K Nearest Neighbors Neighborhood";
  }


  @Override
  public String getShortName() {
    return "k-neighbors-neighborhood";
  }
    
  }
 
  

