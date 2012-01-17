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
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.outlier.BasicOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;

/**
 * Detect outliers by comparing their attribute value to the mean and standard
 * deviation of their neighborhood.
 * 
 * <p>
 * Reference: <br>
 * S. Shekhar and C.-T. Lu and P. Zhang <br>
 * A Unified Approach to Detecting Spatial Outliers<br>
 * in in GeoInformatica 7-2, 2003.
 * </p>
 * <p>
 * Description: <br>
 * Z-Test Algorithm uses mean to represent the average non-spatial attribute
 * value of neighbors. <br>
 * The Difference e = non-spatial-attribute-value - mean (Neighborhood) is
 * computed.<br>
 * The Spatial Objects with the highest standardized e value are Spatial
 * Outliers.
 * </p>
 * 
 * @author Ahmed Hettab
 * 
 * @param <N> Neighborhood type
 */
@Title("Z-Test Outlier Detection")
@Description("Outliers are detected by their z-deviation from the local mean.")
@Reference(authors = "S. Shekhar and C.-T. Lu and P. Zhang", title = "A Unified Approach to Detecting Spatial Outliers", booktitle = "GeoInformatica 7-2, 2003", url="http://dx.doi.org/10.1023/A:1023455925009")
public class CTLuZTestOutlier<N> extends AbstractNeighborhoodOutlier<N> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CTLuZTestOutlier.class);

  /**
   * Constructor
   * 
   * @param npredf
   */
  public CTLuZTestOutlier(NeighborSetPredicate.Factory<N> npredf) {
    super(npredf);
  }

  /**
   * Main method
   * 
   * @param database Database
   * @param nrel Neighborhood relation
   * @param relation Data relation (1d!)
   * @return Outlier detection result
   */
  public OutlierResult run(Database database, Relation<N> nrel, Relation<? extends NumberVector<?, ?>> relation) {
    final NeighborSetPredicate npred = getNeighborSetPredicateFactory().instantiate(nrel);
    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_STATIC);

    MeanVariance zmv = new MeanVariance();
    for(DBID id : relation.iterDBIDs()) {
      DBIDs neighbors = npred.getNeighborDBIDs(id);
      // Compute Mean of neighborhood
      Mean localmean = new Mean();
      for(DBID n : neighbors) {
        if(id.equals(n)) {
          continue;
        }
        else {
          localmean.put(relation.get(n).doubleValue(1));
        }
      }
      final double localdiff;
      if(localmean.getCount() > 0) {
        localdiff = relation.get(id).doubleValue(1) - localmean.getMean();
      }
      else {
        localdiff = 0.0;
      }
      scores.putDouble(id, localdiff);
      zmv.put(localdiff);
    }

    // Normalize scores using mean and variance
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : relation.iterDBIDs()) {
      double score = Math.abs(scores.doubleValue(id) - zmv.getMean()) / zmv.getSampleStddev();
      minmax.put(score);
      scores.putDouble(id, score);
    }

    // Wrap result
    Relation<Double> scoreResult = new MaterializedRelation<Double>("ZTest", "Z Test score", TypeUtil.DOUBLE, scores, relation.getDBIDs());
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
    protected CTLuZTestOutlier<N> makeInstance() {
      return new CTLuZTestOutlier<N>(npredf);
    }
  }
}