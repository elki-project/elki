/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Median Approach is used to discover spatial outliers with multiple
 * attributes.
 * <p>
 * Reference:
 * <p>
 * C.-T. Lu, D. Chen, Y. Kou<br>
 * Detecting Spatial Outliers with Multiple Attributes<br>
 * Proc. 15th IEEE Int. Conf. Tools with Artificial Intelligence (TAI 2003)
 * <p>
 * Implementation note: attribute standardization is not used; this is
 * equivalent to using the
 * {@link de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise.AttributeWiseVarianceNormalization
 * AttributeWiseVarianceNormalization} filter.
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <N> Spatial Vector
 * @param <O> Non Spatial Vector
 */
@Reference(authors = "C.-T. Lu, D. Chen, Y. Kou", //
    title = "Detecting Spatial Outliers with Multiple Attributes", //
    booktitle = "Proc. 15th IEEE Int. Conf. Tools with Artificial Intelligence (TAI 2003)", //
    url = "https://doi.org/10.1109/TAI.2003.1250179", //
    bibkey = "DBLP:conf/ictai/LuCK03")
public class CTLuMedianMultipleAttributes<N, O extends NumberVector> extends AbstractNeighborhoodOutlier<N> {
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
   * @param database Database
   * @param spatial Spatial relation
   * @param attributes Attributes relation
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> spatial, Relation<O> attributes) {
    final int dim = RelationUtil.dimensionality(attributes);
    if(LOG.isDebugging()) {
      LOG.debug("Dimensionality: " + dim);
    }
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(database, spatial);

    CovarianceMatrix covmaker = new CovarianceMatrix(dim);
    WritableDataStore<double[]> deltas = DataStoreUtil.makeStorage(attributes.getDBIDs(), DataStoreFactory.HINT_TEMP, double[].class);
    for(DBIDIter iditer = attributes.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final O obj = attributes.get(iditer);
      final DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      // Compute the median vector
      final double[] median = new double[dim];
      {
        double[][] data = new double[dim][neighbors.size()];
        int i = 0;
        // Load data
        for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
          // TODO: skip object itself within neighbors?
          O nobj = attributes.get(iter);
          for(int d = 0; d < dim; d++) {
            data[d][i] = nobj.doubleValue(d);
          }
          i++;
        }
        for(int d = 0; d < dim; d++) {
          median[d] = QuickSelect.median(data[d]);
        }
      }

      // Delta vector "h"
      double[] delta = minusEquals(obj.toArray(), median);
      deltas.put(iditer, delta);
      covmaker.put(delta);
    }
    // Finalize covariance matrix:
    double[] mean = covmaker.getMeanVector();
    double[][] cmati = inverse(covmaker.destroyToSampleMatrix());

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(attributes.getDBIDs(), DataStoreFactory.HINT_STATIC);
    for(DBIDIter iditer = attributes.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Note: we modify deltas here
      double[] v = minusEquals(deltas.get(iditer), mean);
      final double score = transposeTimesTimes(v, cmati, v);
      minmax.put(score);
      scores.putDouble(iditer, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("Median multiple attributes outlier", "median-outlier", scores, attributes.getDBIDs());
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
   * @hidden
   * 
   * @param <N> Neighborhood type
   * @param <O> Attributes vector type
   */
  public static class Parameterizer<N, O extends NumberVector> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected CTLuMedianMultipleAttributes<N, O> makeInstance() {
      return new CTLuMedianMultipleAttributes<>(npredf);
    }
  }
}
