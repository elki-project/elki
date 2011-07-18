package experimentalcode.hettab.outlier;

import org.apache.commons.math.stat.StatUtils;
import org.apache.commons.math.stat.descriptive.rank.Median;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * A Trimmed Mean Approach to finding Spatial Outliers
 * 
 * @author Ahmed Hettab
 * @param <N> Neighborhood object type
 */
@Title("A Trimmed Mean Approach to Finding Spatial Outliers")
@Description("a local trimmed mean approach to evaluating the spatial outlier factor which is the degree that a site is outlying compared to its neighbors")
// FIXME: Reference
public class TrimmedMeanApproach<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrimmedMeanApproach.class);

  /**
   * The association id to associate the TR_SCORE of an object for the TR
   * algorithm.
   */
  public static final AssociationID<Double> TR_SCORE = AssociationID.getOrCreateAssociationID("tr", Double.class);

  /**
   * the parameter p
   */
  private double p;

  /**
   * Constructor
   * 
   * @param p Parameter p
   * @param npredf Neighborhood factory.
   */
  protected TrimmedMeanApproach(NeighborSetPredicate.Factory<N> npredf, double p) {
    super(npredf);
    this.p = p;
  }

  /**
   * Run the algorithm
   * 
   * @param database Database
   * @param neighbors Neighborhood relation
   * @param relation Relation
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> neighbors, Relation<? extends NumberVector<?, ?>> relation) {
    assert (DatabaseUtil.dimensionality(relation) == 1) : "TrimmedMean can only process one-dimensional data sets.";
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(neighbors);

    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);

    // calculate the error Term
    Matrix temp1 = Matrix.identity(relation.size(), relation.size()).minus(getNeighborhoodMatrix(relation, npred));
    Matrix temp2 = getSpatialAttributeMatrix(relation).minus(getLocalTrimmedMeanMatrix(relation, npred));
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
    i = 0;
    double[] temp = new double[relation.size()];
    for(DBID id : relation.getDBIDs()) {
      temp[i] = Math.abs(error.get(id) - median_i);
      i++;
    }
    MAD = median.evaluate(temp);

    // calculate score
    DoubleMinMax minmax = new DoubleMinMax();
    i = 0;
    for(DBID id : relation.getDBIDs()) {
      double score = temp[i] * 0.6745 / MAD;
      scores.put(id, score);
      minmax.put(score);
      i++;
    }
    //
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("OTR", "Trimmedmean-outlier", TR_SCORE, scores);
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  /**
   * the neighborhood Matrix
   * 
   */
  // TODO: can we do this more efficiently?
  public Matrix getNeighborhoodMatrix(Relation<? extends NumberVector<?, ?>> relation, NeighborSetPredicate npred) {
    Matrix m = new Matrix(relation.size(), relation.size());
    int i = 0;
    for(DBID id : relation.iterDBIDs()) {
      int j = 0;
      for(DBID n : relation.iterDBIDs()) {
        if(npred.getNeighborDBIDs(id).contains(n)) {
          m.set(i, j, 1);
        }
        else {
          m.set(i, j, 0);
        }
        j++;
      }
      i++;
    }
    m.normalizeColumns();
    return m;
  }

  /**
   * return the Local trimmed Mean Matrix
   */
  public Matrix getLocalTrimmedMeanMatrix(Relation<? extends NumberVector<?, ?>> relation, NeighborSetPredicate npred) {
    Matrix m = new Matrix(relation.size(), 1);
    int i = 0;
    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int j = 0;
      double[] aValues = new double[neighbors.size()];
      for(DBID n : neighbors) {
        aValues[j] = relation.get(n).doubleValue(1);
        j++;
      }
      m.set(i, 0, StatUtils.percentile(aValues, p * 100));
      i++;
    }
    return m;
  }

  /**
   * return the non Spatial atribut value Matrix
   * 
   */
  public Matrix getSpatialAttributeMatrix(Relation<? extends NumberVector<?, ?>> relation) {
    Matrix m = new Matrix(relation.size(), 1);
    int i = 0;
    for(DBID id : relation.iterDBIDs()) {
      m.set(i, 0, relation.get(id).doubleValue(1));
      i++;
    }
    return m;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // Get one dimensional attribute for analysis.
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  /**
   * Parameterizer
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    /**
     * Parameter for the percentile value p
     */
    public static final OptionID P_ID = OptionID.getOrCreateOptionID("tma.p", "the percentile parameter");

    /**
     * Percentile parameter p
     */
    protected double p = 0.2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter pP = new DoubleParameter(P_ID, new GreaterConstraint(0.0));
      if(config.grab(pP)) {
        p = pP.getValue();
      }
    }

    @Override
    protected TrimmedMeanApproach<N> makeInstance() {
      return new TrimmedMeanApproach<N>(npredf, p);
    }

  }
}
