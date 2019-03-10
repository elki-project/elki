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
package elki.outlier.spatial;

import elki.outlier.spatial.neighborhood.NeighborSetPredicate;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.math.DoubleMinMax;
import elki.math.Mean;
import elki.math.MeanVariance;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;

/**
 * Detect outliers by comparing their attribute value to the mean and standard
 * deviation of their neighborhood.
 * <p>
 * Reference:
 * <p>
 * S. Shekhar, C.-T. Lu, P. Zhang<br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * GeoInformatica 7-2, 2003
 * <p>
 * Description:
 * <p>
 * Z-Test Algorithm uses mean to represent the average non-spatial attribute
 * value of neighbors.<br>
 * The Difference e = non-spatial-attribute-value - mean (Neighborhood) is
 * computed.<br>
 * The Spatial Objects with the highest standardized e value are Spatial
 * Outliers.
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <N> Neighborhood type
 */
@Title("Z-Test Outlier Detection")
@Description("Outliers are detected by their z-deviation from the local mean.")
@Reference(authors = "S. Shekhar, C.-T. Lu, P. Zhang", //
    title = "A Unified Approach to Detecting Spatial Outliers", //
    booktitle = "GeoInformatica 7-2, 2003", //
    url = "https://doi.org/10.1023/A:1023455925009", //
    bibkey = "DBLP:journals/geoinformatica/ShekharLZ03")
public class CTLuZTestOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CTLuZTestOutlier.class);

  /**
   * Constructor.
   * 
   * @param npredf Neighbor predicate
   */
  public CTLuZTestOutlier(NeighborSetPredicate.Factory<N> npredf) {
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

    MeanVariance zmv = new MeanVariance();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      DBIDs neighbors = npred.getNeighborDBIDs(iditer);
      // Compute Mean of neighborhood
      Mean localmean = new Mean();
      for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iditer, iter)) {
          continue;
        }
        localmean.put(relation.get(iter).doubleValue(0));
      }
      final double localdiff;
      if(localmean.getCount() > 0) {
        localdiff = relation.get(iditer).doubleValue(0) - localmean.getMean();
      }
      else {
        localdiff = 0.0;
      }
      scores.putDouble(iditer, localdiff);
      zmv.put(localdiff);
    }

    // Normalize scores using mean and variance
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double score = Math.abs(scores.doubleValue(iditer) - zmv.getMean()) / zmv.getSampleStddev();
      minmax.put(score);
      scores.putDouble(iditer, score);
    }

    // Wrap result
    DoubleRelation scoreResult = new MaterializedDoubleRelation("ZTest", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0.0, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(or).addChild(npred);
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
    protected CTLuZTestOutlier<N> makeInstance() {
      return new CTLuZTestOutlier<>(npredf);
    }
  }
}
