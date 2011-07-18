package experimentalcode.hettab.outlier;



import org.apache.commons.math.stat.descriptive.rank.Median;

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
 * @Title("Algorithms for Spatial Outlier Detection")
 * @Description("Spatial Outlier Detection Algorithm")
 *  @Reference(authors = "Chang-Tien Lu", title = "Algorithms for Spatial Outlier Detection", booktitle = "Proceedings of the Third IEEE International Conference on Data Mining")
 * @author Ahmed Hettab
 *
 * @param <V>
 */
public class MedianAlgorithm<V extends NumberVector<?, ?>> extends SingleAttributeSpatialOutlier<V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(MedianAlgorithm.class);
  /**
   * The association id to associate the SCORE of an object for the 
   * algorithm.
   */
  public static final AssociationID<Double> MEDIAN_SCORE = AssociationID.getOrCreateAssociationID("score", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param z
   */
  public MedianAlgorithm(NeighborSetPredicate.Factory<V> npredf, int z) {
    super(npredf,z);
  }

  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation){
    WritableDataStore<Double> gi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> hi = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    //
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      //
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int size = neighbors.size();
      double[] fi = new double[size];
      Median m = new Median();
      int i = 0;
      // calculate and store Median of neighborhood
      for(DBID n : neighbors) {
        fi[i] = relation.get(n).doubleValue(z);
        i++;
      }
      double median = m.evaluate(fi);
      gi.put(id, median);
      double h = relation.get(id).doubleValue(z) - median ;
      hi.put(id, h);
      mv.put(h);
    }
    
    
    MinMax<Double> minmax = new MinMax<Double>();
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((hi.get(id)- mv.getMean())/mv.getNaiveVariance());
      minmax.put(score);
      scores.put(id, score);
    }

    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("MOF", "Median-single-attribut-outlier", MEDIAN_SCORE, scores);
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
    protected MedianAlgorithm<V> makeInstance() {
      return new MedianAlgorithm<V>(npredf,z);
    }
    
  }

}
