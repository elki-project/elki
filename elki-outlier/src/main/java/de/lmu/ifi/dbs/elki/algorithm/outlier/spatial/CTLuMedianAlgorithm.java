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

import de.lmu.ifi.dbs.elki.algorithm.outlier.spatial.neighborhood.NeighborSetPredicate;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.DoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedDoubleRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.datastructures.QuickSelect;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Median Algorithm of C.-T. Lu
 * <p>
 * Reference:
 * <p>
 * C.-T. Lu, D. Chen, Y. Kou<br>
 * Algorithms for Spatial Outlier Detection <br>
 * Proc. 3rd IEEE International Conference on Data Mining (ICDM)
 * <p>
 * Median Algorithm uses Median to represent the average non-spatial attribute
 * value of neighbors.<br>
 * The Difference e = non-spatial-Attribute-Value - Median (Neighborhood) is
 * computed.<br>
 * The Spatial Objects with the highest standardized e value are Spatial
 * Outliers.
 * 
 * @author Ahmed Hettab
 * @since 0.4.0
 * 
 * @param <N> Neighborhood type
 */
@Title("Median Algorithm for Spatial Outlier Detection")
@Reference(authors = "C.-T. Lu, D. Chen, Y. Kou", //
    title = "Algorithms for Spatial Outlier Detection", //
    booktitle = "Proc. 3rd IEEE International Conference on Data Mining", //
    url = "https://doi.org/10.1109/ICDM.2003.1250986", //
    bibkey = "DBLP:conf/icdm/LuCK03")
public class CTLuMedianAlgorithm<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CTLuMedianAlgorithm.class);

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuMedianAlgorithm(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  /**
   * Main method.
   * 
   * @param database Database
   * @param nrel Neighborhood relation
   * @param relation Data relation (1d!)
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(database, nrel);
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    MeanVariance mv = new MeanVariance();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      final double median;
      {
        double[] fi = new double[neighbors.size()];
        // calculate and store Median of neighborhood
        int c = 0;
        for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
          if(DBIDUtil.equal(iditer, iter)) {
            continue;
          }
          fi[c] = relation.get(iter).doubleValue(0);
          c++;
        }

        if(c > 0) {
          median = QuickSelect.median(fi, 0, c);
        }
        else {
          median = relation.get(iditer).doubleValue(0);
        }
      }
      double h = relation.get(iditer).doubleValue(0) - median;
      scores.putDouble(iditer, h);
      mv.put(h);
    }

    // Normalize scores
    final double mean = mv.getMean();
    final double stddev = mv.getNaiveStddev();
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double score = Math.abs((scores.doubleValue(iditer) - mean) / stddev);
      minmax.put(score);
      scores.putDouble(iditer, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("MO", "Median-outlier", scores, relation.getDBIDs());
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
  }

  /**
   * Parameterization class.
   * 
   * @author Ahmed Hettab
   * 
   * @hidden
   * 
   * @param <N> Neighborhood object type
   */
  public static class Parameterizer<N> extends AbstractNeighborhoodOutlier.Parameterizer<N> {
    @Override
    protected CTLuMedianAlgorithm<N> makeInstance() {
      return new CTLuMedianAlgorithm<>(npredf);
    }
  }
}
