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
import elki.utilities.exceptions.AbortException;

/**
 * Implementation of the COP k-medoids algorithm.
 *
 * The COP (Clustering with Optimal Partitioning) k-medoids algorithm
 * extends the standard k-medoids approach by incorporating label information to guide
 * cluster assignment and medoid selection. It ensures that labeled objects are assigned
 * to clusters that match their labels, while also optimizing the placement of medoids.
 *
 * @author Miriama Janosova
 * @author Andreas Lang
 *
 * @navassoc - - - MedoidModel
 * @has - - - KMedoidsInitialization
 *
 * @param <O> object datatype
 */
public class COPKmedoids<O> extends SemiSupervisedKMedoids<O> {
    /**
     * The logger for this class.
     */
    private static final Logging LOG = Logging.getLogger(COPKmedoids.class);

    /**
     * Key for statistics logging.
     */
    private static final String KEY = COPKmedoids.class.getName();

    /**
     * Constructor.
     *
     * @param distance distance function
     * @param k k parameter
     * @param maxiter Maxiter parameter
     * @param initializer Function to generate the initial means
     */
    public COPKmedoids(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
        super(distance, k, maxiter, initializer);
    }


    /**
     * Instance class for COP k-medoids algorithm.
     * This inner class handles the specific implementation of the COP algorithm
     * including assignment to clusters and medoid re-computation.
     */
    protected static class Instance extends SemiSupervisedKMedoids.Instance {
        private int[] countLabelledInCl;

        /**
         * Constructor for the COP k-medoids instance.
         *
         * @param distQ Distance query function
         * @param ids IDs to process
         * @param assignment Cluster assignment
         * @param labelsMaps Labels mapping
         * @param clusterLabel Cluster labels
         * @param numberOfLabels Number of labels
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
            super(distQ, ids, assignment, labelsMaps, clusterLabel, numberOfLabels);
            this.countLabelledInCl = new int[clusterLabel.length];
        }

        /**
         * Run the PAM optimization phase for COP k-medoids.
         *
         * @param medoids Medoids list
         * @param maxiter Maximum number of iterations
         * @return final cost
         */
        @Override
        protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
            // Swap phase
            IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("COP-Kmedoids iteration", LOG) : null;
            double tc = 0;
            int iteration = 0, swaps = 0;
            while (iteration < maxiter) {
                ++iteration;
                LOG.incrementProcessed(prog);
                tc = assignToNearestCluster(medoids);

                if (LOG.isStatistics()) {
                    LOG.statistics(new DoubleStatistic(KEY + ".iteration-" + iteration + ".cost", tc));
                }
                ArrayModifiableDBIDs updatedMedoids =  DBIDUtil.newArray();
                int newSwaps = recomputeMedoids(medoids, updatedMedoids);
                if (newSwaps == 0) {
                    areAllMedsdPartOfClusters(medoids);
                    break; // Converged
                }
                DBIDArrayMIter newMedsIter = updatedMedoids.iter();
                for (; newMedsIter.valid(); newMedsIter.advance()) {
                    medoids.set(newMedsIter.getOffset(), newMedsIter);
                }
                swaps += newSwaps;
                areAllMedsdPartOfClusters(medoids);
            }
            areAllMedsdPartOfClusters(medoids);
            // TODO: we may have accumulated some error on tc.
            LOG.setCompleted(prog);
            if (LOG.isStatistics()) {
                LOG.statistics(new LongStatistic(KEY + ".iterations", iteration));
                LOG.statistics(new LongStatistic(KEY + ".swaps", swaps));
                LOG.statistics(new DoubleStatistic(KEY + ".final-cost", tc));
            }
            // Cleanup
            for (DBIDIter it = ids.iter(); it.valid(); it.advance()) {
                assignment.putInt(it, assignment.intValue(it));
            }
            areAllMedsdPartOfClusters(medoids);

            for (int i = 0; i < clusterLabels.length; i++) {
                System.out.println("Cluster id " + i + " label " + clusterLabels[i]);
            }
            return tc;
        }

        /**
         * Check if all medoids are part of clusters.
         *
         * @param medoids Medoids to check
         * @return true if all medoids are in clusters, false otherwise
         */
        public boolean areAllMedsdPartOfClusters(ArrayModifiableDBIDs medoids){
            DBIDArrayMIter miter = medoids.iter();
            ArrayDBIDs[] clusters = getClusters();

            for (; miter.valid(); miter.advance()) {
                // System.out.println(miter.toString());
                // System.out.println("is present in cluster " +
                // miter.getOffset());
                if (assignment.intValue(miter) != miter.getOffset()) {
                    System.out.println("ERROR: i'm loosing medoids");
                    return false;
                }
            }

            for (int i = 0; i < clusters.length; i++){
                System.out.println("cluster number " + i);
                DBIDRef medoid = miter.seek(i);
                DBIDIter iter = clusters[i].iter();
                boolean b = false;
                for (; iter.valid(); iter.advance()){
                    if(DBIDUtil.equal(medoid, iter)) {
                        b = true;
                    }
                }
                if (!b){
                    System.out.println("ERROR: i am somehow in the assignment, but not in the cluster???");
                }
            }
            return true;
        }

