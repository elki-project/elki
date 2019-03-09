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
package elki.algorithm.clustering.trivial;

import elki.data.ClassLabel;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.NoSupportedDataTypeException;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.result.Metadata;
import elki.utilities.Priority;
/**
 * Trivial class that will try to cluster by label, and fall back to an
 * "all-in-one" clustering.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Priority(Priority.SUPPLEMENTARY - 50)
public class ByLabelOrAllInOneClustering extends ByLabelClustering {
  /**
   * Constructor.
   */
  public ByLabelOrAllInOneClustering() {
    super();
  }

  @Override
  public Clustering<Model> run(Database database) {
    // Prefer a true class label
    try {
      Relation<ClassLabel> relation = database.getRelation(TypeUtil.CLASSLABEL);
      return run(relation);
    }
    catch(NoSupportedDataTypeException e) {
      // Ignore.
    }
    try {
      Relation<ClassLabel> relation = database.getRelation(TypeUtil.GUESSED_LABEL);
      return run(relation);
    }
    catch(NoSupportedDataTypeException e) {
      // Ignore.
    }
    final DBIDs ids = database.getRelation(TypeUtil.ANY).getDBIDs();
    Clustering<Model> result = new ReferenceClustering<>();
    Metadata.of(result).setLongName("All-in-one Trivial Clustering");
    Cluster<Model> c = new Cluster<Model>(ids, ClusterModel.CLUSTER);
    result.addToplevelCluster(c);
    return result;
  }
}
