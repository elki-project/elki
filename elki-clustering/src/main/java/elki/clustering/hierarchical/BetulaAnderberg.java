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
import java.util.Iterator;
import java.util.ListIterator;

import elki.Algorithm;
import elki.clustering.hierarchical.linkage.Linkage;
import elki.clustering.hierarchical.linkage.WardLinkage;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.index.tree.betula.CFTree;
import elki.index.tree.betula.distance.CFDistance;
import elki.index.tree.betula.distance.VarianceIncreaseDistance;
import elki.index.tree.betula.features.ClusterFeature;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * @author Andreas Lang
 *
 */
public class BetulaAnderberg implements HierarchicalClusteringAlgorithm {
    /**
     * Class logger
     */
    private static final Logging LOG = Logging.getLogger(BetulaAnderberg.class);

    /**
     * Distance function used.
     */
    protected CFDistance distance;

    /**
     * Current linkage method in use.
     */
    protected Linkage linkage = WardLinkage.STATIC;

    /**
     * CFTree factory.
     */
    CFTree.Factory<?> cffactory;

    /**
     * Ignore weight
     */
    boolean ignoreWeight;

    /**
     * Constructor.
     *
     * @param distance Distance function to use
     * @param linkage Linkage method
     * @param cffactory
     * @param ignoreWeight
     * @param
     * @param
     */
    public BetulaAnderberg(CFDistance distance, Linkage linkage, CFTree.Factory<?> cffactory, boolean ignoreWeight) {
        super();
        this.distance = distance;
        this.linkage = linkage;
        this.cffactory = cffactory;
        this.ignoreWeight = ignoreWeight;
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
        while(lit.hasNext()) {
            ClusterFeature cf = lit.next();
            idList.add(tree.getDBIDs(cf));
            dists[i] = cf.variance() / cf.getWeight();
            i++;
        }

        ClusterDistanceMatrix mat = initializeDistanceMatrix(cfs, distance);
        ClusterMergeHistoryBuilder cmhb = initializeHistoryBuilder(idList, relation.size(), dists, mat.clustermap, ignoreWeight);
        ClusterMergeHistory res = new Anderberg.Instance(linkage).run(mat, cmhb);
        if(ignoreWeight) {
            fixCount(cmhb);
        }
        return res;

    }

    protected static void fixCount(ClusterMergeHistoryBuilder cmhb) {

        int m = cmhb.mergecount;
        int n = m + 1;
        for(int i = 0; i < m; i++) {
            int a = cmhb.merges[i << 1];
            int b = cmhb.merges[(i << 1) + 1];
            cmhb.setSize(n + i, cmhb.getSize(a) + cmhb.getSize(b));
        }
    }

    protected static ClusterMergeHistoryBuilder initializeHistoryBuilder(ArrayList<DBIDs> idList, int n, double[] dists, int[] clustermap, boolean ignoreWeight) {
        ArrayModifiableDBIDs ids = DBIDUtil.newArray(n);
        ClusterMergeHistoryBuilder cmhb = new ClusterMergeHistoryBuilder(ids, n, true);
        Iterator<DBIDs> cluIter = idList.iterator();
        for(int i = 0, j = 0; i < idList.size(); i++) {
            DBIDIter iter = cluIter.next().iter();
            ids.add(iter);
            iter.advance();
            int clunum = j++;
            for(; iter.valid(); iter.advance(), j++) {
                ids.add(iter);
                clunum = cmhb.strictAdd(j, dists[i], clunum);
            }
            clustermap[i] = clunum;
            if(ignoreWeight) {
                if(clunum > j) {
                    cmhb.setSize(clunum, 1);
                }
            }
        }
        return cmhb;
    }

    /**
     * Initialize a distance matrix.
     *
     * @param cfs
     * @param df Distance function
     * @return cluster distance matrix
     */
    protected static ClusterDistanceMatrix initializeDistanceMatrix(ArrayList<? extends ClusterFeature> cfs, CFDistance df) {
        ClusterDistanceMatrix mat = new ClusterDistanceMatrix(cfs.size());
        final double[] matrix = mat.matrix;
        FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Distance matrix computation", matrix.length, LOG) : null;
        int pos = 0;
        for(int i = 1; i < cfs.size(); i++) {
            assert pos == ClusterDistanceMatrix.triangleSize(i);
            for(int j = 0; j < i; j++) {
                matrix[pos++] = df.squaredDistance(cfs.get(i), cfs.get(j));
            }
            if(prog != null) {
                prog.setProcessed(pos, LOG);
            }
        }
        // Avoid logging errors in case scratch space was too large:
        if(prog != null) {
            prog.setProcessed(matrix.length, LOG);
        }
        LOG.ensureCompleted(prog);
        return mat;
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
        protected Linkage linkage;

        /**
         * The distance function to use.
         */
        protected CFDistance distance;

        /**
         * CFTree factory.
         */
        CFTree.Factory<?> cffactory;

        /**
         * Ignore weight
         */
        boolean ignoreWeight = false;

        @Override
        public void configure(Parameterization config) {
            cffactory = config.tryInstantiate(CFTree.Factory.class);
            new Flag(IGNORE_WEIGHT_ID).grab(config, x -> ignoreWeight = x);
            new ObjectParameter<Linkage>(AGNES.Par.LINKAGE_ID, Linkage.class) //
                    .setDefaultValue(WardLinkage.class) //
                    .grab(config, x -> linkage = x);
            new ObjectParameter<CFDistance>(Algorithm.Utils.DISTANCE_FUNCTION_ID, CFDistance.class, VarianceIncreaseDistance.class) //
                    .grab(config, x -> distance = x);
        }

        @Override
        public BetulaAnderberg make() {
            return new BetulaAnderberg(distance, linkage, cffactory, ignoreWeight);
        }
    }

}
