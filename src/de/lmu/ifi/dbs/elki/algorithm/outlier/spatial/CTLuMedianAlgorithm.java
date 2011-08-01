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
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.statistics.QuickSelect;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Median Algorithm of C.-T. Lu
 * 
 * <p>
 * Reference: <br>
 * Chang-Tien Lu <br>
 * Algorithms for Spatial Outlier Detection <br>
 * in Third IEEE International Conference on Data Mining <br>
 * </p>
 * 
 * Median Algorithm uses Median to represent the average non-spatial attribute
 * value of neighbors. <br>
 * The Difference e = non-spatial-Attribute-Value - Median (Neighborhood) is
 * computed.<br>
 * The Spatial Objects with the highest standardized e value are Spatial
 * Outliers. </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood type
 */
@Title("Median Algorithm for Spatial Outlier Detection")
@Reference(authors = "Chang-Tien Lu", title = "Algorithms for Spatial Outlier Detection", booktitle = "Proceedings of the Third IEEE International Conference on Data Mining")
public class CTLuMedianAlgorithm<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CTLuMedianAlgorithm.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuMedianAlgorithm(NeighborSetPredicate.Factory<N> npredf) {
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
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);

    MeanVariance mv = new MeanVariance();
    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      final double median;
      {
        double[] fi = new double[neighbors.size()];
        // calculate and store Median of neighborhood
        int c = 0;
        for(DBID n : neighbors) {
          if(id.equals(n)) {
            continue;
          }
          fi[c] = relation.get(n).doubleValue(1);
          c++;
        }

        if(c > 0) {
          // Note: only use up to c-1, since we may have used a too big array
          median = QuickSelect.median(fi, 0, c - 1);
        }
        else {
          median = relation.get(id).doubleValue(1);
        }
      }
      double h = relation.get(id).doubleValue(1) - median;
      scores.put(id, h);
      mv.put(h);
    }

    // Normalize scores
    final double mean = mv.getMean();
    final double stddev = mv.getNaiveStddev();
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : relation.iterDBIDs()) {
      double score = Math.abs((scores.get(id) - mean) / stddev);
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("MO", "Median-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
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
    protected CTLuMedianAlgorithm<N> makeInstance() {
      return new CTLuMedianAlgorithm<N>(npredf);
    }
  }
}