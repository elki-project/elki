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
package elki.clustering.trivial;

import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.relation.Relation;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * Trivial pseudo-clustering that just considers all points to be one big
 * cluster.
 * <p>
 * Useful for evaluation and testing.
 *
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Trivial all-in-one clustering")
@Description("Returns a 'tivial' clustering which just considers all points to be one big cluster.")
@Priority(Priority.SUPPLEMENTARY - 50)
public class TrivialAllInOne implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * Constructor.
   */
  public TrivialAllInOne() {
    super();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  /**
   * Perform trivial clustering.
   * 
   * @param relation Data to cluster
   * @return Result
   */
  public Clustering<Model> run(Relation<?> relation) {
    Clustering<Model> result = new ReferenceClustering<>();
    Metadata.of(result).setLongName("All-in-one Trivial Clustering");
    result.addToplevelCluster(new Cluster<Model>(relation.getDBIDs(), ClusterModel.CLUSTER));
    return result;
  }
}
