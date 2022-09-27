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
package elki.clustering;

import static elki.math.linearalgebra.VMath.diagonal;

import java.util.Map;

import elki.clustering.em.BetulaGMM;
import elki.clustering.kmeans.BetulaLloydKMeans;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.EMModel;
import elki.data.model.MeanModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.CFTree.LeafIterator;
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.logging.statistics.DoubleStatistic;
import elki.result.Metadata;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * BETULA-based clustering algorithm that simply treats the leafs of the CFTree
 * as clusters. As this usually are smaller parts than actual clusters, we call
 * this a preclustering, as it is primarily useful as a data simplification
 * prior to, e.g., clustering. For actual clustering methods based on the
 * leaves, please use {@link BetulaGMM} and {@link BetulaLloydKMeans}.
 * <p>
 * References:
 * <p>
 * Andreas Lang and Erich Schubert<br>
 * BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees<br>
 * Information Systems
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @depend - - - CFTree
 */
@Priority(Priority.SUPPLEMENTARY)
@Reference(authors = "Andreas Lang and Erich Schubert", //
    title = "BETULA: Fast Clustering of Large Data with Improved BIRCH CF-Trees", //
    booktitle = "Information Systems", //
    url = "https://doi.org/10.1016/j.is.2021.101918", //
    bibkey = "DBLP:journals/is/LangS22")
public class BetulaLeafPreClustering implements ClusteringAlgorithm<Clustering<MeanModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(BetulaLeafPreClustering.class);

  /**
   * CFTree factory.
   */
  CFTree.Factory<?> cffactory;

  /**
   * Store ids
   */
  boolean storeIds = false;

  /**
   * Constructor.
   *
   * @param cffactory CFTree Factory
   */
  public BetulaLeafPreClustering(CFTree.Factory<?> cffactory, boolean storeIds) {
    super();
    this.cffactory = cffactory;
    this.storeIds = storeIds;
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
    CFTree<?> tree = cffactory.newTree(relation.getDBIDs(), relation, storeIds);
    Map<ClusterFeature, DBIDs> idmap = new Reference2ObjectOpenHashMap<>(tree.numLeaves());
    if(storeIds) {
      for(LeafIterator<?> it = tree.leafIterator(); it.valid(); it.advance()) {
        idmap.put(it.get(), tree.getDBIDs(it.get()));
      }
    }
    else {
      // The CFTree did not store point ids.
      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        ClusterFeature cf = tree.findLeaf(relation.get(iter));
        ModifiableDBIDs ids = (ModifiableDBIDs) idmap.get(cf);
        if(ids == null) {
          idmap.put(cf, ids = DBIDUtil.newArray(cf.getWeight()));
        }
        ids.add(iter);
      }
    }
    Clustering<MeanModel> result = new Clustering<>();
    for(Map.Entry<ClusterFeature, DBIDs> ent : idmap.entrySet()) {
      ClusterFeature leaf = ent.getKey();
      double[] center = leaf.toArray();
      double[] variance = new double[dim];
      for(int i = 0; i < dim; i++) {
        variance[i] = leaf.variance(i);
      }
      result.addToplevelCluster(new Cluster<>(ent.getValue(), new EMModel(center, diagonal(variance))));
    }
    DoubleStatistic varstat = new DoubleStatistic(this.getClass().getName() + ".varsum");
    double varsum = 0.;
    for(LeafIterator<?> iter = tree.leafIterator(); iter.valid(); iter.advance()) {
      varsum += iter.get().sumdev();
    }
    LOG.statistics(varstat.setDouble(varsum));
    Metadata.of(result).setLongName("BETULA Leaf Nodes");
    return result;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option to store ids rather than reassigning.
     */
    public static final OptionID STORE_IDS_ID = new OptionID("betula.storeids", "Store IDs when building the tree, and use when assigning to leaves.");

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    /**
     * Store ids
     */
    boolean storeIds = false;

    @Override
    public void configure(Parameterization config) {
      cffactory = config.tryInstantiate(CFTree.Factory.class);
      new Flag(STORE_IDS_ID).grab(config, x -> storeIds = x);
    }

    @Override
    public BetulaLeafPreClustering make() {
      return new BetulaLeafPreClustering(cffactory, storeIds);
    }
  }
}
