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
import elki.outlier.spatial.neighborhood.NeighborSetPredicate.Factory;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
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
 * Moran scatterplot outliers, based on the standardized deviation from the
 * local and global means. In contrast to the definition given in the reference,
 * we use this as a ranking outlier detection by not applying the signedness
 * test,
 * but by using the score (- localZ) * (Average localZ of Neighborhood)
 * directly.
 * This allows us to differentiate a bit between stronger and weaker outliers.
 * <p>
 * Reference:
 * <p>
 * S. Shekhar, C.-T. Lu, P. Zhang<br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * GeoInformatica 7-2, 2003
 * <p>
 * Moran scatterplot is a plot of normalized attribute values against the
 * neighborhood average of normalized attribute values. Spatial Objects on the
 * upper left or lower right are Spatial Outliers.
 *
 * @author Ahmed Hettab
 * @since 0.4.0
 *
 * @param <N> Neighborhood type
 */
@Title("Moran Scatterplot Outlier")
@Description("Spatial Outlier detection based on the standardized deviation from the local means.")
@Reference(authors = "S. Shekhar, C.-T. Lu, P. Zhang", //
    title = "A Unified Approach to Detecting Spatial Outliers", //
    booktitle = "GeoInformatica 7-2, 2003", //
    url = "https://doi.org/10.1023/A:1023455925009", //
    bibkey = "DBLP:journals/geoinformatica/ShekharLZ03")
public class CTLuMoranScatterplotOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CTLuMoranScatterplotOutlier.class);

  /**
   * Constructor.
   * 
   * @param npredf Neighborhood
   */
  public CTLuMoranScatterplotOutlier(Factory<N> npredf) {
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

    // Compute the global mean and variance
    MeanVariance globalmv = new MeanVariance();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      globalmv.put(relation.get(iditer).doubleValue(0));
    }

    DoubleMinMax minmax = new DoubleMinMax();
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    // calculate normalized attribute values
    // calculate neighborhood average of normalized attribute values.
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      // Compute global z score
      final double globalZ = (relation.get(iditer).doubleValue(0) - globalmv.getMean()) / globalmv.getNaiveStddev();
      // Compute local average z score
      Mean localm = new Mean();
      for(DBIDIter iter = npred.getNeighborDBIDs(iditer).iter(); iter.valid(); iter.advance()) {
        if(DBIDUtil.equal(iditer, iter)) {
          continue;
        }
        localm.put((relation.get(iter).doubleValue(0) - globalmv.getMean()) / globalmv.getNaiveStddev());
      }
      // if neighors.size == 0
      final double localZ;
      if(localm.getCount() > 0) {
        localZ = localm.getMean();
      }
      else {
        // if s has no neighbors => Wzi = zi
        localZ = globalZ;
      }

      // compute score
      // Note: in the original moran scatterplot, any object with a score < 0
      // would be an outlier.
      final double score = Math.max(-globalZ * localZ, 0);
      minmax.put(score);
      scores.putDouble(iditer, score);
    }

    DoubleRelation scoreResult = new MaterializedDoubleRelation("MoranOutlier", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 0);
    OutlierResult or = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(or).addChild(npred);
    return or;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getNeighborSetPredicateFactory().getInputTypeRestriction(), TypeUtil.NUMBER_VECTOR_FIELD_1D);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
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
  public static class Par<N> extends AbstractNeighborhoodOutlier.Par<N> {
    @Override
    public CTLuMoranScatterplotOutlier<N> make() {
      return new CTLuMoranScatterplotOutlier<>(npredf);
    }
  }
}
