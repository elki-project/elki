package experimentalcode.shared.outlier.generalized.neighbors;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
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
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
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
public class KNearestNeighborNeighborhood<D extends Distance<D>> implements NeighborSetPredicate{
  
  /**
   * Parameter to specify the number of neighbors
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("neighborhood.k","the number of neighbors");
  
  /**
   * Parameter to specify the distance function to use
   */
  public static final OptionID DISTANCEFUNCTION_ID = OptionID.getOrCreateOptionID("neighborhood.distancefunction","the distance function to use");
  
  /**
   * Data store for result.
   */
  DataStore<DBIDs> store;
  
  /**
   * Constructor.
   * 
   * @param store Store to access
   */
  public KNearestNeighborNeighborhood(DataStore<DBIDs> store) {
    this.store = store;
  }
  
  
  @Override
  public DBIDs getNeighborDBIDs(DBID reference) {
    return store.get(reference);
  }
  
   /**
    * 
    */
  public static class Factory<D extends Distance<D>> implements NeighborSetPredicate.Factory<DatabaseObject>{
    
    /**
     * 
     */
    private int k ;
    
    /**
     * 
     */
    private DistanceFunction<DatabaseObject,D> distFunc ;
    
    /**
     * 
     */
     public Factory(int k , DistanceFunction<DatabaseObject,D> distFunc){
       super();
       this.k = k ;
       this.distFunc = distFunc ;
     }
    /**
     * 
     */
    public NeighborSetPredicate instantiate(Database< ? extends DatabaseObject> database) {
      DataStore<DBIDs> store = getKNNNeighbors(database);
      return new KNearestNeighborNeighborhood<D>(store);
    }
    
    /**
     * method to get the K-Neighborhood
     */
    //TODO DistanceResultPair 
    private DataStore<DBIDs> getKNNNeighbors(Database< ? extends DatabaseObject> database){
      WritableDataStore<DBIDs> store = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC | DataStoreFactory.HINT_TEMP, DBIDs.class);
      KNNQuery<?,D> knnQuery = database.getKNNQuery(distFunc, KNNQuery.HINT_EXACT);
      
      for(DBID id : database){
         List<DistanceResultPair<D>> neighbors = knnQuery.getKNNForDBID(id, k);
         ArrayModifiableDBIDs neighbours = DBIDUtil.newArray();
         for(DistanceResultPair<D> drp : neighbors){
           neighbours.add(drp.second);
         }
         store.put(id,neighbours);
         System.out.println(neighbours);
      }
      
      return store ;
    }
    
    /**
     * Factory method for {@link Parameterizable}
     * 
     * @param config Parameterization
     * @return instance
     */
   
    public static <D extends Distance<D>> KNearestNeighborNeighborhood.Factory<D> parameterize(Parameterization config) {
      int k = getParameterK(config);
      DistanceFunction<DatabaseObject,D> distFunc = getParameterDistanceFunction(config);
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
    
  }
 
  

