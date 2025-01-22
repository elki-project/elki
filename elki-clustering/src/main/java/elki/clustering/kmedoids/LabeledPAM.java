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

import java.util.Arrays;
import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayDBIDs;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDArrayMIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
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
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class LabeledPAM<O> extends SemiSupervisedKMedoids<O> {
    // private int[] listOfLabels;

    /**
     * x
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(LabeledPAM.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = LabeledPAM.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public LabeledPAM(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
        super(distance, k, maxiter, initializer);
    }

    Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels){
      return new Instance(distQ, ids, assignment, labelsMaps, clusterLabel, noLabels);
    }

    /**
     * Instance for a single dataset.
     * <p>
     * Note: we experimented with not caching the distance to nearest and second
     * nearest, but only the assignments. The matrix lookup was more expensive,
     * so this is probably worth the 2*n doubles in storage.
     *
     * @author Miriama Janosova 
     * @author Andreas Lang
     */
    protected static class Instance extends SemiSupervisedKMedoids.Instance {
        protected final int[] countLabelledPointsInCluster;

        protected int potentialSwaps;

        protected int swaps;

        protected int iteration;

        // for each label we store the number of clusters coloured this way
        protected final int[] numberOfClustersWithLabel;

        /**
         * Constructor.
         *
         * @param distQ Distance query
         * @param ids IDs to process
         * @param assignment Cluster assignment
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
            super(distQ, ids, assignment, labelsMaps, clusterLabel, numberOfLabels);
            this.countLabelledPointsInCluster = new int[clusterLabel.length];
            this.potentialSwaps = 0;
            this.numberOfClustersWithLabel = new int[numberOfLabels+1];
            for(int i = 0; i < clusterLabel.length; i++) {
                numberOfClustersWithLabel[clusterLabel[i]] += 1;
            }
        }

        /**
         * Run the PAM optimization phase.
         *
         * @param medoids Medoids list
         * @param maxiter
         * @return final cost
         */
        @Override
        protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
            final int k = medoids.size();
            // Initial assignment to nearest medoids
            double tc = assignToNearestCluster(medoids);
            if(LOG.isStatistics()) {
                LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + 0 + ".cost", tc));
            }
            // Swap phase
            IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("LabelledPAM iteration", LOG) : null;
            // Compute costs of reassigning to the second closest medoid.
            DBIDArrayMIter m = medoids.iter();
            double[] pcost = new double[k];
            tc = clusterData(maxiter, k, tc, m, pcost, prog);
            LOG.setCompleted(prog);
            if(LOG.isStatistics()) {
                LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
                LOG.statistics(new LongStatistic(KEY + ".swaps", swaps));
                LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
            }
            // Cleanup
            for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
                assignment.putInt(it, assignment.intValue(it) & 0x7FFF);
            }
            for(int i = 0; i < clusterLabels.length; i++) {
              LOG.verbose(KEY +".cluster_label." + i + ": " + clusterLabels[i]);
            }
            return tc;
        }

        protected double clusterData(int maxiter, int k, double tc, DBIDArrayMIter m, double[] pcost, IndefiniteProgress prog){
            updatePriorCost(pcost, m);
            Arrays.fill(numberOfClustersWithLabel, 0);
            for(int i = 0; i < k; i++) {
              numberOfClustersWithLabel[clusterLabels[i]]++;
            }
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

        protected int[] updateCosts(DBIDRef h, double[][] acc, double[] loss, int k) {
          // update the cost with given accumulator -> based on the
          // cluster
          // color + add uncolored one
          int hLabel = pointLabelMap.intValue(h);
          int[] new_col = new int[k];
          for(int i = 0; i < loss.length; i++) {
            int clusterLabel = clusterLabels[i];
            // medoid has a colour, we know what has to be a colour
            // of a new cluster
            if(hLabel != 0) {
              if(hLabel != clusterLabel && !canUncolor(k, clusterLabel)) {
                loss[i] = Double.POSITIVE_INFINITY;
                new_col[i] = clusterLabel;
                continue;
              }
              loss[i] += acc[i][0] + acc[i][hLabel]; 
              new_col[i] = hLabel;
              continue;
            }
            // medoid does not have a color..
            // can we get rid of the original colour??
            if(canUncolor(k, clusterLabel)) {
              // this retrieves best color
              int minCol = VMath.argmin(acc[i], 1, acc[i].length);
              if(acc[i][minCol] < 0) {
                loss[i] += acc[i][minCol] + acc[i][0];
                new_col[i] = minCol;
              }
              else {
                loss[i] += acc[i][0]; // in most cases this is also bigger than 0 and will just increase the loss
                new_col[i] = 0;
              }
              continue;
            }
              // this retrieves best with the original color
              loss[i] += acc[i][clusterLabel] + acc[i][0];
              new_col[i] = clusterLabel;
          }
          // Find the best possible swap for each medoid:
          return new_col;
        }

        /**
         * Check if we can uncolor the cluster.
         * @param k number of clusters
         * @param color color of the cluster
         */
        protected boolean canUncolor(int k, int color) {
            // never uncolor when k = numberOfLabels
            if(k < numberOfLabels) {
                return false;
            }
            if(this.numberOfClustersWithLabel[color] > 1) {
                return true;
            }
            // is there some uncolored cluster? if yes, OK
            return numberOfClustersWithLabel[0] > 0;
        }

        protected void updatePriorCost(double[] pcost, DBIDArrayIter mIter) {
            Arrays.fill(pcost, 0);
            for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
                int n = assignment.intValue(j) & 0x7FFF;
                int s = assignment.intValue(j) >> 16;
                assert second.doubleValue(j) > 0 || s == n || distQ.distance(j, mIter.seek(s)) == nearest.doubleValue(j);
                // TODO check if this can be precomputed
                if(s == n || !isValidObjSndMedPair(j, s)) {
                    // second closest was coloured at some point after j was
                    // assigned
                    // there, thus we need to look for a new one
                    s = updateSecondNearest(j, mIter, s, Double.POSITIVE_INFINITY, n);
                    assignment.putInt(j, n | (s << 16));
                    assert second.doubleValue(j) > 0 || s == n;
                }
                assert second.doubleValue(j) > 0 || s == n || distQ.distance(j, mIter.seek(s)) == nearest.doubleValue(j);
                // cost of removing the medoid + reassigning objects to the
                // second_closest
                pcost[n] += second.doubleValue(j) - nearest.doubleValue(j);
            }
        }

        /**
         * Update the assignments
         *
         * @param miter Medoid iterator
         * @param h new Medoid
         * @param m Medoid index to replace
         * @param newLbl new label
         */
        protected void updateAssignment(DBIDArrayMIter miter, DBIDRef h, int m, int newLbl) {
            // update the cluster information
            miter.seek(m).setDBID(h);
            clusterLabels[m] = newLbl;
            // update the new medoid itself.
            final double hdist = nearest.putDouble(h, 0);
            final int oldM = assignment.intValue(h) & 0x7FFF;
            final int oldS = assignment.intValue(h) >> 16;
            if(oldM != m && (hdist < second.doubleValue(h) || oldM == oldS)) {
                // stores its own assignment -> second closest is the old
                // closest
                assignment.putInt(h, m | (oldM << 16));
                // saves the previously closest dist as second
                second.putDouble(h, hdist);
                if(pointLabelMap.intValue(h) != 0) {
                    countLabelledPointsInCluster[m] += 1;
                    countLabelledPointsInCluster[oldM] -= 1;
                }
            }
            // new medoid but same second closest
            else if(oldM != m) {
                assignment.putInt(h, m | (oldS << 16));
                if(pointLabelMap.intValue(h) != 0) {
                    countLabelledPointsInCluster[m] += 1;
                    countLabelledPointsInCluster[oldM] -= 1;
                }
            }
            assert second.doubleValue(h) > 0. || (assignment.intValue(h) >> 16) == (assignment.intValue(h) & 0x7FFF);
            assert (DBIDUtil.equal(h, miter.seek(m)));
            // Compute costs of reassigning other objects j:
            for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
                // new medoid is already dealt with
                if(DBIDUtil.equal(h, j)) {
                    continue;
                }
                int objColor = pointLabelMap.intValue(j);
                // distance(j, i) for pi == pj
                final double distCur = nearest.doubleValue(j);
                // distance(j, o) to second nearest / possible reassignment
                final double distSec = second.doubleValue(j);
                // distance(j, h) to new medoid
                double distH = Double.POSITIVE_INFINITY;
                if(isValidObjMedPair(j, m)) {
                    distH = distQ.distance(h, j);
                }
                // lower byte nearest, upper byte second
                final int prevN = assignment.intValue(j) & 0x7FFF;
                final int prevS = assignment.intValue(j) >> 16;
                final boolean sValid = prevS != prevN;
                // Case 1b: j switches to new medoid, or to the second nearest:
                // Nearest medoid is gone.
                if(prevN == m) { 
                    // new point is closer than the second closest
                    // or second is not valid
                    if(distH < distSec || distH < distCur ||!sValid) {
                        // Replace nearest, medoid id stays the same
                        nearest.putDouble(j, distH);
                    }
                    else {
                        // second is closer than the new medoid
                        // Obj goes to the second closest
                        // recolor that if necessary
                        if(objColor != 0) {
                            countLabelledPointsInCluster[prevN] -= 1;
                            countLabelledPointsInCluster[prevS] += 1;
                            assert clusterLabels[prevS] == 0 || clusterLabels[prevS] == objColor;
                            clusterLabels[prevS] = objColor;
                        }
                        nearest.putDouble(j, distSec);
                        // Find new second nearest.
                        int newSecondNearest = updateSecondNearest(j, miter, m, distH, prevS);
                        assignment.putInt(j, prevS | (newSecondNearest << 16));
                        // change of color of prev_med -> 0 ; +1 for the new
                        // assignment
                        assert second.doubleValue(j) > 0. || prevS == newSecondNearest || distQ.distance(j, miter.seek(newSecondNearest)) == second.doubleValue(j);
                    }
                    assert second.doubleValue(j) > 0. || (assignment.intValue(j) >> 16) == (assignment.intValue(j) & 0x7FFF) || distQ.distance(j, miter.seek(assignment.intValue(j) >> 16)) == second.doubleValue(j);
                }
                // Nearest medoid not replaced
                else {
                    // boolean secValid = ps != pn && ps != m && (objColor == 0
                    // ||
                    // objColor == oldLbl);
                    // obj is compatible to the new cluster
                    // h new closest
                    if(distH < distCur) {
                        // update nearest
                        if(objColor != 0) {
                            countLabelledPointsInCluster[prevN] -= 1;
                            countLabelledPointsInCluster[m] += 1;
                        }
                        nearest.putDouble(j, distH);
                        // update second to prev nearest
                        if(distCur <= distSec || !sValid || m == prevS) {
                            second.putDouble(j, distCur);
                            assignment.putInt(j, m | (prevN << 16));
                        }
                        // second was already closer than nearest
                        // keep the old second as the new second
                        else {
                            assignment.putInt(j, m | (prevS << 16));
                        }
                        assert (second.doubleValue(j) > 0. && (assignment.intValue(j) >> 16) != (assignment.intValue(j) & 0x7FFF)) || (second.doubleValue(j) == 0. && (assignment.intValue(j) >> 16) == (assignment.intValue(j) & 0x7FFF)) || distQ.distance(j, miter.seek(assignment.intValue(j) >> 16)) == second.doubleValue(j);
                    }
                    // prev second was replaced
                    else if(prevS == m) { // Second was replaced.
                        int secondN = updateSecondNearest(j, miter, m, distH, prevN);
                        assignment.putInt(j, prevN | (secondN << 16));
                        assert (second.doubleValue(j) > 0. && (assignment.intValue(j) >> 16) != (assignment.intValue(j) & 0x7FFF)) || (second.doubleValue(j) == 0. && (assignment.intValue(j) >> 16) == (assignment.intValue(j) & 0x7FFF)) || distQ.distance(j, miter.seek(assignment.intValue(j) >> 16)) == second.doubleValue(j);
                    }
                    // h is second clostest
                    else if(distH < distSec) {
                        second.putDouble(j, distH);
                        assignment.putInt(j, prevN | (m << 16));
                    }
                    assert (second.doubleValue(j) > 0. && (assignment.intValue(j) >> 16) != (assignment.intValue(j) & 0x7FFF)) || (second.doubleValue(j) == 0. && (assignment.intValue(j) >> 16) == (assignment.intValue(j) & 0x7FFF)) || distQ.distance(j, miter.seek(assignment.intValue(j) >> 16)) == second.doubleValue(j);
                }
                assert (second.doubleValue(j) > 0. && (assignment.intValue(j) >> 16) != (assignment.intValue(j) & 0x7FFF))|| (second.doubleValue(j) == 0. && (assignment.intValue(j) >> 16) == (assignment.intValue(j) & 0x7FFF)) || distQ.distance(j, miter.seek(assignment.intValue(j) >> 16)) == second.doubleValue(j);
                assert (distQ.distance(j, miter.seek(assignment.intValue(j) & 0x7FFF)) == nearest.doubleValue(j));
                assert objColor > 0 || (assignment.intValue(j) & 0x7FFF) != (assignment.intValue(j) >> 16);
            }
            // adjust cluster coloring
            for(int i = 0; i < countLabelledPointsInCluster.length; i++) {
              if(countLabelledPointsInCluster[i] == 0) {
                clusterLabels[i] = 0; // we have uncolored the
                                      // cluster
              }
              // color of the cluster is set when the medoid is
              // swapped
            }
        }

        protected int updateSecondNearest(DBIDRef j, DBIDArrayIter miter, int s, double ds, int n) {
            assert ds >= nearest.doubleValue(j); // might fail
            double sDist = ds;
            if(s == n) {
                sDist = Double.POSITIVE_INFINITY;
            }
            int sBest = s;
            for(miter.seek(0); miter.valid(); miter.advance()) {
                // check is second has the valid label as j
                if(!isValidObjSndMedPair(j, miter.getOffset())) {
                    continue;
                }
                if(miter.getOffset() != s && miter.getOffset() != n) {
                    double d = distQ.distance(j, miter);
                    if(d < sDist) {
                        sDist = d;
                        sBest = miter.getOffset();
                    }
                }
            }
            if(sDist == Double.POSITIVE_INFINITY) {
                assert sBest == s && (s == n || !isValidObjMedPair(j, s));
                second.putDouble(j, 0);
                return n;
            }
            assert sDist > 0. || distQ.distance(j, miter.seek(sBest)) == sDist;
            assert sDist < Double.POSITIVE_INFINITY;
            second.putDouble(j, sDist);
            return sBest;
        }

        /**
         * Compute the reassignment cost, for all medoids in one pass.
         *
         * @param h Current object to swap with any medoid.
         * @param loss Loss change aggregation array, must have size k
         * @return Loss change accumulator that applies to all
         */
        protected void computeLabeledReassignmentCost(DBIDRef h, double[] loss, double[][] acc) {
          final int k = loss.length;
          final int hColor = this.pointLabelMap.intValue(h);
          // for h labelled it can be reduced to 1 x k
          // Compute costs of reassigning other objects o:
          for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
            final int jColor = this.pointLabelMap.intValue(j);

            final double dn = nearest.doubleValue(j);
            final double ds = second.doubleValue(j);
            int n = assignment.intValue(j) & 0x7FFF;
            int s = assignment.intValue(j) >> 16;

            boolean sValid = (n != s);
            boolean colComp = hColor == jColor || hColor == 0 || jColor == 0;

            // both colored and not the same color
            if(!colComp) {
              // obj can not be assigned to the new medoid
              continue;
            }
            final double dh = distQ.distance(h, j);
            assert ds != Double.POSITIVE_INFINITY;
            // Case (i): new medoid is closest:
            if(sValid) {
              if(dh < dn) {
                // regardless of switched medoid we go to the new one
                // get the color of x_o + update the
                // correct_label_accumulator
                double benefit = dh - dn;
                for(int i = 0; i < k; i++) {
                  if(i == n) {
                    acc[i][jColor] += dn - ds; // loss update
                  }
                  acc[i][jColor] += benefit;
                }
                continue;
              }
              // New Medoid Closer than Second
              // but not better than the current nearest
              if(dh < ds) {
                assert ds != Double.POSITIVE_INFINITY;
                // 0 - dn (negative value when second does not exist)
                // loss already includes ds - dn, adjust to d(xo) - dn
                // we remove ds vom the loss, add dn to the loss 
                // and remove dn from the loss and add dh
                // we do not have any benefit
                acc[n][jColor] += dh - ds;
              }
              // Obj stays with the prev. second closest cluster
              continue;
            }
            assert hColor == jColor || hColor == 0;
            assert ds == 0;
            double benefit = dh - dn;
            for(int i = 0; i < k; i++) {
              if(i == n) {
                // acc[i][jColor] += dn - ds;
                acc[i][jColor] += dn; // update loss
                acc[i][jColor] += benefit;
              }
              else if(benefit < 0) {
                acc[i][jColor] += benefit; // update loss
              }
            }
          }
        }

        protected double assignToNearestCluster(ArrayDBIDs medoids) {
            DBIDArrayIter miter = medoids.iter();
            double cost = 0.;
            for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {

                double mindist = Double.POSITIVE_INFINITY;
                double mindist2 = Double.POSITIVE_INFINITY;
                int minindx = -1, minindx2 = -1;
                for(miter.seek(0); miter.valid(); miter.advance()) {
                    // invalid combination of object and medoid, so they can
                    // never end up
                    // in the same cluster
                    if(!isValidObjSndMedPair(iditer, miter.getOffset())) {
                        continue;
                    }
                    final double dist = distQ.distance(iditer, miter);
                    if(dist < mindist && isValidObjMedPair(iditer, miter.getOffset())) {
                        if(mindist < mindist2) {
                            minindx2 = minindx;
                            mindist2 = mindist;
                        }
                        minindx = miter.getOffset();
                        mindist = dist;
                    }
                    else if(dist < mindist2) {
                        minindx2 = miter.getOffset();
                        mindist2 = dist;
                    }
                }
                if(minindx < 0) {
                    throw new AbortException("Too many infinite distances. Cannot assign objects.");
                }
                countLabelledPointsInCluster[minindx] += pointLabelMap.intValue(iditer) == 0 ? 0 : 1;
                // it is important to track the number of colored objects that
                // consider
                // obj as second closest
                if(minindx2 != -1) {
                    assignment.put(iditer, minindx | (minindx2 << 16));
                    second.put(iditer, mindist2);
                }
                else {
                    assignment.put(iditer, minindx | (minindx << 16));
                    second.put(iditer, 0);
                }
                nearest.put(iditer, mindist);
                assert second.doubleValue(iditer) > 0 || minindx2 == -1 || distQ.distance(medoids.get(minindx),medoids.get(minindx2)) == 0;
                cost += mindist;
            }
            return cost;
        }

        protected boolean isValidObjMedPair(DBIDRef objIt, int clId) {
            int objLbl = pointLabelMap.intValue(objIt);
            // objLabel isn't set, we don't care
            if(objLbl == 0) {
                return true;
            }
            // if cluster doesn't have the label, then we have to make sure,
            // that we
            // only insert unlabeled points
            int clLbl = clusterLabels[clId];
            // both have labels, they have to be the same
            return objLbl == clLbl;
        }

        protected boolean isValidObjSndMedPair(DBIDRef objIt, int clId) {
            int cluLbl = clusterLabels[clId];
            // here it doesn't matter snd closest doesn't have a label
            if(cluLbl == 0) {
                return true;
            }
            int objLbl = pointLabelMap.intValue(objIt);
            // objLabel isn't set, we don't care
            if(objLbl == 0) {
                return true;
            }
            // both have labels, they have to be the same
            return objLbl == cluLbl;
        }

        protected double calculateTotalCost(DBIDArrayMIter m){
          double loss = 0.;
          for (DBIDIter x = ids.iter(); x.valid(); x.advance()){
            int n = assignment.intValue(x) & 0x7FFF;
            double dn = nearest.doubleValue(x);
            double d = distQ.distance(x, m.seek(n));
            assert d == dn;
            assert isValidObjMedPair(x, n);
            loss += dn;
          }
          return loss;
        }
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
    public static class Par<O> extends SemiSupervisedKMedoids.Par<O> {

      @Override
      public void configure(Parameterization config) {
        super.configure(config);
      }

        @Override
        public LabeledPAM<O> make() {
            return new LabeledPAM<>(distance, k, maxiter, initializer);
        }
    }
}
