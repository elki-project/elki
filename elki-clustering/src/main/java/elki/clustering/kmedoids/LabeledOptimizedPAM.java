package elki.clustering.kmedoids;

import java.util.Arrays;

import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.database.datastore.WritableIntegerDataStore;
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

/**
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class LabeledOptimizedPAM<O> extends LabeledPAM<O> {

    private static final Logging LOG = Logging.getLogger(LabeledOptimizedPAM.class);

    private static final String KEY = LabeledOptimizedPAM.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     * @param rnd
     */
    public LabeledOptimizedPAM(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
        super(distance, k, maxiter, initializer);
    }

    @Override
    Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels) {
      return new Instance(distQ, ids, assignment, labelsMaps, clusterLabel, noLabels);
    }

    /**
     * Instance for a single dataset.
     * <p>
     * 
     * @author Miriama Janosova
     * @author Andreas Lang
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
        protected double clusterData(int maxiter, int k, double tc, DBIDArrayMIter m, double[] pcost, IndefiniteProgress prog) {
            // double calculatedLoss = calculateTotalCost(m);
            // if(Math.abs(calculatedLoss - tc) > 1e-12) {
            //     System.out.println("Error in initial Loss: " + (calculatedLoss - tc));
            //     tc = calculatedLoss;
            // }
            Arrays.fill(numberOfClustersWithLabel, 0);
            for(int i = 0; i < k; i++) {
                numberOfClustersWithLabel[clusterLabels[i]]++;
            }
            updatePriorCost(pcost, m, k);
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
                    double[] colAcc = new double[numberOfLabels + 1];
                    double[] lossCor = new double[k];
                    computeLabeledReassignmentCostNew(h, cost, colAcc, lossCor);

                    // Update costs and select color
                    int[] new_col = updateCostsUpd(h, cost, colAcc, lossCor, k);

                    
                    int min = VMath.argmin(cost);
                    double bestcost = cost[min];

                    // if the improvement is rounded 0
                    if(!(bestcost < -1e-12 * tc)) {
                        continue;
                    }
                    ++swaps;
                    lastswap.set(h); // x_c .. so we make the swap
                    updateAssignment(m, h, min, new_col[min]);
                    Arrays.fill(numberOfClustersWithLabel, 0);
                    for(int i = 0; i < k; i++) {
                        numberOfClustersWithLabel[clusterLabels[i]]++;
                    }
                    updatePriorCost(pcost, m, k);
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

        protected void updatePriorCost(double[] pcost, DBIDArrayIter mIter, int k) {
            Arrays.fill(pcost, 0);
            for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
                int n = assignment.intValue(j) & 0x7FFF;
                int s = assignment.intValue(j) >> 16;
                assert second.doubleValue(j) > 0 || s == n;
                if (s == n){
                    if (canUncolor(k, pointLabelMap.intValue(j))){
                        s = updateSecondNearest(j, mIter, s, Double.POSITIVE_INFINITY, n);
                        assignment.putInt(j, n | (s << 16));
                        assert second.doubleValue(j) > 0 || s == n;
                    }
                }else if (!isValidObjSndMedPair(j, s)) {
                    // second closest was coloured at some point after j was
                    // assigned
                    // there, thus we need to look for a new one
                    s = updateSecondNearest(j, mIter, s, Double.POSITIVE_INFINITY, n);
                    assignment.putInt(j, n | (s << 16));
                    assert second.doubleValue(j) > 0 || s == n;
                }
                assert second.doubleValue(j) > 0 || s == n;
                // cost of removing the medoid + reassigning objects to the
                // second_closest
                pcost[n] += second.doubleValue(j) - nearest.doubleValue(j);
            }
        }

        /**
         * 
         * Update costs and select color
         * 
         * @param h
         * @param acc
         * @param colAcc
         * @param cost
         * @param k
         * @return
         */
        protected int[] updateCostsUpd(DBIDRef h, double[] loss, double[] colAcc, double[] lossCor, int k) {
            // update the cost with given accumulator -> based on the
            // cluster
            // color + add uncolored one
            int hLabel = pointLabelMap.intValue(h);
            int[] new_col = new int[k];
            int bestCol = hLabel;
            int secondCol = -1;
            double min = 0.;
            double min2 = 0.;
            // Decide which color gives the best benefit
            if(bestCol == 0) {
                for (int i = 1; i< colAcc.length;i++){
                    assert colAcc[i] <= 0.;
                    if (colAcc[i] < min){
                        min2 = min;
                        secondCol = bestCol;
                        min = colAcc[i];
                        bestCol = i;
                    }
                    else if (colAcc[i] < min2){
                        min2 = colAcc[i];
                        secondCol = i;
                    }
                }
            }

            for(int i = 0; i < loss.length; i++) {
                // medoid has a colour, we know what has to be a colour
                // of a new cluster
                int clusterLabel = clusterLabels[i];
                if(hLabel != 0) {
                    if(hLabel != clusterLabel && !canUncolor(k, clusterLabel)) {
                        loss[i] = Double.POSITIVE_INFINITY;
                        new_col[i] = clusterLabel;
                        continue;
                    }
                    loss[i] += colAcc[hLabel] + colAcc[0];
                    if (bestCol == clusterLabel){
                        loss[i] += lossCor[i];
                    }
                    new_col[i] = hLabel;
                    continue;
                }
                // medoid does not have a color
                // cluster can be relabeled
                if(canUncolor(k, clusterLabel)) {
                    // check if best is really the best
                    // adds the correction for colored points of that cluster if needed
                    int localBest = bestCol;
                    if (lossCor[i] < 0){
                        localBest = min < colAcc[clusterLabel] + lossCor[i] ? localBest: clusterLabel;
                    }
                    else if (localBest == clusterLabel && lossCor[i] > 0){
                        localBest = colAcc[localBest] + lossCor[i] < min2 ? localBest : secondCol;
                    }
                    if (localBest == clusterLabel) {
                        loss[i] += lossCor[i];
                    }
                    loss[i] += colAcc[0];
                    if(localBest > 0) {
                        loss[i] += colAcc[localBest];
                    }
                    new_col[i] = localBest;
                    continue;
                }
                // Cluster can not be relabeled
                // cluster label can not be 0
                loss[i] += colAcc[clusterLabel] + colAcc[0] + lossCor[i]; 
                new_col[i] = clusterLabel;
            }
            return new_col;
        }

        /**
         * Compute the reassignment cost, for all medoids in one pass.
         *
         * @param h Current object to swap with any medoid.
         * @param loss Loss change aggregation array, must have size k
         * @return Loss change accumulator that applies to all
         */
        protected void computeLabeledReassignmentCostNew(DBIDRef h, double[] loss, double[] colAcc, double[] lossCor) {
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

                // Case 0
                if(!colComp) {
                    // obj can not be assigned to the new medoid
                    continue;
                }
                final double dh = distQ.distance(h, j);
                assert ds != Double.POSITIVE_INFINITY;
                assert ds != 0 || s == n;
                if(sValid) {
                    // Cluster can be replaced with a medoid of any color.
                    if(dh < dn) {
                        // New Medoid Closer than Current
                        // add benefit to the accumulator
                        // update loss for cluster so that it is based on the
                        // old medoid not the second
                        double benefit = dh - dn;
                        if (jColor == 0){
                            loss[n] += dn - ds;
                        }
                        else{
                            lossCor[n] += dn - ds;
                        }
                        colAcc[jColor] += benefit;
                        continue;
                    }
                    // New Medoid Closer than Second
                    // but not better than the current nearest
                    if(dh < ds) {
                        assert ds != Double.POSITIVE_INFINITY;
                        // reduce the cost of removing the medoid
                        // might be problematic
                        if(jColor == 0) {
                            loss[n] += dh - ds;
                        }
                        else {
                            lossCor[n] += dh - ds;
                        }
                        continue;
                    }
                    // object is assigned to the second closest medoid
                    // nothing changes
                    continue;
                }
                // Cluster needs to keep the same color
                assert hColor == jColor || hColor == 0;
                assert ds == 0;
                // Colors are compatible otherwise Case 0
                // cluster color can not be changed
                // Benefit can be positive if the
                // new Medoid is farther than the old medoid
                double benefit = dh - dn;
                if(benefit < 0) {
                    // loss[n] += dn - ds;
                    loss[n] += dn;
                    colAcc[jColor] += dh - dn;
                    continue;
                }
                // loss[n] += dh - ds;
                loss[n] += dh;
            }
        }
    }

    /**
     * Parameterization class.
     *
     * @author Andreas Lang, Miriama Janosova
     */
    public static class Par<O> extends LabeledPAM.Par<O> {

        @Override
        public LabeledOptimizedPAM<O> make() {
            return new LabeledOptimizedPAM<>(distance, k, maxiter, initializer);
        }
    }
}
