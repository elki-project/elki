/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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

import elki.clustering.dbscan.GeneralizedDBSCAN;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.CoreObjectsModel;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.outlier.OutlierAlgorithm;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Outlier detection algorithm using DBSCAN Clustering.
 * <p>
 * The outlierness score is attributed to each point according to the set to
 * which it is linked: global outliers (noise points), local outliers (border
 * points), and inliers (core points).
 *
 * @author Braulio V.S. Vinces
 * @since 0.7.5
 * 
 * @has - - - GeneralizedDBSCAN
 */
@Title("DBSCAN Outlier Detection: Outlier Detection based on the Generalized DBSCAN clustering")
public class DBSCANOutlierDetection implements OutlierAlgorithm {
  /**
   * Inner algorithm.
   */
  private GeneralizedDBSCAN clusterer;

  /**
   * Constructor with an existing Genearalized DBSCAN clustering algorithm.
   * 
   * @param clusterer Generalized DBSCAN clustering algorithm to use.
   */
  public DBSCANOutlierDetection(GeneralizedDBSCAN clusterer) {
    this.clusterer = clusterer;
  }

  /**
   * Runs the algorithm in the timed evaluation part.
   * 
   * @param relation Relation to process
   * @return Outlier result
   */
  public OutlierResult run(Database db, Relation<? extends NumberVector> relation) {
    // Run the primary algorithm
    Clustering<?> c = clusterer.autorun(db);

    WritableDoubleDataStore scores = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB);
    final double noise = 1., border = .5, core = 0.;

    // Iterate over all clusters:
    List<?> topLevelClusters = c.getToplevelClusters();
    for(Object obj : topLevelClusters) {
      Cluster<?> cluster = (Cluster<?>) obj;
      // Process objects in the cluster
      if(cluster.isNoise()) {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, noise);
        }
        continue;
      }
      // GeneralizedDBSCAN with core enabled
      if(cluster.getModel() instanceof CoreObjectsModel) {
        DBIDs coreObjects = ((CoreObjectsModel) cluster.getModel()).getCoreObjects();
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, border); // core points will be overwritten below:
        }
        for(DBIDIter iter = coreObjects.iter(); iter.valid(); iter.advance()) {
          scores.put(iter, core);
        }
      }
      else {
        for(DBIDIter iter = cluster.getIDs().iter(); iter.valid(); iter.advance()) {
          scores.put(iter, core);
        }
      }
    }

    // Build result representation.
    DoubleRelation scoreResult = new MaterializedDoubleRelation("DBSCAN outlier score", relation.getDBIDs(), scores);
    OutlierScoreMeta scoreMeta = new BasicOutlierScoreMeta(0., 1., 0., 1., 0.);
    OutlierResult result = new OutlierResult(scoreMeta, scoreResult);
    Metadata.hierarchyOf(result).addChild(c);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return clusterer.getInputTypeRestriction();
  }

  /**
   * Parameterizer.
   *
   * @author Braulio V.S. Vinces
   */
  public static class Par implements Parameterizer {
    /**
     * Generalized DBSCAN clustering algorithm to run.
     */
    protected GeneralizedDBSCAN clusterer;

    @Override
    public void configure(Parameterization config) {
      clusterer = config.tryInstantiate(GeneralizedDBSCAN.class);
    }

    @Override
    public DBSCANOutlierDetection make() {
      return new DBSCANOutlierDetection(clusterer);
    }
  }
}
