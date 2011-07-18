package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate.Factory;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
 * @author hettab
 *
 * @param <V>
 */
public class MoranScatterPlotOutlier<V extends NumberVector<?,?>> extends SingleAttributeSpatialOutlier<V> {

  /**
    * The logger for this class.
    */
  private static final Logging logger = Logging.getLogger(MoranScatterPlotOutlier.class);

   /**
   * The association id to associate the SLOM_SCORE of an object for the SLOM
   * algorithm.
   */
  public static final AssociationID<Double> SCORE = AssociationID.getOrCreateAssociationID("Moran-Outlier", Double.class);
  /**
   * Constructor
   * @param npredf
   * @param z
   */
  public MoranScatterPlotOutlier(Factory<V> npredf, int z) {
    super(npredf, z);
    
  }
  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation) {
  WritableDataStore<Double> stdZ = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
  WritableDataStore<Double> neighborStdZ = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
  final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);
  MeanVariance stdZMV = new MeanVariance();
  
  for(DBID id : relation.getDBIDs()) {
     stdZMV.put(relation.get(id).doubleValue(z));
  }
  
  System.out.println(stdZMV.getMean());
  System.out.println(stdZMV.getSampleVariance());
  
  for(DBID id : relation.getDBIDs()) {
    double zValue = (relation.get(id).doubleValue(z)-stdZMV.getMean())/stdZMV.getSampleVariance();
    stdZ.put(id, zValue);
    double neighborZValue = 0 ;
    for(DBID n : npred.getNeighborDBIDs(id)){
      neighborZValue += (relation.get(n).doubleValue(z)-stdZMV.getMean())/stdZMV.getSampleVariance();
     }
    neighborStdZ.put(id, neighborZValue/(double)npred.getNeighborDBIDs(id).size());
    }

  MinMax<Double> minmax = new MinMax<Double>();
  WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
  for(DBID id : relation.getDBIDs()) {
    double score = stdZ.get(id)*neighborStdZ.get(id);
    System.out.println(score);
    if(score<0){
    minmax.put(1.0);
    scores.put(id, 1.0);
    }
    else{
      minmax.put(0.0);
      scores.put(id, 0.0);
    }
  }

  AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("MoranOutlier", "Moran Scatterplot Outlier", SCORE , scores);
  OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
  return new OutlierResult(scoreMeta, scoreResult);
}
  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return logger ;
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
    protected MoranScatterPlotOutlier<V> makeInstance() {
      return new MoranScatterPlotOutlier<V>(npredf,z);
    }
    
  }
  
  
}
