/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2024
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
package elki.clustering.kmedoids;

import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDVar;
import elki.database.ids.DBIDs;
import elki.database.query.distance.DistanceQuery;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.math.linearalgebra.VMath;

import java.util.Arrays;

/**
 * 
 * Alternative implementation of the labeled PAM algorithm that tries to label medoids quicker.
 * 
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class LabeledPAMFixLazy<O> extends LabeledPAM<O> {

    /**
     * x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(LabeledPAMFixLazy.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = LabeledPAMFixLazy.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public LabeledPAMFixLazy(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
        super(distance, k, maxiter, initializer);
    }

    @Override
    Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels) {
      return new Instance(distQ, ids, assignment, labelsMaps, clusterLabel, noLabels);
    }

    /**
     * Instance for a single dataset.
     * <p>
     * Note: we experimented with not caching the distance to nearest and second
     * nearest, but only the assignments. The matrix lookup was more expensive,
     * so this is probably worth the 2*n doubles in storage.
     *
     * @author Andreas Lang
     * @author Miriama Janosova
     */
    protected static class Instance extends LabeledPAM.Instance {
        /**
         * Constructor.
         *
         * @param distQ Distance query
         * @param ids IDs to process
         * @param assignment Cluster assignment
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
            super(distQ, ids, assignment, labelsMaps, clusterLabel, numberOfLabels);
        }

        @Override
        protected double clusterData(int maxiter, int k, double tc, DBIDArrayMIter m, double[] pcost, IndefiniteProgress prog){
            updatePriorCost(pcost, m);
            DBIDVar lastswap = DBIDUtil.newVar();
            double[] cost = new double[k];
            int prevswaps = 0;
            iteration = 0;
            swaps = 0;
            while(iteration < maxiter || maxiter <= 0) {
                ++iteration;
                LOG.incrementProcessed(prog);
                // Iterate over all non-medoids:
                for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
                    this.potentialSwaps++;
                    // Check if we completed an entire round without swapping:
                    if(DBIDUtil.equal(h, lastswap)) {
                        break;
                    }
                    // do not exclude medoids for swaps
                    // Initialize with medoid removal cost:
                    System.arraycopy(pcost, 0, cost, 0, pcost.length);
                    // Compare object to its own medoid.
                    int hAssignment = assignment.intValue(h) & 0x7FFF;
                    if(DBIDUtil.equal(m.seek(hAssignment), h)) {
                      for(int i = 0; i < cost.length; i++) {
                        if(i == hAssignment) {
                          continue;
                        }
                        cost[i] = Double.POSITIVE_INFINITY;
                      }
                    }
                    // The cost we get back by making the non-medoid h medoid.
                    double[][] acc = new double[k][numberOfLabels + 1];
                    computeLabeledReassignmentCost(h, cost, acc);
                    int[] new_col = updateCosts(h, acc,cost,k);
                    int min = VMath.argmin(cost);
                    double bestcost = cost[min];
                    // if the improvement is rounded 0
                    if(!(bestcost < -1e-12 * tc)) {
                        continue;
                    }
                    ++swaps;
                    lastswap.set(h); // x_c .. so we make the swap
                    updateAssignment(m, h, min, new_col[min]);
                    updatePriorCost(pcost, m);
                    Arrays.fill(numberOfClustersWithLabel, 0);
                    for(int i = 0; i < k; i++) {
                      numberOfClustersWithLabel[clusterLabels[i]]++;
                    }
                    tc += bestcost;
                    assert tc >= 0;
                    if(LOG.isStatistics()) {
                        LOG.statistics(new DoubleStatistic(KEY + ".swap-" + swaps + ".cost", tc));
                        LOG.statistics(new LongStatistic(KEY + ".potential-swaps", potentialSwaps));
                    }
                }
                if(LOG.isStatistics()) {
                    LOG.statistics(new LongStatistic(KEY + ".iteration-" + iteration + ".swaps", swaps - prevswaps));
                }
                if(prevswaps == swaps) {
                    break; // Converged
                }
                prevswaps = swaps;
                if(LOG.isStatistics()) {
                    LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
                }
            }
          return tc;
        }
        

      }

    @Override
    protected Logging getLogger() {
        return LOG;
    }

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     */
    public static class Par<O> extends LabeledPAM.Par<O> {

        @Override
        public LabeledPAMFixLazy<O> make() {
            return new LabeledPAMFixLazy<>(distance, k, maxiter, initializer);
        }
    }
}
