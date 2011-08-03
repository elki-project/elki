package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Mean Approach is used to discover spatial outliers with multiple attributes.
 * 
 * <p>
 * Reference:<br>
 * Chang-Tien Lu and Dechang Chen and Yufeng Kou:<br>
 * Detecting Spatial Outliers with Multiple Attributes<br>
 * in 15th IEEE International Conference on Tools with Artificial Intelligence,
 * 2003
 * </p>
 * 
 * <p>
 * Implementation note: attribute standardization is not used; this is
 * equivalent to using the
 * {@link de.lmu.ifi.dbs.elki.datasource.filter.AttributeWiseVarianceNormalization
 * AttributeWiseVarianceNormalization} filter.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector
 * @param <O> Attribute Vector
 */
@Reference(authors = "Chang-Tien Lu and Dechang Chen and Yufeng Kou", title = "Detecting Spatial Outliers with Multiple Attributes", booktitle = "Proc. 15th IEEE International Conference on Tools with Artificial Intelligence, 2003", url = "http://dx.doi.org/10.1109/TAI.2003.1250179")
public class CTLuMeanMultipleAttributes<N, O extends NumberVector<?, ?>> extends AbstractNeighborhoodOutlier<N> {
  /**
   * logger
   */
  public static final Logging logger = Logging.getLogger(CTLuMeanMultipleAttributes.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuMeanMultipleAttributes(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  public OutlierResult run(Relation<N> spatial, Relation<O> attributes) {
    if(logger.isDebugging()) {
      logger.debug("Dimensionality: " + DatabaseUtil.dimensionality(attributes));
    }
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);

    CovarianceMatrix covmaker = new CovarianceMatrix(DatabaseUtil.dimensionality(attributes));
    WritableDataStore<Vector> deltas = DataStoreUtil.makeStorage(attributes.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);
    for(DBID id : attributes.iterDBIDs()) {
      final O obj = attributes.get(id);
      final DBIDs neighbors = npred.getNeighborDBIDs(id);
      // TODO: remove object itself from neighbors?

      // Mean vector "g"
      Vector mean = Centroid.make(attributes, neighbors);
      // Delta vector "h"
      Vector delta = obj.getColumnVector().minus(mean);
      deltas.put(id, delta);
      covmaker.put(delta);
    }
    // Finalize covariance matrix:
    Vector mean = covmaker.getMeanVector();
    Matrix cmati = covmaker.destroyToSampleMatrix().inverse();

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(attributes.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    for(DBID id : attributes.iterDBIDs()) {
      Vector temp = deltas.get(id).minus(mean);
      final Vector res = temp.transposeTimes(cmati).times(temp);
      assert (res.getDimensionality() == 1);
      double score = res.get(0);
      minmax.put(score);
      scores.put(id, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("mean multiple attributes spatial outlier", "mean-multipleattributes-outlier", TypeUtil.DOUBLE, scores, attributes.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @apiviz.exclude
   * 
   * @param <N> Neighborhood type
   * @param <O> Attribute object type
   */
  public static class Parameterizer<N, O extends NumberVector<?, ?>> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected CTLuMeanMultipleAttributes<N, O> makeInstance() {
      return new CTLuMeanMultipleAttributes<N, O>(npredf);
    }
  }
}