package experimentalcode.hettab.outlier;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.math.stat.regression.SimpleRegression;

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
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 */
public class ScatterplotOutlier<V extends NumberVector<?, ?>> extends SingleAttributeSpatialOutlier<V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ScatterplotOutlier.class);
  /**
   * The association id to associate the SCORE of an object for the 
   * algorithm.
   */
  public static final AssociationID<Double> SCATTERPLOT_SCORE = AssociationID.getOrCreateAssociationID("score", Double.class);
  /**
   * Constructor
   * 
   * @param npredf
   * @param z
   */
  public ScatterplotOutlier(NeighborSetPredicate.Factory<V> npredf, int z) {
    super(npredf,z);
  }
  /**
   * 
   * @param database
   * @param relation
   * @return
   */
  public OutlierResult run(Database database, Relation<V> relation) {
    WritableDataStore<Double> means = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);   
    
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);
    
    //calculate average for each object
    for(DBID id : relation.getDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double d = neighbors.size() ;
      double avg = 0 ;
      for(DBID n : neighbors){
           avg += relation.get(n).doubleValue(z);
         }
      means.put(id, avg/d);
    }
    
    //calculate errors using regression
    SimpleRegression regression = new SimpleRegression();
    for(DBID id : relation.getDBIDs()){
      regression.addData(relation.get(id).doubleValue(z), means.get(id));
    }
    
 // calculate mean and variance for error
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()){
      double y_i = relation.get(id).doubleValue(z);
      double e = means.get(id) - (regression.getSlope()*y_i + regression.getIntercept());
      error.put(id, e);
      mv.put(e);
    }
    
   
    double mean = mv.getMean();
    double variance = mv.getSampleVariance();
    
    MinMax<Double> minmax = new MinMax<Double>();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    for(DBID id : relation.getDBIDs()) {
      double score = Math.abs((error.get(id) - mean) / variance);
      minmax.put(score);
      scores.put(id, score);
      
    } 
    //build representation
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("SPO", "Scatterplot-Outlier", SCATTERPLOT_SCORE, scores);
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
    protected ScatterplotOutlier<V> makeInstance() {
      return new ScatterplotOutlier<V>(npredf , z);
    }
    
  }

}
 


