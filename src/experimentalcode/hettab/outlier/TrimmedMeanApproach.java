package experimentalcode.hettab.outlier;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.rank.Median;

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
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import experimentalcode.shared.outlier.generalized.neighbors.NeighborSetPredicate;

/**
 * A Trimmed Mean Approach to finding Spatial Outliers
 *  
 * @author Ahmed Hettab
 * @param <V>
 */

@Title("A Trimmed Mean Approach to Finding Spatial Outliers")
@Description("a local trimmed mean approach to evaluating the spatial outlier factor which is the degree that a site is outlying compared to its neighbors")
public class TrimmedMeanApproach<V extends NumberVector<?, ?>> extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrimmedMeanApproach.class);

  /**
   * Parameter to specify the neighborhood predicate to use.
   */
  public static final OptionID NEIGHBORHOOD_ID = OptionID.getOrCreateOptionID("neighborhood", "The neighborhood predicate to use");

  /**
   * Our predicate to obtain the neighbors
   */
  NeighborSetPredicate.Factory<Object> npredf = null;

  /**
   * 
   * Holds the p value
   */
  private static final OptionID P_ID = OptionID.getOrCreateOptionID("tma.p", "the p parameter");

  /**
   * the parameter p
   */
  private double p;

  /**
   * 
   * Holds the y value
   */
  private static final OptionID Y_ID = OptionID.getOrCreateOptionID("dim.y", "the non spatial attribut");

  /**
   * Holds the dimension of y attribute
   */
  private int y;

  /**
   * The association id to associate the TR_SCORE of an object for the TR
   * algorithm.
   */
  public static final AssociationID<Double> TR_SCORE = AssociationID.getOrCreateAssociationID("tr", Double.class);

  /**
   * Constructor
   * @param p
   * @param y
   * @param npredf
   */
  protected TrimmedMeanApproach(double p, int y, NeighborSetPredicate.Factory<Object> npredf) {
    this.p = p;
    this.y = y;
    this.npredf = npredf;
  }

  /**
   * 
   * @param database
   * @param relation
   * @return
   */
    public OutlierResult run(Database database, Relation<V> relation) {
   
    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);

    final NeighborSetPredicate npred = npredf.instantiate(relation);
    
    //calculate the error Term
    Matrix temp1 = Matrix.identity(relation.size(), relation.size()).minus(getNeighborhoodMatrix(relation, npred));
    Matrix temp2 = getSpatialAttributMatrix(relation).minus(getLocalTrimmedMeanMatrix(relation, npred));
    Matrix E = temp1.times(temp2);


    // calculate the median of error Term
    int i = 0;
    double[] ei = new double[relation.size()];
    Median median = new Median();
    for(DBID id : relation.getDBIDs()) {
      error.put(id, E.get(i, 0));
      ei[i] = E.get(i, 0);
      i++;
    }
    double median_i = median.evaluate(ei);

    // calculate MAD
    double MAD;
    i= 0;
    double[] temp = new double[relation.size()];
    for(DBID id : relation.getDBIDs()) {
      temp[i] = Math.abs(error.get(id) - median_i);
      i++;
    }
    MAD = median.evaluate(temp);

    // calculate score
    MinMax<Double> minmax = new MinMax<Double>();
    i = 0;
    for(DBID id : relation.getDBIDs()) {
      double score = temp[i] * 0.6745 / MAD;
      scores.put(id, score);
      minmax.put(score);
      System.out.println(score);
      i++;
    }
    //
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("OTR", "Trimmedmean-outlier", TR_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);

  }
  
  /**
   *  the neighborhood Matrix 
   *   
   */
  public Matrix getNeighborhoodMatrix(Relation<V> relation , NeighborSetPredicate npred){
    Matrix m = new Matrix(relation.size(),relation.size());
    int i = 0 ;
    for(DBID id : relation.iterDBIDs()){
      int j = 0 ;
      for(DBID n : relation.iterDBIDs()){
        if(npred.getNeighborDBIDs(id).contains(n)){
          m.set(i, j, 1);
        }
        else{
          m.set(i, j, 0);
        }
        j++;
      }
      i++ ;
    }
   m.normalizeColumns();
   return m ;
  }
  
  /**
   * return the Local trimmed Mean Matrix
   */
  public Matrix getLocalTrimmedMeanMatrix(Relation<V> relation , NeighborSetPredicate npred){
    Matrix m = new Matrix(relation.size(),1);
    int i = 0 ;
    for(DBID id : relation.iterDBIDs()){
        DBIDs neighbors = npred.getNeighborDBIDs(id);
        int j = 0 ;
        double[] aValues = new double[neighbors.size()];
        for(DBID n :neighbors){
          aValues[j] = relation.get(n).doubleValue(y);
          j++ ;
        }
        m.set(i, 0, StatUtils.percentile(aValues, p*100));
        i++ ;
    }
    return m ;
  }
  
  /**
   * return the non Spatial atribut value Matrix
   * 
   */
  public Matrix getSpatialAttributMatrix(Relation<V> relation){
    Matrix m = new Matrix(relation.size(),1) ;
    int i = 0 ;
    for(DBID id : relation.iterDBIDs()) {
      m.set(i, 0, relation.get(id).doubleValue(y));
      i++ ;
    }  
   return m ;
  }

  /**
   * 
   * @param <V>
   * @param config
   * @return
   */
  public static <V extends NumberVector<V, ?>> TrimmedMeanApproach<V> parameterize(Parameterization config) {
    final NeighborSetPredicate.Factory<Object> npred = getNeighborPredicate(config);
    final double p = getParameterP(config);
    final int y = getParameterY(config);
    if(config.hasErrors()) {
      return null;
    }
    return new TrimmedMeanApproach<V>(p, y, npred);
  }

  /**
   * Get the p parameter
   * 
   * @param config Parameterization
   * @return p parameter
   */
  protected static double getParameterP(Parameterization config) {
    final DoubleParameter param = new DoubleParameter(P_ID);
    if(config.grab(param)) {
      return param.getValue();
    }
    return 0.0;
  }

  /**
   * Get the y parameter
   * 
   * @param config Parameterization
   * @return y parameter
   */
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

  /**
   * 
   */
  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }
}
