package experimentalcode.hettab.outlier;

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

/**
 * 
 * @author Ahmed Hettab
 *
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
  @Title("SLOM: a new measure for local spatial outliers")
  @Description("spatial local outlier measure (SLOM), which captures the local behaviour of datum in their spatial neighbourhood")
  @Reference(authors = "Sanjay Chawla and  Pei Sun", title = "SLOM: a new measure for local spatial outliers", booktitle = "Knowledge and Information Systems 2005", url = "http://rp-www.cs.usyd.edu.au/~chawlarg/papers/KAIS_online.pdf")
 

public class SLOM<O extends DatabaseObject, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedAlgorithm<O, D, OutlierResult> implements OutlierAlgorithm<O, OutlierResult> {

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(SLOM.class);

  /**
   * Parameter to specify the number of nearest neighbors of an object to be
   * considered for computing its SLOM_SCORE. must be an integer greater than
   * 1.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("slom.k", "The number of nearest neighbors of an object to be considered for computing its SLOM_SCORE.");

  /**
   * Holds the value of {@link #K_ID}.
   */
  private int k;

  /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SLOM_SCORE = AssociationID.getOrCreateAssociationID("slom", Double.class);

  /**
   * 
   * @param k
   */
  protected SLOM(int k) {
    super(new EmptyParameterization());
    this.k = k;
  }

  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * 
   */
  @Override
  protected OutlierResult runInTime(Database<O> database) throws IllegalStateException {

    DistanceQuery<O, D> distFunc = database.getDistanceQuery(getDistanceFunction());
    
    // Modified Distance
    WritableDataStore<Double> modifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    //average of modified distance
    WritableDataStore<Double> avgModifiedDistance = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    //beta
    WritableDataStore<Double> betaList = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, Double.class);
    KNNQuery<O,D> knnQuery = database.getKNNQuery(distFunc, k, DatabaseQuery.HINT_HEAVY_USE);
    //first step :
    //compute modified distance
    for(DBID id : database.getIDs()){
      double maxDist = 0 ;
      double sum = 0 ;
       for( DistanceResultPair<D> dResultPair : knnQuery.getKNNForDBID(id, k)){
         if(dResultPair.first.doubleValue()>0){
           maxDist = dResultPair.first.doubleValue() ;
         }
         sum = sum + dResultPair.first.doubleValue() ;
       }
       modifiedDistance.put(id, (sum-maxDist)/(knnQuery.getKNNForDBID(id, k).size()-1));
    }
    
    //second step :
    //compute average modified distance of neighborhood 
    for(DBID id : database.getIDs()){
      double avg = 0 ;
      double beta = 0 ;
      
      for( DistanceResultPair<D> dResultPair : knnQuery.getKNNForDBID(id, k)){
          avg = avg + modifiedDistance.get(dResultPair.getID()).doubleValue() ;
        }
      
      avgModifiedDistance.put(id, avg);
      
      for(DistanceResultPair<D> dResultPair : knnQuery.getKNNForDBID(id, k)){
        if(modifiedDistance.get(dResultPair.getID()).doubleValue()>avg){
          beta++ ;
        }
        if(modifiedDistance.get(dResultPair.getID()).doubleValue()<avg){
          beta--;
        }    
      }
      beta = Math.abs(beta);
      beta = Math.max(beta, 1)/(knnQuery.getKNNForDBID(id, k).size()-2);
      beta = beta/(1+avg);
      betaList.put(id,beta);
   }
    //compute SLOM for each Object
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> sloms = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : database.getIDs()){
      double slom = betaList.get(id)*modifiedDistance.get(id);
      sloms.put(id, slom);
      minmax.put(slom);
    }
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SLOM", "SLOM-outlier", SLOM_SCORE, sloms);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
    
    
  }

  /**nfluence Outlier Score
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return SLOM Outlier Algorithm
   */
  public static <O extends DatabaseObject, D extends NumberDistance<D, ?>> SLOM<O, D> parameterize(Parameterization config) {
    int k = getParameterK(config);
    if(config.hasErrors()) {
      return null;
    }
    return new SLOM<O, D>(k);
  }

  /**
   * Get parameter k
   * 
   * @param config Parameterization
   * @return k value
   */
  protected static int getParameterK(Parameterization config) {
    final IntParameter param = new IntParameter(K_ID, new GreaterConstraint(1));
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }
}
