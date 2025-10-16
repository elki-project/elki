package elki.clustering.kmedoids;

import java.util.Arrays;

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

/**
 * Debug class, to be removed, that compares the optimized with non-optimized implementation.
 * This class serves as a verification tool to ensure both implementations produce identical results
 * while demonstrating performance differences between approaches.
 *
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class LabeledVerifyingPAM<O> extends LabeledOptimizedPAM<O> {

    private static final Logging LOG = Logging.getLogger(LabeledVerifyingPAM.class);

    private static final String KEY = LabeledVerifyingPAM.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     * @param rnd
     */
    public LabeledVerifyingPAM(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
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
    protected static class Instance extends LabeledOptimizedPAM.Instance {

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
            double calculatedLoss = calculateTotalCost(m);
            if(Math.abs(calculatedLoss - tc) > 1e-12) {
                System.out.println("Error in initial Loss: " + (calculatedLoss - tc));
                tc = calculatedLoss;
            }
            Arrays.fill(numberOfClustersWithLabel, 0);
            for(int i = 0; i < k; i++) {
                numberOfClustersWithLabel[clusterLabels[i]]++;
            }
            updatePriorCost(pcost, m, k);
            Arrays.fill(numberOfClustersWithLabel, 0);
            DBIDVar lastswap = DBIDUtil.newVar();
            double[] cost = new double[k];
            double[] costOld = new double[k];
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
                    int hLabel = pointLabelMap.intValue(h);
                    int[] old_labels = clusterLabels.clone();
                    System.arraycopy(pcost, 0, cost, 0, pcost.length);
                    System.arraycopy(pcost, 0, costOld, 0, pcost.length);
                    // The cost we get back by making the non-medoid h medoid.

                    // Compute Reassignment costs
                    //Old
                    double[][] acc = new double[k][numberOfLabels + 1];
                    computeLabeledReassignmentCost(h, costOld, acc);

                    //new
                    double[] colAcc = new double[numberOfLabels + 1];
                    double[] lossCor = new double[k];
                    computeLabeledReassignmentCostNew(h, cost, colAcc, lossCor);

                    // Update costs and select color
                    // Old
                    int[] new_colO = updateCosts(h, acc, costOld, k);
                    // New
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

                    for(int i = 0; i < k; i++) {
                        if(Math.abs(cost[i] - costOld[i]) > 2e-12) {
                            System.out.println("Swap: " + swaps + " cost: " + cost[i] + " costNew: " + costOld[i] + " diff: " + (cost[i] - costOld[i]));
                        }
                        if(new_col[i] != new_colO[i]) {
                            System.out.println("Swap: " + swaps + " new_col: " + new_col[i] + " new_colU: " + new_colO[i]);
                        }
                    }

                    calculatedLoss = calculateTotalCost(m);
                    if(Math.abs(calculatedLoss - tc) > 5e-12) {
                        System.out.println("Swap: "+ swaps + " Error in Calculated Loss: " + (calculatedLoss - tc));
                        System.out.println("A Point wih label: " + hLabel + " was swapped to cluster: " + min);
                        System.out.println("Old Cluster Labels:");
                        for(int i = 0; i < k; i++) {
                            System.out.print(old_labels[i] + ", ");
                        }
                        System.out.println();
                        System.out.println("New Cluster Labels:");
                        for(int i = 0; i <k; i++){
                            System.out.print(clusterLabels[i] + ", ");
                        }
                        System.out.println();
                    }
                    tc = calculatedLoss;

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

    /**
     * Parameterization class.
     *
     * @author Andreas Lang
     * @author Miriama Janosova
     */
    public static class Par<O> extends LabeledOptimizedPAM.Par<O> {

        @Override
        public LabeledVerifyingPAM<O> make() {
            return new LabeledVerifyingPAM<>(distance, k, maxiter, initializer);
        }
    }
}
