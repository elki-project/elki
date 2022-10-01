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
package elki.clustering.kmedoids.initialization;

import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.KMedoidsClustering;
import elki.clustering.kmedoids.PAM;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.ChainedParameterization;
import elki.utilities.optionhandling.parameterization.ListParameterization;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Initialize k-medoids with k-medoids, for methods such as PAMSIL.<br>
 * This could also be used to initialize, e.g., PAM with CLARA.
 * <p>
 * TODO: this could be made more useful by adding a sampling option.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
@Title("K-medoids Initialization by K-medoids")
@Description("Initialize k-medoids with k-medoids, usually a less expensive variant.")
public class KMedoidsKMedoidsInitialization<O> implements KMedoidsInitialization<O> {
  /**
   * Inner k-medoids clustering to use.
   */
  private KMedoidsClustering<O> inner;

  /**
   * Constructor.
   *
   * @param inner Inner clustering
   */
  public KMedoidsKMedoidsInitialization(KMedoidsClustering<O> inner) {
    super();
    this.inner = inner;
  }

  @Override
  public DBIDs chooseInitialMedoids(int k, DBIDs ids, DistanceQuery<? super O> distance) {
    // TODO: add subsampling option!
    @SuppressWarnings("unchecked")
    Relation<O> rel = (Relation<O>) distance.getRelation();
    Clustering<MedoidModel> cl = inner.run(rel, k, distance);
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(k);
    for(Cluster<MedoidModel> c : cl.getAllClusters()) {
      medoids.add(c.getModel().getMedoid());
    }
    return medoids;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @param <O> Object type
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Option ID for the nested k-medoids clustering algorithm.
     */
    public static final OptionID INNER_ID = new OptionID("kmedoids.inner", "Nested k-medoids algorithm for initialization.");

    /**
     * Clustering algorithm
     */
    private KMedoidsClustering<O> inner;

    @Override
    public void configure(Parameterization config) {
      ObjectParameter<KMedoidsClustering<O>> innerP = new ObjectParameter<>(INNER_ID, KMedoidsClustering.class, PAM.class);
      if(config.grab(innerP)) {
        ListParameterization innerParameters = new ListParameterization();
        // Hack: hide the k and distance parameter by predefining them:
        innerParameters.addParameter(KMeans.K_ID, 13);
        innerParameters.addParameter(KMeans.DISTANCE_FUNCTION_ID, SquaredEuclideanDistance.class);

        ChainedParameterization combinedConfig = new ChainedParameterization(innerParameters, config);
        combinedConfig.errorsTo(config);
        inner = innerP.instantiateClass(combinedConfig);
      }
    }

    @Override
    public KMedoidsKMedoidsInitialization<O> make() {
      return new KMedoidsKMedoidsInitialization<>(inner);
    }
  }
}
