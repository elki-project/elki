/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2020
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

package elki.clustering.hierarchical.betula.initialization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import elki.clustering.hierarchical.betula.CFInterface;
import elki.clustering.hierarchical.betula.CFNode;
import elki.clustering.hierarchical.betula.CFTree;
import elki.math.linearalgebra.VMath;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import net.jafama.FastMath;

/**
 * Initialize K-means by choosing k distant existing subtrees.
 *
 * @author Andreas Lang
 */
public class CFKppTree extends AbstractCFKMeansInitialization {

    int maxdepth = -1;

    CFIDistance dist;

    boolean first_var;

    public CFKppTree(RandomFactory rf, CFIDistance dist, int maxdepth, boolean first_var) {
        super(rf);
        this.dist = dist;
        this.maxdepth = maxdepth;
        this.first_var = first_var;
    }

    @Override
    public double[][] chooseInitialMeans(CFTree<CFInterface> tree, CFInterface[] cfs, int k) {
        if(maxdepth == 0) {
            maxdepth = FastMath.log2(k) / FastMath.log2(tree.getRoot().getChilds().length) + 1;
        }
        if(tree.getLeaves() < k) {
            throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + tree.getLeaves() + " < k objects.");
        }
        Random rnd = rf.getSingleThreadedRandom();
        CFNode<CFInterface> current;
        CFInterface next;
        CFInterface[] ccs = new CFInterface[k];
        int first = rnd.nextInt(cfs.length);
        int d = cfs[first].getDimensionality();
        if(first_var) {
            ccs[0] = tree.getRoot();
            ccs[0] = chooseNextNode(cfs, ccs, 1, rnd);
        }
        else {
            ccs[0] = cfs[first];
        }
        for(int m = 1; m < k; m++) {
            next = tree.getRoot();
            int depth = maxdepth;
            while(next instanceof CFNode && depth != 0) {
                depth--;
                current = (CFNode<CFInterface>) next;
                next = chooseNextNode(current.getChilds(), ccs, m, rnd);
            }
            ccs[m] = next;
        }
        double[][] means = new double[k][];
        double[] mean;
        for(int i = 0; i < k; i++) {
            mean = new double[d];
            for(int j = 0; j < cfs[0].getDimensionality(); j++) {
                mean[j] = ccs[i].centroid(j);
            }
            means[i] = mean;

        }
        return means;
    }

    CFInterface chooseNextNode(CFInterface[] childs, CFInterface[] ccs, int m, Random rnd) {
        double weightsum = 0;
        double[] weights = new double[childs.length];
        Arrays.fill(weights, Double.POSITIVE_INFINITY);
        for(int i = 0; i < childs.length && childs[i] != null; i++) {
            double weight;
            for(int j = 0; j < m; j++) {
                weight = dist.squaredDistance(ccs[j], childs[i]);
                if(weight < weights[i]) {
                    weights[i] = weight;
                }
            }
            weightsum += weights[i];
        }

        while(true) {
            double r = rnd.nextDouble() * weightsum;
            int i = 0;
            while(i < childs.length && childs[i] != null) {
                if((r -= weights[i]) <= 0) {
                    return childs[i];
                }
                i++;
            }
            weightsum -= r; // Decrease
        }
    }

    /**
     * Parameterization class.
     * 
     * @author Andreas Lang
     */
    public static class Par extends AbstractCFKMeansInitialization.Par {

        public static final OptionID DEPTH_ID = new OptionID("kmpp.depth", //
                "maximum depth for intitialization");

        /**
         * k Means distance.
         */
        public static final OptionID KMPlusPlus_ID = new OptionID("kmeans.distance", "Distance to use for kmeans++ criterion");

        public static final OptionID KMPFirst_ID = new OptionID("kmpp.first_var", "Chooose first dependent on Var");

        int depth = -1;

        CFIDistance dist;

        boolean first_var = false;

        @Override
        public void configure(Parameterization config) {
            new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
            new ObjectParameter<CFIDistance>(KMPlusPlus_ID, CFIDistance.class, VarDist.class)//
                    .grab(config, x -> dist = x);
            new Flag(KMPFirst_ID).grab(config, x -> first_var = x);
            new IntParameter(DEPTH_ID, -1)//
                    .grab(config, x -> depth = x);
        }

        @Override
        public CFKppTree make() {
            return new CFKppTree(rnd, dist, depth, first_var);
        }
    }
}
