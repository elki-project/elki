package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Scatterplot-outlier is a spatial outlier detection method that performs a
 * linear regression of object attributes and their neighbors average value.
 * 
 * <p>
 * Reference: <br>
 * S. Shekhar and C.-T. Lu and P. Zhang <br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * in in GeoInformatica 7-2, 2003.
 * </p>
 * 
 * <p>
 * Scatterplot shows attribute values on the X-axis and the average of the
 * attribute values in the neighborhood on the Y-axis. Best fit regression line
 * is used to identify spatial outliers. Vertical difference of a data point
 * tells about outlierness.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood object type
 */
@Title("Scatterplot Spatial Outlier")
@Description("Spatial Outlier Detection Algorithm using linear regression of attributes and the mean of their neighbors.")
@Reference(authors = "S. Shekhar and C.-T. Lu and P. Zhang", title = "A Unified Approach to Detecting Spatial Outliers", booktitle = "GeoInformatica 7-2, 2003", url="http://dx.doi.org/10.1023/A:1023455925009")
public class CTLuScatterplotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CTLuScatterplotOutlier.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuScatterplotOutlier(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  /**
   * Main method
   * 
   * @param nrel Neighborhood relation
   * @param relation Data relation (1d!)
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);
    WritableDataStore<Double> means = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, Double.class);

    // Calculate average of neighborhood for each object and perform a linear
    // regression using the covariance matrix
    CovarianceMatrix covm = new CovarianceMatrix(2);
    for(DBID id : relation.iterDBIDs()) {
      final double local = relation.get(id).doubleValue(1);
      // Compute mean of neighbors
      Mean mean = new Mean();
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      for(DBID n : neighbors) {
        if(id.equals(n)) {
          continue;
        }
        mean.put(relation.get(n).doubleValue(1));
      }
      final double m;
      if(mean.getCount() > 0) {
        m = mean.getMean();
      }
      else {
        // if object id has no neighbors ==> avg = non-spatial attribute of id
        m = local;
      }
      // Store the mean for the score calculation
      means.put(id, m);
      covm.put(new double[] { local, m });
    }
    // Finalize covariance matrix, compute linear regression
    final double slope, inter;
    {
      double[] meanv = covm.getMeanVector().getArrayRef();
      Matrix fmat = covm.destroyToSampleMatrix();
      final double covxx = fmat.get(0, 0);
      final double covxy = fmat.get(0, 1);
      slope = covxy / covxx;
      inter = meanv[1] - slope * meanv[0];
    }

    // calculate mean and variance for error
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.iterDBIDs()) {
      // Compute the error from the linear regression
      double y_i = relation.get(id).doubleValue(1);
      double e = means.get(id) - (slope * y_i + inter);
      scores.put(id, e);
      mv.put(e);
    }

    // Normalize scores
    DoubleMinMax minmax = new DoubleMinMax();
    {
      final double mean = mv.getMean();
      final double variance = mv.getNaiveStddev();
      for(DBID id : relation.iterDBIDs()) {
        double score = Math.abs((scores.get(id) - mean) / variance);
        minmax.put(score);
        scores.put(id, score);
      }
    }
    // build representation
    Relation<Double> scoreResult = new MaterializedRelation<Double>("SPO", "Scatterplot-Outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), VectorFieldTypeInformation.get(NumberVector.class, 1));
  }

  /**
   * Parameterization class
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected CTLuScatterplotOutlier<N> makeInstance() {
      return new CTLuScatterplotOutlier<N>(npredf);
    }
  }
}