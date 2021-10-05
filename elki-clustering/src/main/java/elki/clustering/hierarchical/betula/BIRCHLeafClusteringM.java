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
package elki.clustering.hierarchical.betula;

import static elki.math.linearalgebra.VMath.diagonal;

import java.util.Map;

import elki.clustering.ClusteringAlgorithm;
import elki.clustering.hierarchical.betula.features.ClusterFeature;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * BIRCH-based clustering algorithm that simply treats the leafs of the CFTree
 * as clusters.
 * <p>
 * References:
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: An Efficient Data Clustering Method for Very Large Databases
 * Proc. 1996 ACM SIGMOD International Conference on Management of Data
 * <p>
 * T. Zhang, R. Ramakrishnan, M. Livny<br>
 * BIRCH: A New Data Clustering Algorithm and Its Applications
 * Data. Min. Knowl. Discovery
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @depend - - - CFTree
 */
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: An Efficient Data Clustering Method for Very Large Databases", //
    booktitle = "Proc. 1996 ACM SIGMOD International Conference on Management of Data", //
    url = "https://doi.org/10.1145/233269.233324", //
    bibkey = "DBLP:conf/sigmod/ZhangRL96")
@Reference(authors = "T. Zhang, R. Ramakrishnan, M. Livny", //
    title = "BIRCH: A New Data Clustering Algorithm and Its Applications", //
    booktitle = "Data Min. Knowl. Discovery", //
    url = "https://doi.org/10.1023/A:1009783824328", //
    bibkey = "DBLP:journals/datamine/ZhangRL97")
public class BIRCHLeafClusteringM implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * CFTree factory.
   */
  CFTree.Factory<?> cffactory;

  /**
   * Constructor.
   *
   * @param cffactory CFTree Factory
   */
  public BIRCHLeafClusteringM(CFTree.Factory<?> cffactory) {
    super();
    this.cffactory = cffactory;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the clustering algorithm.
   *
   * @param relation Input data
   * @return Clustering
   */
  public Clustering<MeanModel> run(Relation<NumberVector> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    CFTree<?> tree = cffactory.newTree(relation.getDBIDs(), relation, true);
    Map<ClusterFeature, ArrayModifiableDBIDs> idmap = tree.idmap;
    Clustering<MeanModel> result = new Clustering<>();
    for(Map.Entry<ClusterFeature, ArrayModifiableDBIDs> ent : idmap.entrySet()) {
      ClusterFeature leaf = ent.getKey();
      double[] center = new double[dim];
      double[] variance = new double[dim];
      for(int i = 0; i < dim; i++) {
        center[i] = leaf.centroid(i);
        variance[i] = leaf.variance(i);
      }
      result.addToplevelCluster(new Cluster<>(ent.getValue(), new EMModel(center, diagonal(variance))));
    }
    Metadata.of(result).setLongName("BIRCH Clustering");
    return result;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
    }

    @Override
    public BIRCHLeafClusteringM make() {
      return new BIRCHLeafClusteringM(cffactory);
    }
  }
}
