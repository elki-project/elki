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
package elki.clustering.trivial;

import elki.algorithm.AbstractAlgorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * Trivial pseudo-clustering that just considers all points to be noise.
 * 
 * Useful for evaluation and testing.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Trivial all-noise clustering")
@Description("Returns a 'trivial' clustering which just considers all points as noise points.")
@Priority(Priority.SUPPLEMENTARY - 50)
public class TrivialAllNoise extends AbstractAlgorithm<Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(TrivialAllNoise.class);

  /**
   * Constructor.
   */
  public TrivialAllNoise() {
    super();
  }

  public Clustering<Model> run(Relation<?> relation) {
    final DBIDs ids = relation.getDBIDs();
    Clustering<Model> result = new ReferenceClustering<>();
    Metadata.of(result).setLongName("All-in-noise Trivial Clustering");
    Cluster<Model> c = new Cluster<Model>(ids, true, ClusterModel.CLUSTER);
    result.addToplevelCluster(c);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.ANY);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
