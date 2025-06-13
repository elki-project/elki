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

package elki.clustering.hierarchical;

import java.util.ArrayList;
import java.util.ListIterator;

import elki.clustering.hierarchical.linkage.GeometricLinkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CFDistance;
import elki.index.tree.betula.features.ClusterFeature;

/**
 * NNchain clustering algorithm with linear memory, for particular linkages
 * (that can be aggregated) and numerical vector data only.
 * 
 * Based on Cluster Features
 * 
 * <p>
 * Reference:
 * <p>
 * F. Murtagh<br>
 * Multidimensional Clustering Algorithms,1985<br>
 * http://www.multiresolutions.com/strule/MClA/
 *
 * @author Andreas Lang
 *
 */
public class BetulaLinearMemoryNNChain implements HierarchicalClusteringAlgorithm {

    /**
     * Distance function used.
     */
    protected CFDistance distance;

    /**
     * Current linkage method in use.
     */
    protected GeometricLinkage linkage = WardLinkage.STATIC;

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    /**
     * Constructor.
     *
     * @param distance Distance function to use
     * @param linkage Linkage method
     * @param
     * @param
     */
    public BetulaLinearMemoryNNChain( GeometricLinkage linkage, CFTree.Factory<?> cffactory) {
        this.linkage = linkage;
        this.cffactory = cffactory;
    }

    /**
     * Run the algorithm
     *
     * @param relation Relation
     * @return Clustering hierarchy
     */
    public ClusterMergeHistory run(Relation<NumberVector> relation) {
        final ArrayDBIDs ids = DBIDUtil.ensureArray(relation.getDBIDs());
        CFTree<?> tree = cffactory.newTree(ids, relation, true);
        ArrayList<? extends ClusterFeature> cfs = tree.getLeaves();

        ArrayList<DBIDs> idList = new ArrayList<>();
        double[] dists = new double[cfs.size()];
        ListIterator<? extends ClusterFeature> lit = cfs.listIterator();
        int i = 0;

        double[][] clusters = new double[cfs.size()][];

        while(lit.hasNext()) {
            ClusterFeature cf = lit.next();
            idList.add(tree.getDBIDs(cf));
            dists[i] = cf.variance() / cf.getWeight();
            clusters[i] = cf.toArray();
            i++;
        }

        int[] clustermap = new int[cfs.size()];
        ClusterMergeHistoryBuilder cmhb = BetulaAnderberg.initializeHistoryBuilder(idList, relation.size(), dists, clustermap, false);
        cmhb.optimizeOrder(); // TODO avoid?
        new LinearMemoryNNChain.Instance<NumberVector>(linkage).nnChainCore(clusters, clustermap, cmhb);

        cmhb.optimizeOrder();
        ClusterMergeHistory res = cmhb.complete();
        return res;
    }

    @Override
    public TypeInformation[] getInputTypeRestriction() {
        return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
    }

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     */
    public static class Par implements Parameterizer {
        /**
         * Ignore cluster weights (naive approach)
         */
        public static final OptionID IGNORE_WEIGHT_ID = new OptionID("betulaAnderberg.naive", "Treat leaves as single points, not weighted points.");

        /**
         * Current linkage in use.
         */
        protected GeometricLinkage linkage;

        /**
         * CFTree factory.
         */
        CFTree.Factory<?> cffactory;

        @Override
        public void configure(Parameterization config) {
            cffactory = config.tryInstantiate(CFTree.Factory.class);
            new ObjectParameter<GeometricLinkage>(AGNES.Par.LINKAGE_ID, GeometricLinkage.class) //
                    .setDefaultValue(WardLinkage.class) //
                    .grab(config, x -> linkage = x);
        }

        @Override
        public BetulaLinearMemoryNNChain make() {
            return new BetulaLinearMemoryNNChain(linkage, cffactory);
        }
    }

}
