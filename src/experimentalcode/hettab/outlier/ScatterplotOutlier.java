package experimentalcode.hettab.outlier;

import org.apache.commons.math.stat.regression.SimpleRegression;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate.Factory;

/**
 * 
 * @author Ahmed Hettab
 *
 * @param <V>
 */
public class ScatterplotOutlier<V extends NumberVector<?, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ScatterplotOutlier.class);

  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use in comparison step.");

  /**
   * Our predicate to obtain the neighbors
   */
  NeighborSetPredicate.Factory<Object> npredf = null;

  /**
   * 
   * Holds the Y value
   */
  private static final OptionID Y_ID = OptionID.getOrCreateOptionID("dim.y", "the dimension of non spatial attribut");

  /**
   * parameter y
   */
  private int y;

  /**
   * The association id to associate the SCORE of an object for the 
   * algorithm.
   */
  public static final AssociationID<Double> SCATTERPLOT_SCORE = AssociationID.getOrCreateAssociationID("score", Double.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param y
   */
  public ScatterplotOutlier(Factory<Object> npredf, int y) {
    super();
    this.npredf = npredf;
    this.y = y;
  }

  /**
   * 
   * @param <V>
   * @param config
   * @return
   */
  public static <V extends NumberVector<V, ?>> ScatterplotOutlier<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<Object> npred = getNeighborPredicate(config);
    final int y = getParameterY(config);
    if(config.hasErrors()) {
      return null;
    }
    return new ScatterplotOutlier<V>(npred, y);
  }

  protected static int getParameterY(Parameterization config) {
    final IntParameter param = new IntParameter(Y_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0;
  }

  /**
   * 
   * @param config
   * @return
   */
  public static NeighborSetPredicate.Factory<Object> getNeighborPredicate(Parameterization config) {
    final ObjectParameter<NeighborSetPredicate.Factory<Object>> param = new ObjectParameter<NeighborSetPredicate.Factory<Object>>(NEIGHBORHOOD_ID, NeighborSetPredicate.Factory.class, true);
    if(config.grab(param)) {
      return param.instantiateClass(config);
    }
    return null;
  }

  public OutlierResult run(Database database, Relation<V> relation) {
    WritableDataStore<Double> means = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    
    
    final NeighborSetPredicate npred = npredf.instantiate(relation);
    
    //calculate average for each object
    for(DBID id : relation.getDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      double d = neighbors.size() ;
      double avg = 0 ;
      for(DBIDs n : neighbors){
         if(n.contains(id)){
           d = neighbors.size() -1 ;
           continue ;
         }
         else{
           avg += relation.get(id).doubleValue(y)/d;
         }
      }
      means.put(id, avg);
    }
    
    //calculate errors using regression
    SimpleRegression regression = new SimpleRegression();
    for(DBID id : relation.getDBIDs()){
      regression.addData(relation.get(id).doubleValue(y), means.get(id));
    }
    for(DBID id : relation.getDBIDs()){
      double y_i = relation.get(id).doubleValue(y);
      double e = means.get(id) - (regression.getIntercept()*y_i + regression.getSlope());
      error.put(id, e);
    }
    
    // calculate mean and variance for error
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.getDBIDs()) {
      mv.put(error.get(id));
    }
    double mean = mv.getMean();
    double variance = mv.getSampleVariance();
    
    //calculate scores
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

}
 


