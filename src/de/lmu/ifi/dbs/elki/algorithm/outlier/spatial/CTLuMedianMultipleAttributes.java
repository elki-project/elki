package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Median Approach is used to discover spatial outliers with multiple
 * attributes.
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
 * {@link de.lmu.ifi.dbs.elki.datasource.filter.normalization.AttributeWiseVarianceNormalization
 * AttributeWiseVarianceNormalization} filter.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Spatial Vector
 * @param <O> Non Spatial Vector
 */
@Reference(authors = "Chang-Tien Lu and Dechang Chen and Yufeng Kou", title = "Detecting Spatial Outliers with Multiple Attributes", booktitle = "Proc. 15th IEEE International Conference on Tools with Artificial Intelligence, 2003", url = "http://dx.doi.org/10.1109/TAI.2003.1250179")
public class CTLuMedianMultipleAttributes<N, O extends NumberVector<?, ?>> extends AbstractNeighborhoodOutlier<N> {
  /**
   * logger
   */
  private static final Logging LOG = Logging.getLogger(CTLuMedianMultipleAttributes.class);

  /**
   * Constructor
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuMedianMultipleAttributes(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Run the algorithm
   * 
   * @param spatial Spatial relation
   * @param attributes Attributes relation
   * @return Outlier detection result
   */
  public OutlierResult run(Relation<N> spatial, Relation<O> attributes) {
    final int dim = DatabaseUtil.dimensionality(attributes);
    if(LOG.isDebugging()) {
      LOG.debug("Dimensionality: " + dim);
    }
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(spatial);

    CovarianceMatrix covmaker = new CovarianceMatrix(dim);
    WritableDataStore<Vector> deltas = DataStoreUtil.makeStorage(attributes.getDBIDs(), DataStoreFactory.HINT_TEMP, Vector.class);
    for(DBIDIter iditer = attributes.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final O obj = attributes.get(iditer);
      final DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      // Compute the median vector
      final Vector median;
      {
        double[][] data = new double[dim][neighbors.size()];
        int i = 0;
        // Load data
        for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
          // TODO: skip object itself within neighbors?
          O nobj = attributes.get(iter);
          for(int d = 0; d < dim; d++) {
            data[d][i] = nobj.doubleValue(d + 1);
          }
          i++;
        }
        double[] md = new double[dim];
        for(int d = 0; d < dim; d++) {
          md[d] = QuickSelect.median(data[d]);
        }
        median = new Vector(md);
      }

      // Delta vector "h"
      Vector delta = obj.getColumnVector().minusEquals(median);
      deltas.put(iditer, delta);
      covmaker.put(delta);
    }
    // Finalize covariance matrix:
    Vector mean = covmaker.getMeanVector();
    Matrix cmati = covmaker.destroyToSampleMatrix().inverse();

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(attributes.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = attributes.iterDBIDs(); iditer.valid(); iditer.advance()) {
      Vector temp = deltas.get(iditer).minus(mean);
      final double score = temp.transposeTimesTimes(cmati, temp);
      minmax.put(score);
      scores.putDouble(iditer, score);
    }

    Relation<Double> scoreResult = new MaterializedRelation<Double>("Median multiple attributes outlier", "median-outlier", TypeUtil.DOUBLE, scores, attributes.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
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
   * @param <O> Attributes vector type
   */
  public static class Parameterizer<N, O extends NumberVector<?, ?>> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected CTLuMedianMultipleAttributes<N, O> makeInstance() {
      return new CTLuMedianMultipleAttributes<N, O>(npredf);
    }
  }
}