package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleObjPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.hettab.neighborhood.DistanceBasedNeighborSetPredicate;


/**
 * <p>
 * Random Walk on Exhaustive Combination
 * <br>
 * Liu, Xutong and Lu, Chang-Tien and Chen, Feng,
 * <br>
 * Spatial outlier detection: random walk based approaches,
 * <br>
 * in Proceedings of the 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems,2010  
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector
 * @param<O>  non Spatial Vector
 * @param <D> Distance to use
 */
@Title("Random Walk on Exhaustive Combination")
@Description("Random Walk on Exhaustive Combination, which detect spatial Outlier")
@Reference(authors = "Liu, Xutong and Lu, Chang-Tien and Chen, Feng", title = "Spatial outlier detection: random walk based approaches", booktitle = "in Proceedings of the 18th SIGSPATIAL International Conference on Advances in Geographic Information Systems,2010")
public class RandomWalkEC<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedNeighborhoodOutlier<N,D> {
  /**
   * Logger
   */
  private static final Logging logger = Logging.getLogger(RandomWalkEC.class);
  /**
   * Parameter alpha
   */
  private double alpha;
  /**
   * parameter c
   */
  private double c;
  /**
   * Constructor
   * @param npredf
   * @param alpha
   * @param c
   */
  public RandomWalkEC(DistanceBasedNeighborSetPredicate.Factory<N,D> npredf, double alpha, double c) {
  super(npredf);
	this.alpha = alpha;
	this.c = c;
}

  /**
   * 
   * @param nrel
   * @param relation
   * @return
   */
public OutlierResult run(Database database, Relation<N> spatial, Relation<? extends NumberVector<?, ?>> relation) {
    final DistanceBasedNeighborSetPredicate<N, D> npred = getNeighborSetPredicateFactory().instantiate(spatial);
    PrimitiveDistanceFunction<N,D> distFunc = getNeighborSetPredicateFactory().getSpatialDistanceFunction();
    WritableDataStore<Matrix> similarityVectors = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, Matrix.class);
    WritableDataStore<List<Pair<DBID, Double>>> simScores = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_TEMP, List.class);
    
    // construct the relation Matrix of the ec-graph
    Matrix E = new Matrix(spatial.size(), spatial.size());
    int i = 0;
    for(DBID id : spatial.iterDBIDs()) {
      int j = 0;
      for(DBID n : spatial.iterDBIDs()) {
        double e;
        if(n.equals(id)) {
          e = 0;
        }
        else {
          double dist = distFunc.distance(spatial.get(id), spatial.get(n)).doubleValue() ;
          double diff = Math.abs(relation.get(id).doubleValue(1) - relation.get(n).doubleValue(1));
          diff = (Math.pow(diff,alpha));
          double exp = Math.exp(diff);
          e = (1/exp) * (1/dist);
        }
        E.set(i, j, e);
        j++;
      }
      i++;
    }
    // normalize the adjacent Matrix
    E.normalizeColumns() ;
    Matrix I = Matrix.identity(spatial.size(), spatial.size());
    Matrix temp1 = E.times(-c) ;
    Matrix temp2 = I.minus(temp1);
    temp2 .inverse() ;
    Matrix W = temp2.times((1-c));
    
    
    // compute similarity vector for each Object
    int count = 0;
    for(DBID id : spatial.iterDBIDs()) {
     Matrix ei = new Matrix(relation.size(),1);
     ei.set(count, 0, 1);
     Matrix sim = W.times(ei);
     similarityVectors.put(id, sim);
     count ++ ;
    }
    //compute the relevance scores between specified Object and its neighbors   
    for(DBID id : spatial.iterDBIDs()){
      Collection<DoubleObjPair<DBID>> neighbours = npred.getDistanceBasedNeighbors(id);
      ArrayList<Pair<DBID, Double>> value = new ArrayList<Pair<DBID,Double>>();
      for(DoubleObjPair<DBID> n : neighbours){
        double sim = cosineSimilarity(similarityVectors.get(id).getColumnVector(0), similarityVectors.get(n.second).getColumnVector(0));
        Pair<DBID,Double> pair = new Pair<DBID, Double>(n.second, sim);
        value.add(pair);
      }
      simScores.put(id, value);
    }
    
    //compute geometric mean
    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(spatial.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : spatial.iterDBIDs()) {
      List<Pair<DBID, Double>> simScore = simScores.get(id);
      double score = 1 ;
      double cnt = 0 ;
      for(Pair<DBID,Double> pair : simScore){
        if(id.equals(pair.first)){
          continue ;
        }
        else{
          score *= pair.second ;
          cnt ++ ;
        }
      }
      //add Scores
      double s = Math.pow(score, 1/cnt);
      minmax.put(s);
      scores.put(id,s);
      System.out.println(s);
      }
    
    Relation<Double> scoreResult = new MaterializedRelation<Double>("randomwalkec", "RandomWalkEC", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0.0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * Computes the cosine similarity for two given feature vectors.
   */
  private static double cosineSimilarity(Vector v1, Vector v2) {
    double p = 0;
    double p1 = 0;
    double p2 = 0;
    for(int i = 0; i < v1.getDimensionality(); i++) {
      p += v1.get(i) * v2.get(i);
      p1 += v1.get(i) * v1.get(i);
      p2 += v2.get(i) * v2.get(i);
    }
    return (p / (Math.sqrt(p1) * Math.sqrt(p2)));
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * 
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <V>
   * @param <D>
   */
  public static class Parameterizer<N, O, D extends NumberDistance<D, ?>> extends AbstractDistanceBasedNeighborhoodOutlier.Parameterizer<N, D> {
    
    
    /**
     * Parameter to specify alpha
     */
    public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("randomwalkec.alpha", "helps compute more accurate edge value");
    /**
     * Parameter to specify the c
     */
    public static final OptionID C_ID = OptionID.getOrCreateOptionID("randomwalkec.c", "the Parameter c");
    /**
     * 
     */
    double alpha = 0.5;   
    /**
     * 
     */
    double c = 0.9;
    /**
     * 
     */
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configAlpha(config);
      configC(config);
    }

    /**
     * Get the alpha parameter
     * 
     * @param config Parameterization
     * @return alpha parameter
     */
    protected void configAlpha(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(ALPHA_ID);
      if(config.grab(param)) {
        alpha = param.getValue();
      }
    }

    /**
     * get the c parameter
     * 
     * @param config
     * @return
     */
    protected void configC(Parameterization config) {
      final DoubleParameter param = new DoubleParameter(C_ID);
      if(config.grab(param)) {
        c = param.getValue();
      }
    }

    
    @Override
    protected RandomWalkEC<N,O, D> makeInstance() {
      return new RandomWalkEC<N,O, D>( npredf, alpha, c);
    }
  }
  
}