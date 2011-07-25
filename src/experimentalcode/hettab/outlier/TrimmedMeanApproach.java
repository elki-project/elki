package experimentalcode.hettab.outlier;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.AbstractNeighborhoodOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.statistics.QuickSelect;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * <p>
 * Reference: <br>
 * Tianming Hu and Sam Yuan Sung<br>
 * A Trimmed Mean Approach to finding Spatial Outliers<br>
 * in Intelligent Data Analysis, Volume 8, 2004.
 * </p>
 * 
 * <p>
 * the contiguity Matrix is definit as <br>
 * wij = 1/k if j is neighbor of i, k is the neighbors size of i.
 * </p>
 * 
 * @author Ahmed Hettab
 * @param <N> Neighborhood object type
 */
@Title("A Trimmed Mean Approach to Finding Spatial Outliers")
@Description("a local trimmed mean approach to evaluating the spatial outlier factor which is the degree that a site is outlying compared to its neighbors")
@Reference(authors = "Tianming Hu and Sam Yuan Sung", title = "A trimmed mean approach to finding spatial outliers", booktitle = "Intell. Data Anal. pages 79-95 ,2004 volume8 ")
public class TrimmedMeanApproach<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(TrimmedMeanApproach.class);

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
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    assert (DatabaseUtil.dimensionality(relation) == 1) : "TrimmedMean can only process one-dimensional data sets.";
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);

    WritableDataStore<Double> error = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, double.class);

    for(DBID id : relation.getDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      int i = 0;
      double[] values = new double[neighbors.size()];
      // calculate trimmedMean
      for(DBID n : neighbors) {
        values[i] = relation.get(n).doubleValue(1);
        i++;
      }
      // calculate local trimmed Mean and error term
      double tm = QuickSelect.quantile(values, p);
      error.put(id, relation.get(id).doubleValue(1) - tm);
    }

    // calculate the median of error Term
    double[] ei = new double[relation.size()];
    {
      int i = 0;
      for(DBID id : relation.getDBIDs()) {
        ei[i] = error.get(id);
        i++;
      }
    }
    double median_i = QuickSelect.median(ei);

    // calculate MAD
    double[] temp = new double[relation.size()];
    {
      int i = 0;
      for(DBID id : relation.getDBIDs()) {
        temp[i] = Math.abs(error.get(id) - median_i);
        i++;
      }
    }
    double MAD = QuickSelect.median(temp);

    // calculate score
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : relation.getDBIDs()) {
      double score = error.get(id) * 0.6745 / MAD;
      scores.put(id, score);
      minmax.put(score);
    }
    //
    Relation<Double> scoreResult = new MaterializedRelation<Double>("TrimmedMean", "Trimmed Mean Score", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
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
