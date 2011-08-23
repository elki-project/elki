package de.lmu.ifi.dbs.elki.algorithm.outlier.spatial;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
 * C.-T. Lu and D. Chen and Y. Kou<br>
 * Algorithms for Spatial Outlier Detection <br>
 * in Proc. 3rd IEEE International Conference on Data Mining <br>
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
@Reference(authors = "C.-T. Lu and D. Chen and Y. Kou", title = "Algorithms for Spatial Outlier Detection", booktitle = "Proc. 3rd IEEE International Conference on Data Mining", url="http://dx.doi.org/10.1109/ICDM.2003.1250986")
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
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    or.addChildResult(npred);
    return or;
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