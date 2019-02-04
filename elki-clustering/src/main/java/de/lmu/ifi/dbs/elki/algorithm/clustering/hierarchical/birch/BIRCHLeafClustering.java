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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.birch;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

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
public class BIRCHLeafClustering extends AbstractAlgorithm<Clustering<MeanModel>> implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BIRCHLeafClustering.class);

  /**
   * CFTree factory.
   */
  CFTree.Factory cffactory;

  /**
   * Constructor.
   *
   * @param cffactory CFTree Factory
   */
  public BIRCHLeafClustering(CFTree.Factory cffactory) {
    super();
    this.cffactory = cffactory;
  }

  /**
   * Run the clustering algorithm.
   *
   * @param relation Input data
   * @return Clustering
   */
  public Clustering<MeanModel> run(Relation<NumberVector> relation) {
    final int dim = RelationUtil.dimensionality(relation);
    CFTree tree = cffactory.newTree(relation.getDBIDs(), relation);
    // The CFTree does not store points. We have to reassign them (and the
    // quality is better than if we used the initial assignment, because centers
    // move in particular in the beginning, so we always had many outliers.
    Map<ClusteringFeature, ModifiableDBIDs> idmap = new HashMap<ClusteringFeature, ModifiableDBIDs>(tree.leaves);
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      ClusteringFeature cf = tree.findLeaf(relation.get(iter));
      ModifiableDBIDs ids = idmap.get(cf);
      if(ids == null) {
        idmap.put(cf, ids = DBIDUtil.newArray(cf.n));
      }
      ids.add(iter);
    }
    Clustering<MeanModel> result = new Clustering<>("BIRCH-leaves", "BIRCH leaves");
    for(Map.Entry<ClusteringFeature, ModifiableDBIDs> ent : idmap.entrySet()) {
      ClusteringFeature leaf = ent.getKey();
      double[] center = new double[dim];
      for(int i = 0; i < dim; i++) {
        center[i] = leaf.centroid(i);
      }
      result.addToplevelCluster(new Cluster<>(ent.getValue(), new MeanModel(center)));
    }
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * CFTree factory.
     */
    CFTree.Factory cffactory;

    @Override
    protected void makeOptions(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
    }

    @Override
    protected BIRCHLeafClustering makeInstance() {
      return new BIRCHLeafClustering(cffactory);
    }
  }
}
