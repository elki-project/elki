package experimentalcode.hettab.outlier;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * SLOM Algorithm
 * @author Ahmed Hettab
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
  @Title("SLOM: a new measure for local spatial outliers")
  @Description("spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
  @Reference(authors = "Sanjay Chawla and  Pei Sun", title = "SLOM: a new measure for local spatial outliers", booktitle = "Knowledge and Information Systems 2005", url = "http://rp-www.cs.usyd.edu.au/~chawlarg/papers/KAIS_online.pdf")
 

public class SLOM<O extends DatabaseObject,  D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D , OutlierResult> implements OutlierAlgorithm<O, OutlierResult> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its SLOM_SCORE. must be an integer greater than 2.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("slom.k", "The number of nearest neighbors of an object to be considered for computing its SLOM_SCORE.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k ;
  
  /**
   * Parameter to specify the neighborhood distance function to use ;
   */
  public static final OptionID SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.spatialdistancefunction","The distance function to use for spatial attributes");
  
  /**
   * Parameter to specify the non spatial distance function to use
   */
  public static final OptionID NON_SPATIAL_DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("slom.nonspatialdistancefunction","The distance function to use for non spatial attributes");
  
  /**
   * Holds the value of {@link #SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<O,D> neighborhoodDistanceFunction ;
  
  /**
   * Holds the value of {@link #NON_SPATIAL_DISTANCE_FUNCTION_ID}
   */
  protected PrimitiveDistanceFunction<O,D> nonSpatialDistanceFunction ;
  
  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   * @param config
   */
  protected SLOM(int k ,PrimitiveDistanceFunction<O,D> neighborhoodDistanceFunction , PrimitiveDistanceFunction<O,D> nonSpatialDistanceFunction ) {
    super(new EmptyParameterization());
    this.k = k ;
    this.neighborhoodDistanceFunction = neighborhoodDistanceFunction ;
    this.nonSpatialDistanceFunction = nonSpatialDistanceFunction ;
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {
	  
	  DistanceQuery<O,D> distFunc = database.getDistanceQuery(neighborhoodDistanceFunction);
	    
	   // Modified Distance
	    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
	   //average of modified distance 
	    WritableDataStore<Double> avgModifiedDistancePlus = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
	  //average of modified distance 
	    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
	    //beta
	    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
	    KNNQuery<O,D> knnQuery = database.getKNNQuery(distFunc, k+1, DatabaseQuery.HINT_HEAVY_USE);
	   
	    for(DBID id : database.getIDs()){
	        double sum = 0 ;
	        List<DistanceResultPair<D>> dResultPairs = knnQuery.getKNNForDBID(id, k+1);
	        double maxDist = 0 ;
	        for( int i = 1 ; i<= k ; i ++){
	          double dist =  nonSpatialDistanceFunction.distance( database.get(id) , database.get(dResultPairs.get(i).getID()) ).doubleValue() ;
	          sum = sum + dist ;
	          if(dist > maxDist ){
	        	  maxDist = dist ;
	          }  
	        }
	        modifiedDistance.put(id, (sum-maxDist)/(k-1));
	     }
	    
	  //second step :
	    //compute average modified distance of id neighborhood and id it's self
	    //compute average modified distance of only id neighborhood
	    for(DBID id : database.getIDs()){
	      double avgPlus = 0 ;
	      double avg = 0 ;
	      double beta = 0 ;
	      List<DistanceResultPair<D>> dResultPairs = knnQuery.getKNNForDBID(id, k+1);
	      //compute avg
	      for( DistanceResultPair<D> dResultPair : dResultPairs){
	          if(dResultPair.second==id){
	          avgPlus = avgPlus + nonSpatialDistanceFunction.distance( database.get(id) , database.get(dResultPair.getID()) ).doubleValue() ;
	          }
	          else{
	            avg = avg + nonSpatialDistanceFunction.distance( database.get(id) , database.get(dResultPair.getID()) ).doubleValue() ;
	            avgPlus = avgPlus + nonSpatialDistanceFunction.distance( database.get(id) , database.get(dResultPair.getID()) ).doubleValue() ;
	          }
	        }
	      avgPlus = avg/(dResultPairs.size()) ;
	      avg = avg/(dResultPairs.size()-1) ;
	      avgModifiedDistancePlus.put(id, avgPlus);
	      avgModifiedDistance.put(id, avg);
	      
	      for(DistanceResultPair<D> dResultPair : dResultPairs){
	        if(modifiedDistance.get(dResultPair.second).doubleValue()>avgPlus){
	          beta++ ;
	        }
	        if(modifiedDistance.get(dResultPair.second).doubleValue()<avgPlus){
	          beta--;
	        }    
	      }
	      beta = Math.abs(beta);
	      beta = Math.max(beta, 1)/(dResultPairs.size()-2);
	      beta = beta/(1+avg);
	      betaList.put(id,beta);
	    
	   }
	       
	  //compute SLOM for each Object
	    MinMax<Double> minmax = new MinMax<Double>();
	    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
	    for(DBID id : database.getIDs()){
	      double slom = modifiedDistance.get(id);
	      sloms.put(id, slom);
	      minmax.put(slom);
	    }
	    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
	    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
	    return new OutlierResult(scoreMeta, scoreResult);
        
  }
  
  /**
   * 
   * @param database
   * @param k
   * @return
   */
  public List<Integer> getNeighborhood(Database<O> database , int k){
	  return null ;
  }
  
  
 
  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }
  
  /**SLOM Outlier Score
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return SLOM Outlier Algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> SLOM<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    PrimitiveDistanceFunction<O, D> neighborhooddistanceFunction = getNeighborhoodDistanceFunction(config);
    PrimitiveDistanceFunction<O, D> nonSpatialDistanceFunction = getNonSpatialDistanceFunction(config);
    if(config.hasErrors()) {
      return null;
    }
    
    return new SLOM<O, D>(k , neighborhooddistanceFunction, nonSpatialDistanceFunction);
  }

  /**
   * Get parameter k
   * 
   * @param config Parameterization
   * @return k value
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(2));
    if(config.grab(param)) {
      return param.getValue();
    }
    return -1;
  }
  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends DistanceFunction<?, ?>> F getNeighborhoodDistanceFunction(Parameterization config) {
	    final ObjectParameter<F> param = new ObjectParameter<F>(SPATIAL_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
	    if(config.grab(param)) {
	      return param.instantiateClass(config);
	    }
	    return null;
	  }
  /**
   * 
   * @param <F>
   * @param config
   * @return
   */
  protected static <F extends DistanceFunction<?, ?>> F getNonSpatialDistanceFunction(Parameterization config) {
	    final ObjectParameter<F> param = new ObjectParameter<F>(NON_SPATIAL_DISTANCE_FUNCTION_ID, DistanceFunction.class, true);
	    if(config.grab(param)) {
	      return param.instantiateClass(config);
	    }
	    return null;
	  }

}