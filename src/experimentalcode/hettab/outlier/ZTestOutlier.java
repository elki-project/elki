package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 * @Title=("A Unified Approach to Spatial Outliers Detection")
 * @Description("Spatial Outlier Detection Algorithm")
 * author = "Shekhar, Shashi and Lu, Chang-Tien and Zhang, Pusheng", title = "A Unified Approach to Detecting Spatial Outliers" )
 */
public class ZTestOutlier<V extends NumberVector<?,?>> extends SingleAttributeSpatialOutlier<V>{

  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ZTestOutlier.class);

  /**
   * The association id to associate the SCORE of an object for the 
   * algorithm.
   */
  public static final AssociationID<Double> ZTEST_SCORE = AssociationID.getOrCreateAssociationID("score", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param z
   */
  public ZTestOutlier(NeighborSetPredicate.Factory<V> npredf, int z) {
    super(npredf,z);

  }

 
  public OutlierResult run(Database database, Relation<V> relation){
    WritableDataStore<Double> diffFromlocalMean = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    MeanVariance diffFromMeanMV = new MeanVariance();
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);

    for(DBID id : relation.getDBIDs()) {
      //
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      MeanVariance localMeanMV = new MeanVariance();
      // calculate and store mean
      for(DBID n : neighbors) {
        localMeanMV.put(relation.get(n).doubleValue(z)) ;
      }
      double diffFLM = relation.get(id).doubleValue(z) -localMeanMV.getMean() ;
      diffFromlocalMean.put(id, diffFLM);
      diffFromMeanMV.put(diffFLM);
      
    }
    

    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((diffFromlocalMean.get(id) - diffFromMeanMV.getMean()) / diffFromMeanMV.getSampleVariance());
      minmax.put(score);
      scores.put(id, score);
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("ZTest", "Z Test score", ZTEST_SCORE , scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
  
  /**
   * 
   * @author hettab
   *
   * @param <V>
   */
  public static class Parameterizer<V extends NumberVector<?,?>> extends SingleAttributeSpatialOutlier.Parameterizer<V> {
   
   
    
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }
   
    @Override
    protected ZTestOutlier<V> makeInstance() {
      return new ZTestOutlier<V>(npredf,z);
    }
    
  }

}

