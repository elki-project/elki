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
import elki.database.ids.DBIDArrayIter;
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

/**
 * @author Miriama Janosova and Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class LabeledPAMFix<O> extends LabeledPAM<O> {

    /**
     * x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(LabeledPAMFix.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = LabeledPAMFix.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public LabeledPAMFix(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
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

        private int applyFixesIters;

        /**
         * Constructor.
         *
         * @param distQ Distance query
         * @param ids IDs to process
         * @param assignment Cluster assignment
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
            super(distQ, ids, assignment, labelsMaps, clusterLabel, numberOfLabels);
            this.applyFixesIters = 0;
        }

        @Override
        protected double clusterData(int maxiter, int k, double tc, DBIDArrayMIter m, double[] pcost, IndefiniteProgress prog){
            tc += applyFixes(m, k);
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
                    // Compare object to its own medoid.
                    if(DBIDUtil.equal(m.seek(assignment.intValue(h) & 0x7FFF), h)) {
                        continue; // This is a medoid.
                    }
                    // Initialize with medoid removal cost:
                    System.arraycopy(pcost, 0, cost, 0, pcost.length);
                    // Compare object to its own medoid.
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
                    tc += applyFixes( m, k);
                    updatePriorCost(pcost, m);
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

        private double applyFixes(DBIDArrayIter miter, int k) {
          double costImprovement = 0d;
          while(true) {
            applyFixesIters++;
            for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
              int n = assignment.intValue(j) & 0x7FFF;
              int s = assignment.intValue(j) >> 16;
              assert second.doubleValue(j) > 0 || s == n;
              if(s == n || !isValidObjSndMedPair(j, s)) {
                // second closest was coloured at some point after j was
                // assigned
                // there, thus we need to look for a new one
                s = updateSecondNearest(j, miter, s, Double.POSITIVE_INFINITY, n);
                assignment.putInt(j, n | (s << 16));
                assert second.doubleValue(j) > 0 || s == n;
              }
              assert second.doubleValue(j) > 0 || s == n;
              // cost of removing the medoid + reassigning objects to
              // second
            }
            double[][] acc = findFixes(k);
            int col = -1;
            int clu = -1;
            double min = 0.;
            for(int i = 0; i < acc.length; i++) {
              for(int j = 0; j < acc[i].length; j++) {
                if(acc[i][j] < min) {
                  min = acc[i][j];
                  col = j;
                  clu = i;
                }
              }
            }
            if(min == 0.) {
              if(LOG.isStatistics()) {
                LOG.statistics(new LongStatistic(KEY + ".apply-fixes-iters", applyFixesIters));
                LOG.statistics(new DoubleStatistic(KEY + ".apply-fixes-cost-improvement", costImprovement));
              }
              this.applyFixesIters = 0;
              return 0d + costImprovement;
            }
            costImprovement += min;
            updateAssignFindFixes(miter, clu, col);
          }
        }

        private void updateAssignFindFixes(DBIDArrayIter miter, int m, int newLbl) {
          // The new medoid itself.
          clusterLabels[m] = newLbl;
          DBIDVar h = DBIDUtil.newVar();
          h.set(miter.seek(m));

          for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
            // new point is already dealt with
            if(DBIDUtil.equal(h, j)) {
              continue;
            }
            int objColor = pointLabelMap.intValue(j);
            final double distcur = nearest.doubleValue(j);
            final double distsec = second.doubleValue(j);

            final int pn = assignment.intValue(j) & 0x7FFF;
            final int ps = assignment.intValue(j) >> 16;
            final boolean sValid = ps != pn;
            assert distsec > 0. || !sValid;

            // if the switched med is my second and colour is OK, I reassign
            // myself
            // always look for second
            if(ps != m) {
              continue;
            }
            if(objColor == newLbl && distsec < distcur) {
              countLabelledPointsInCluster[pn] -= 1;
              countLabelledPointsInCluster[ps] += 1;
              nearest.putDouble(j, distsec);
              final int newSecond = updateSecondNearest(j, miter, pn, distcur, ps);
              assignment.putInt(j, ps | (newSecond << 16));
            }
            else {
              final int newSecond = updateSecondNearest(j, miter, pn, Double.POSITIVE_INFINITY, pn);
              assignment.putInt(j, pn | (newSecond << 16));
            }
          }
        }

        private double[][] findFixes(int k) {
          // for h labelled it can be reduced to 1 x k
          double[][] acc = new double[k][numberOfLabels + 1];
          // Compute costs of reassigning other objects o:
          for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
            final int jColor = this.pointLabelMap.intValue(j);
            final double dn = nearest.doubleValue(j);
            final double ds = second.doubleValue(j);
            int n = assignment.intValue(j) & 0x7FFF;
            int s = assignment.intValue(j) >> 16;
            boolean sValid = (n != s);

            if(ds < dn && sValid) {
              // get the color of x_o + update the
              // correct_label_accumulator
              double benefit = ds - dn;
              acc[s][jColor] += benefit;
            }
          }
          return acc;
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
        public LabeledPAMFix<O> make() {
            return new LabeledPAMFix<>(distance, k, maxiter, initializer);
        }
    }
}
