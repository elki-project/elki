package experimentalcode.hettab.outlier;

import java.util.List;

import org.apache.commons.math.stat.descriptive.rank.Median;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.QuotientOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * FIXME: Documentation, Reference
 * 
 * @author Ahmed Hettab
 * 
 * @param <V>
 */
public class MedianMultipleAttributes<V extends NumberVector<?, ?>> extends MultipleAttributesSpatialOutlier<V> {
  /**
   * logger
   */
  public static final Logging logger = Logging.getLogger(MedianMultipleAttributes.class);

  /**
   * Constructor
   * 
   * @param npredf
   * @param dims
   */
  public MedianMultipleAttributes(NeighborSetPredicate.Factory<V> npredf, List<Integer> dims) {
    super(npredf, dims);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  public OutlierResult run(Relation<V> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(relation);
    Matrix hMatrix = new Matrix(getListZ_Dims().size(), relation.size());
    Matrix hMeansMatrix = new Matrix(getListZ_Dims().size(), 1);
    double dbSize = relation.size();

    int i = 0;
    for(Integer dim : getListZ_Dims()) {
      int j = 0;
      // h mean for each dim
      double hMeans = 0;
      for(DBID id : relation.getDBIDs()) {
        // f value
        double f = relation.get(id).doubleValue(dim);
        DBIDs neighbors = npred.getNeighborDBIDs(id);
        int nSize = neighbors.size();
        // g value
        double g[] = new double[nSize];
        Median m = new Median();
        int k = 0;
        for(DBID n : neighbors) {
          g[k] = relation.get(n).doubleValue(dim);
          k++;
        }
        double gm = m.evaluate(g);
        double h = f - gm;
        // add to h Matrix
        hMatrix.set(i, j, h);
        hMeans += h;
        j++;
      }

      hMeans = hMeans / dbSize;
      // add mean to h means hMeansMatrix
      hMeansMatrix.set(i, 0, hMeans);
      i++;
    }

    Matrix sigma = DatabaseUtil.covarianceMatrix(hMatrix);
    Matrix invSigma = sigma.inverse();

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDataStore<Double> scores = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC, Double.class);
    i = 0;
    for(DBID id : relation.getDBIDs()) {
      Matrix h_i = hMatrix.getColumn(i).minus(hMeansMatrix);
      Matrix h_iT = h_i.transpose();
      Matrix m = h_iT.times(invSigma);
      Matrix sM = m.times(h_i);
      double score = sM.get(0, 0);
      minmax.put(score);
      scores.put(id, score);
      i++;
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("MOF", "median-multipleattributes-outlier", TypeUtil.DOUBLE, scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new QuotientOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * FIXME: Documentation
   * 
   * @author hettab
   * 
   * @param <V>
   */
  public static class Parameterizer<V extends NumberVector<?, ?>> extends MultipleAttributesSpatialOutlier.Parameterizer<V> {
    @Override
    protected MedianMultipleAttributes<V> makeInstance() {
      return new MedianMultipleAttributes<V>(npredf, z);
    }
  }
}