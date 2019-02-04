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
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Scatterplot-outlier is a spatial outlier detection method that performs a
 * linear regression of object attributes and their neighbors average value.
 * <p>
 * Reference:
 * <p>
 * S. Shekhar, C.-T. Lu, P. Zhang<br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * GeoInformatica 7-2, 2003
 * <p>
 * Scatterplot shows attribute values on the X-axis and the average of the
 * attribute values in the neighborhood on the Y-axis. Best fit regression line
 * is used to identify spatial outliers. Vertical difference of a data point
 * tells about outlierness.
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <N> Neighborhood object type
 */
@Title("Scatterplot Spatial Outlier")
@Description("Spatial Outlier Detection Algorithm using linear regression of attributes and the mean of their neighbors.")
@Reference(authors = "S. Shekhar, C.-T. Lu, P. Zhang", //
    title = "A Unified Approach to Detecting Spatial Outliers", //
    booktitle = "GeoInformatica 7-2, 2003", //
    url = "https://doi.org/10.1023/A:1023455925009", //
    bibkey = "DBLP:journals/geoinformatica/ShekharLZ03")
public class CTLuScatterplotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CTLuScatterplotOutlier.class);

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood predicate
   */
  public CTLuScatterplotOutlier(NeighborSetPredicate.Factory<N> npredf) {
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
    WritableDoubleDataStore means = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP);

    // Calculate average of neighborhood for each object and perform a linear
    // regression using the covariance matrix
    CovarianceMatrix covm = new CovarianceMatrix(2);
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      final double local = relation.get(iditer).doubleValue(0);
      // Compute mean of neighbors
      Mean mean = new Mean();
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iditer, iter)) {
          continue;
        }
        mean.put(relation.get(iter).doubleValue(0));
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
      means.putDouble(iditer, m);
      covm.put(new double[] { local, m });
    }
    // Finalize covariance matrix, compute linear regression
    final double slope, inter;
    {
      double[] meanv = covm.getMeanVector();
      double[][] fmat = covm.destroyToSampleMatrix();
      final double covxx = fmat[0][0], covxy = fmat[0][1];
      slope = covxy / covxx;
      inter = meanv[1] - slope * meanv[0];
    }

    // calculate mean and variance for error
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);
    MeanVariance mv = new MeanVariance();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Compute the error from the linear regression
      double y_i = relation.get(iditer).doubleValue(0);
      double e = means.doubleValue(iditer) - (slope * y_i + inter);
      scores.putDouble(iditer, e);
      mv.put(e);
    }

    // Normalize scores
    DoubleMinMax minmax = new DoubleMinMax();
    {
      final double mean = mv.getMean();
      final double variance = mv.getNaiveStddev();
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        double score = Math.abs((scores.doubleValue(iditer) - mean) / variance);
        minmax.put(score);
        scores.putDouble(iditer, score);
      }
    }
    // build representation
    DoubleRelation scoreResult = new MaterializedDoubleRelation("SPO", "Scatterplot-Outlier", scores, relation.getDBIDs());
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
    protected CTLuScatterplotOutlier<N> makeInstance() {
      return new CTLuScatterplotOutlier<>(npredf);
    }
  }
}