        /**
         * Get clusters from assignment.
         *
         * @return Array of clusters
         */
        private ArrayDBIDs[] getClusters(){
            int numOfCls = clusterLabels.length;
            ArrayModifiableDBIDs[] result = new ArrayModifiableDBIDs[numOfCls];
            for (int i = 0; i < numOfCls; i++){
                result[i] = DBIDUtil.newArray();
            }

            DBIDIter iter = ids.iter();
            for (; iter.valid(); iter.advance()){
                int clId = assignment.intValue(iter);
                result[clId].add(iter);
            }
            return result;
        }

        /**
         * Recompute medoids for each cluster.
         *
         * @param medoids Current medoids
         * @param updatedMedoids Updated medoids
         * @return Number of swaps
         */
        private int recomputeMedoids(ArrayModifiableDBIDs medoids, ArrayModifiableDBIDs updatedMedoids){
            ArrayDBIDs[] clusters = getClusters();
            DBIDArrayMIter medIter = medoids.iter();
            int swaps = 0;
            for (int i = 0; i < clusters.length; i++){
                DBIDRef newMedoid = findMedoid(clusters[i]);
                DBIDRef oldMedoid = medIter.seek(i);
                if (!DBIDUtil.equal(newMedoid, oldMedoid)){
                    swaps++;
                }
                updatedMedoids.add(newMedoid);
            }
            return swaps;
        }

        /**
         * Find the medoid for a given cluster.
         *
         * @param cluster Cluster to find medoid for
         * @return Medoid of the cluster
         */
        private DBIDRef findMedoid(ArrayDBIDs cluster){
            double minSum = Double.MAX_VALUE;
            DBIDVar medoid = DBIDUtil.newVar();
            for (DBIDIter iter = cluster.iter(); iter.valid(); iter.advance()){
                double sum = 0;
                for (DBIDIter other = cluster.iter(); other.valid(); other.advance()){
                    double d = distQ.distance(iter, other);
                    sum += d;
                }
                if (sum < minSum){
                    minSum = sum;
                    medoid.set(iter);
                }
            }
            return medoid;
        }

        /**
         * Check if an object-cluster pair is valid.
         *
         * @param objIdx Object index
         * @param clusterId Cluster ID
         * @return true if valid, false otherwise
         */
        private boolean validObjClusterPair(DBIDRef objIdx, int clusterId) {
            if (pointLabelMap.intValue(objIdx) == 0){
                return true; // object does not have a label, so we do not care where its clustered
            }
            if (clusterLabels[clusterId] == 0){
                // the cluster itself has no label, so we can assign the object here
                return true;
            }
            // both object and cluster have labels, so we compare if they are the same
            return pointLabelMap.intValue(objIdx) == clusterLabels[clusterId];
        }

        /**
         * Assign objects to nearest clusters based on distance and label constraints.
         *
         * @param medoids Medoids to use for assignment
         * @return Total cost of assignment
         */
        protected double assignToNearestCluster(ArrayDBIDs medoids) {
            DBIDArrayIter miter = medoids.iter();
            Arrays.fill(countLabelledInCl, 0);
            double cost = 0.;

            for (DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
                double mindist = Double.MAX_VALUE;
                int minindx = -1;
                for(miter.seek(0); miter.valid(); miter.advance()) {
                    // invalid combination of object and medoid, so they can
                    // never end up in the same cluster
                    if (!validObjClusterPair(iditer, miter.getOffset())){
                        continue;
                    }
                    final double dist = distQ.distance(iditer, miter);
                    if (dist < mindist) {
                        minindx = miter.getOffset();
                        mindist = dist;
                    }
                }
                if (minindx < 0) {
                    throw new AbortException("Too many infinite distances. Cannot assign objects.");
                }
                int objLabel = pointLabelMap.intValue(iditer);
                countLabelledInCl[minindx] += objLabel == 0 ? 0 : 1;
                assignment.put(iditer, minindx);
                // if cluster does not have label, and object does, then update clusterLabel
                if (clusterLabels[minindx] == 0 && objLabel != 0) {
                    clusterLabels[minindx] = objLabel;
                }
                cost += mindist;
            }
            for (int i = 0; i < clusterLabels.length; i++){
                // nullify the label of the cluster
                if (countLabelledInCl[i] == 0){
                    clusterLabels[i] = 0;
                }
            }
            return cost;
        }
    }

    /**
     * Create a new instance of the COP k-medoids algorithm.
     *
     * @param distQ Distance query function
     * @param ids IDs to process
     * @param assignment Cluster assignment
     * @param labelsMaps Labels mapping
     * @param clusterLabel Cluster labels
     * @param noLabels Number of labels
     * @return New instance
     */
    @Override
    Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels) {
      return new Instance(distQ, ids, assignment, labelsMaps, clusterLabel, noLabels);
    }

    /**
     * Get the logger for this class.
     *
     * @return Logger instance
     */
    @Override
    protected Logging getLogger() {
        return LOG;
    }

    /**
     * Parameterization class for COP k-medoids.
     *
     * @author Andreas lang
     */
    public static class Par<O> extends SemiSupervisedKMedoids.Par<O> {

        /**
         * Create a new COP k-medoids instance.
         *
         * @return New COP k-medoids instance
         */
        @Override
        public COPKmedoids<O> make() {
            return new COPKmedoids<>(distance, k, maxiter, initializer);
        }
    }

}
