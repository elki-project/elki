/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.outlier.clustering;

import java.util.List;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Noise as outliers, from a clustering algorithm.
 * <p>
 * This algorithm simply treats all "noise" cluster members as outliers.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class NoiseAsOutliers implements OutlierAlgorithm {
  /**
   * Clustering algorithm to use
   */
  ClusteringAlgorithm<?> clustering;

  /**
   * Constructor.
   *
   * @param clustering Clustering algorithm
   */
  public NoiseAsOutliers(ClusteringAlgorithm<?> clustering) {
    super();
    this.clustering = clustering;
  }

  @Override
  public OutlierResult autorun(Database database) {
    Clustering<?> c = clustering.autorun(database);
    DBIDs ids = database.getRelation(TypeUtil.DBID).getDBIDs();

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    DoubleMinMax mm = new DoubleMinMax();

    List<? extends Cluster<?>> clusters = c.getAllClusters();
    for(Cluster<?> cluster : clusters) {
      if(cluster.isNoise()) {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, 1.);
        }
        mm.put(1.);
      }
      else {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, 0.);
        }
        mm.put(0.);
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("Noise as outliers", ids, scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(mm.getMin(), mm.getMax(), 0., 1., 0.);
    return new OutlierResult(scoreMeta, scoreResult);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return clustering.getInputTypeRestriction();
  }

  /**
   * Parameterizer.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Clustering algorithm to run.
     */
    ClusteringAlgorithm<?> clustering;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<ClusteringAlgorithm<?>>(Algorithm.Utils.ALGORITHM_ID, ClusteringAlgorithm.class) //
          .grab(config, x -> clustering = x);
    }

    @Override
    public NoiseAsOutliers make() {
      return new NoiseAsOutliers(clustering);
    }
  }
}
