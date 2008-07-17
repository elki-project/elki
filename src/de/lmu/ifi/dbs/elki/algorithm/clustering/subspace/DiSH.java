package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalAxesParallelCorrelationCluster;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalAxesParallelCorrelationClusters;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DiSHDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Algorithm for detecting subspace hierarchies.
 * <p/>
 * <p>Reference:
 * <br>E. Achtert, C. Boehm, H.-P. Kriegel, P. Kröger, I. Mueller-Gorman, A. Zimek:
 * Detection and Visualization of Subspace Cluster Hierarchies.
 * <br>In Proc. DASFAA Conference, Bangkok, Thailand, 2007.
 * </p>
 *
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class DiSH<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {
    /**
     * OptionID for {@link #EPSILON_PARAM}
     */
    public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID(
        "dish.epsilon",
        "The maximum radius of the neighborhood " +
            "to be considered in each dimension for determination of " +
            "the preference vector."
    );

    /**
     * Parameter that specifies the maximum radius of the neighborhood to be
     * considered in each dimension for determination of the preference vector,
     * must be a double equal to or greater than 0.
     * <p>Default value: {@code 0.001} </p>
     * <p>Key: {@code -dish.epsilon} </p>
     */
    private final DoubleParameter EPSILON_PARAM =
        new DoubleParameter(EPSILON_ID, new GreaterEqualConstraint(0), 0.001);

    /**
     * Holds the value of {@link #EPSILON_PARAM}.
     */
    private double epsilon;

    /**
     * OptionID for {@link de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.DiSH#MU_PARAM}
     */
    public static final OptionID MU_ID = OptionID.getOrCreateOptionID(
        "dish.mu",
        "The minimum number of points as a smoothing factor to avoid the single-link-effekt."
    );


    /**
     * Parameter that specifies the a minimum number of points as a smoothing
     * factor to avoid the single-link-effect,
     * must be an integer greater than 0.
     * <p>Default value: {@code 1} </p>
     * <p>Key: {@code -dish.mu} </p>
     */
    private final IntParameter MU_PARAM = new IntParameter(MU_ID, new GreaterConstraint(0), 1);

    /**
     * The optics algorithm to determine the cluster order.
     */
    private OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics;

    /**
     * Holds the result;
     */
    private Result<V> result;

    /**
     * Provides the DiSH algorithm,
     * adding parameters
     * {@link #EPSILON_PARAM} and {@link #MU_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public DiSH() {
        super();
//    debug = true;

        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter mu
        addOption(MU_PARAM);
    }

    /**
     * Performs the DiSH algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
     */
    protected void runInTime(Database<V> database) throws IllegalStateException {
        if (isVerbose()) {
            verbose("\nRun OPTICS algorithm.");
        }
        optics.run(database);

        if (isVerbose()) {
            verbose("\n\nCompute Clusters.");
        }
        computeClusters(database, (ClusterOrder<V, PreferenceVectorBasedCorrelationDistance>) optics.getResult());
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public Result<V> getResult() {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#getDescription()
     */
    public Description getDescription() {
        return new Description(
            "DiSH",
            "Detecting Subspace cluster Hierarchies",
            "Algorithm to find hierarchical correlation clusters in subspaces.",
            "E. Achtert, C. Boehm, H.-P. Kriegel, P. Kröger, I. Mueller-Gorman, A. Zimek: " +
                "Detection and Visualization of Subspace Cluster Hierarchies. " +
                "In Proc. DASFAA Conference, Bangkok, Thailand, 2007."
        );
    }

    /**
     * Calls {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the value of the parameters
     * {@link #EPSILON_PARAM} and {@link #MU_PARAM}.
     * Then the parameters for the algorithm {@link #optics} are set.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = getParameterValue(EPSILON_PARAM);

        // mu
        int minpts = getParameterValue(MU_PARAM);

        // OPTICS
        optics = new OPTICS<V, PreferenceVectorBasedCorrelationDistance>();

        // verbose and time
        optics.setVerbose(isVerbose());
        optics.setTime(isTime());

        // todo provide setter
        // parameters for optics
        List<String> opticsParameters = new ArrayList<String>();

        // epsilon for OPTICS
        Util.addParameter(opticsParameters, OPTICS.EPSILON_ID, DiSHDistanceFunction.INFINITY_PATTERN);

        // minpts for OPTICS
        Util.addParameter(opticsParameters, OPTICS.MINPTS_ID, Integer.toString(minpts));

        // distance function
        Util.addParameter(opticsParameters, OPTICS.DISTANCE_FUNCTION_ID, DiSHDistanceFunction.class.getName());

        // epsilon for distance function
        opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHDistanceFunction.EPSILON_P);
        opticsParameters.add(Double.toString(epsilon));

        // omit flag
        opticsParameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.OMIT_PREPROCESSING_F);

        // preprocessor
        opticsParameters.add(OptionHandler.OPTION_PREFIX + PreprocessorHandler.PREPROCESSOR_CLASS_P);
        opticsParameters.add(DiSHPreprocessor.class.getName());

        // preprocessor epsilon
        opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.EPSILON_P);
        opticsParameters.add(Double.toString(epsilon));

        // preprocessor minpts
        opticsParameters.add(OptionHandler.OPTION_PREFIX + DiSHPreprocessor.MINPTS_P);
        opticsParameters.add(Integer.toString(minpts));

        // remaining parameters
        for (String parameter : remainingParameters) {
            opticsParameters.add(parameter);
        }

        remainingParameters = optics.setParameters(opticsParameters.toArray(new String[opticsParameters.size()]));
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#getAttributeSettings()}
     * and adds to the returned attribute settings the attribute settings of
     * the algorithm {@link #optics}.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#getAttributeSettings()
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> settings = super.getAttributeSettings();
        settings.addAll(optics.getAttributeSettings());
        return settings;
    }

    /**
     * Computes the hierarchical clusters according to the cluster order.
     *
     * @param database     the database holding the objects
     * @param clusterOrder the cluster order
     */
    private void computeClusters(Database<V> database, ClusterOrder<V, PreferenceVectorBasedCorrelationDistance> clusterOrder) {
        int dimensionality = database.dimensionality();

        DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction = (DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>>) optics.getDistanceFunction();

        // extract clusters
        Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> clustersMap = extractClusters(database, distanceFunction, clusterOrder);

        if (this.debug) {
            StringBuffer msg = new StringBuffer("\nStep 1: extract clusters");
            for (List<HierarchicalAxesParallelCorrelationCluster> clusterList : clustersMap.values()) {
                for (HierarchicalAxesParallelCorrelationCluster c : clusterList) {
                    msg.append("\n").append(Util.format(dimensionality, c.getPreferenceVector())).append(" ids ").append(c.getIDs().size());
                }
            }
            debugFine(msg.toString());
        }

        // check if there are clusters < minpts
        checkClusters(database, distanceFunction, clustersMap);
        if (this.debug) {
            StringBuffer msg = new StringBuffer("\n\nStep 2: check clusters");
            for (List<HierarchicalAxesParallelCorrelationCluster> clusterList : clustersMap.values()) {
                for (HierarchicalAxesParallelCorrelationCluster c : clusterList) {
                    msg.append("\n").append(Util.format(dimensionality, c.getPreferenceVector())).append(" ids ").append(c.getIDs().size());
                }
            }
            debugFine(msg.toString());
        }

        // actualize the levels and indices and sort the clusters
        List<HierarchicalAxesParallelCorrelationCluster> clusters = sortClusters(clustersMap, dimensionality);
        if (this.debug) {
            StringBuffer msg = new StringBuffer("\n\nStep 3: sort clusters");
            for (HierarchicalAxesParallelCorrelationCluster c : clusters) {
                msg.append("\n").append(Util.format(dimensionality, c.getPreferenceVector())).append(" ids ").append(c.getIDs().size());
                for (int i = 0; i < c.getParents().size(); i++) {
                    msg.append("\n   parent ").append(c.getParents().get(i));
                }
                for (int i = 0; i < c.numChildren(); i++) {
                    msg.append("\n   child ").append(c.getChild(i));
                }
            }
            debugFine(msg.toString());
        }

        // build the hierarchy
        buildHierarchy(database, distanceFunction, clusters, dimensionality);
        if (this.debug) {
            StringBuffer msg = new StringBuffer("\n\nStep 4: build hierarchy");
            for (HierarchicalAxesParallelCorrelationCluster c : clusters) {
                msg.append("\n").append(Util.format(dimensionality, c.getPreferenceVector())).append(" ids ").append(c.getIDs().size());
                for (int i = 0; i < c.getParents().size(); i++) {
                    msg.append("\n   parent ").append(c.getParents().get(i));
                }
                for (int i = 0; i < c.numChildren(); i++) {
                    msg.append("\n   child ").append(c.getChild(i));
                }
            }
            debugFine(msg.toString());
        }

        int lambda_max = dimensionality - clusters.get(clusters.size() - 1).getLevel();
        List<HierarchicalAxesParallelCorrelationCluster> rootClusters = new ArrayList<HierarchicalAxesParallelCorrelationCluster>();
        for (HierarchicalAxesParallelCorrelationCluster c : clusters) {
            if (dimensionality - c.getLevel() == lambda_max) {
                rootClusters.add(c);
            }
        }
        result = new HierarchicalAxesParallelCorrelationClusters<V, PreferenceVectorBasedCorrelationDistance>(rootClusters, clusterOrder, database);
    }

    /**
     * Extracts the clusters from the cluster order.
     *
     * @param database         the database storing the objects
     * @param distanceFunction the distance function
     * @param clusterOrder     the cluster order to extract the clusters from
     * @return the extracted clusters
     */
    private Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> extractClusters(Database<V> database,
                                                                                          DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                                                                          ClusterOrder<V, PreferenceVectorBasedCorrelationDistance> clusterOrder) {

        Progress progress = new Progress("Extract Clusters", database.size());
        int processed = 0;
        Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> clustersMap = new HashMap<BitSet, List<HierarchicalAxesParallelCorrelationCluster>>();
        Map<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> entryMap = new HashMap<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>>();
        Map<Integer, HierarchicalAxesParallelCorrelationCluster> entryToClusterMap = new HashMap<Integer, HierarchicalAxesParallelCorrelationCluster>();
        for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
        {
            ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
            entryMap.put(entry.getID(), entry);

            V object = database.get(entry.getID());
            BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

            // get the list of (parallel) clusters for the preference vector
            List<HierarchicalAxesParallelCorrelationCluster> parallelClusters = clustersMap.get(preferenceVector);
            if (parallelClusters == null) {
                parallelClusters = new ArrayList<HierarchicalAxesParallelCorrelationCluster>();
                clustersMap.put(preferenceVector, parallelClusters);
            }

            // look for the proper cluster
            HierarchicalAxesParallelCorrelationCluster cluster = null;
            for (HierarchicalAxesParallelCorrelationCluster c : parallelClusters) {
                V c_centroid = Util.centroid(database, c.getIDs(), c.getPreferenceVector());
                PreferenceVectorBasedCorrelationDistance dist = distanceFunction.correlationDistance(object, c_centroid, preferenceVector, preferenceVector);
                if (dist.getCorrelationValue() == entry.getReachability().getCorrelationValue()) {
                    double d = distanceFunction.weightedDistance(object, c_centroid, dist.getCommonPreferenceVector());
                    if (d <= 2 * epsilon) {
                        cluster = c;
                        break;
                    }
                }
            }
            if (cluster == null) {
                cluster = new HierarchicalAxesParallelCorrelationCluster(preferenceVector);
                parallelClusters.add(cluster);
            }
            cluster.addID(entry.getID());
            entryToClusterMap.put(entry.getID(), cluster);

            if (isVerbose()) {
                progress.setProcessed(++processed);
                progress(progress);
            }
        }

        if (this.debug) {
            StringBuffer msg = new StringBuffer("\nStep 0");
            for (List<HierarchicalAxesParallelCorrelationCluster> clusterList : clustersMap.values()) {
                for (HierarchicalAxesParallelCorrelationCluster c : clusterList) {
                    msg.append("\n").append(Util.format(database.dimensionality(), c.getPreferenceVector())).append(" ids ").append(c.getIDs().size());
                }
            }
            debugFine(msg.toString());
        }

        // add the predecessor to the cluster
        for (BitSet pv : clustersMap.keySet()) {
            List<HierarchicalAxesParallelCorrelationCluster> parallelClusters = clustersMap.get(pv);
            for (HierarchicalAxesParallelCorrelationCluster cluster : parallelClusters) {
                if (cluster.getIDs().isEmpty()) {
                    continue;
                }
                Integer firstID = cluster.getIDs().get(0);
                ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = entryMap.get(firstID);
                Integer predecessorID = entry.getPredecessorID();
                if (predecessorID == null) {
                    continue;
                }
                ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> predecessor = entryMap.get(predecessorID);
                // parallel cluster
                if (predecessor.getReachability().getCommonPreferenceVector().equals(entry.getReachability().getCommonPreferenceVector())) {
                    continue;
                }
                if (predecessor.getReachability().compareTo(entry.getReachability()) < 0) {
                    continue;
                }

                HierarchicalAxesParallelCorrelationCluster oldCluster = entryToClusterMap.get(predecessorID);
                oldCluster.removeID(predecessorID);
                cluster.addID(predecessorID);
                entryToClusterMap.remove(predecessorID);
                entryToClusterMap.put(predecessorID, cluster);
            }
        }

        return clustersMap;
    }

    /**
     * Sets the levels and indices in the clusters and returns a sorted list of the clusters.
     *
     * @param clustersMap    the mapping of bits sets to clusters
     * @param dimensionality the dimensionality of the data
     * @return a sorted list of the clusters
     */
    private List<HierarchicalAxesParallelCorrelationCluster> sortClusters(Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> clustersMap, int dimensionality) {
        // actualize the levels and indices
        int[] clustersInLevel = new int[dimensionality + 1];
        List<HierarchicalAxesParallelCorrelationCluster> clusters = new ArrayList<HierarchicalAxesParallelCorrelationCluster>();
        for (BitSet pv : clustersMap.keySet()) {
            int level = pv.cardinality();
            List<HierarchicalAxesParallelCorrelationCluster> parallelClusters = clustersMap.get(pv);
            for (int i = 0; i < parallelClusters.size(); i++) {
                HierarchicalAxesParallelCorrelationCluster c = parallelClusters.get(i);
                c.setLevel(level);
                c.setLevelIndex(clustersInLevel[level]++);
                if (parallelClusters.size() > 1) {
                    c.setLabel("[" + Util.format(dimensionality, pv) + "_" + i + "]");
                }
                else {
                    c.setLabel("[" + Util.format(dimensionality, pv) + "]");
                }
                clusters.add(c);
            }
        }
        Collections.sort(clusters);
        return clusters;
    }

    /**
     * Removes the clusters with size < minpts from the cluster map and adds them to their parents.
     *
     * @param database         the database storing the objects
     * @param distanceFunction the distance function
     * @param clustersMap      the map containing the clusters
     */
    private void checkClusters(Database<V> database,
                               DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                               Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> clustersMap) {

        // check if there are clusters < minpts
        // and add them to not assigned
        //noinspection unchecked
        int minpts = distanceFunction.getPreprocessor().getMinpts();
        List<HierarchicalAxesParallelCorrelationCluster> notAssigned = new ArrayList<HierarchicalAxesParallelCorrelationCluster>();
        Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> newClustersMap = new HashMap<BitSet, List<HierarchicalAxesParallelCorrelationCluster>>();
        HierarchicalAxesParallelCorrelationCluster noise = new HierarchicalAxesParallelCorrelationCluster(new BitSet());
        for (BitSet pv : clustersMap.keySet()) {
            // noise
            if (pv.cardinality() == 0) {
                List<HierarchicalAxesParallelCorrelationCluster> parallelClusters = clustersMap.get(pv);
                for (HierarchicalAxesParallelCorrelationCluster c : parallelClusters) {
                    noise.addIDs(c.getIDs());
                }
            }
            // clusters
            else {
                List<HierarchicalAxesParallelCorrelationCluster> parallelClusters = clustersMap.get(pv);
                List<HierarchicalAxesParallelCorrelationCluster> newParallelClusters = new ArrayList<HierarchicalAxesParallelCorrelationCluster>(parallelClusters.size());
                for (HierarchicalAxesParallelCorrelationCluster c : parallelClusters) {
                    if (!pv.equals(new BitSet()) && c.getIDs().size() < minpts) {
                        notAssigned.add(c);
                    }
                    else {
                        newParallelClusters.add(c);
                    }
                }
                newClustersMap.put(pv, newParallelClusters);
            }
        }

        clustersMap.clear();
        clustersMap.putAll(newClustersMap);

        for (HierarchicalAxesParallelCorrelationCluster c : notAssigned) {
            if (c.getIDs().isEmpty()) {
                continue;
            }
            HierarchicalAxesParallelCorrelationCluster parent = findParent(database, distanceFunction, c, clustersMap);
            if (parent != null) {
                parent.addIDs(c.getIDs());
            }
            else {
                noise.addIDs(c.getIDs());
            }
        }

        List<HierarchicalAxesParallelCorrelationCluster> noiseList = new ArrayList<HierarchicalAxesParallelCorrelationCluster>(1);
        noiseList.add(noise);
        clustersMap.put(noise.getPreferenceVector(), noiseList);
    }

    /**
     * Returns the parent of the specified cluster
     *
     * @param database         the database storing the objects
     * @param distanceFunction the distance function
     * @param child            the child to search teh parent for
     * @param clustersMap      the map containing the clusters
     * @return the parent of the specified cluster
     */
    private HierarchicalAxesParallelCorrelationCluster findParent(Database<V> database,
                                                                  DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                                                  HierarchicalAxesParallelCorrelationCluster child,
                                                                  Map<BitSet, List<HierarchicalAxesParallelCorrelationCluster>> clustersMap) {
        V child_centroid = Util.centroid(database, child.getIDs(), child.getPreferenceVector());

        HierarchicalAxesParallelCorrelationCluster result = null;
        int resultCardinality = -1;

        BitSet childPV = child.getPreferenceVector();
        int childCardinality = childPV.cardinality();
        for (BitSet parentPV : clustersMap.keySet()) {
            int parentCardinality = parentPV.cardinality();
            if (parentCardinality >= childCardinality) {
                continue;
            }
            if (resultCardinality != -1 && parentCardinality <= resultCardinality) {
                continue;
            }

            BitSet pv = (BitSet) childPV.clone();
            pv.and(parentPV);
            if (pv.equals(parentPV)) {
                List<HierarchicalAxesParallelCorrelationCluster> parentList = clustersMap.get(parentPV);
                for (HierarchicalAxesParallelCorrelationCluster parent : parentList) {
                    V parent_centroid = Util.centroid(database, parent.getIDs(), parentPV);
                    double d = distanceFunction.weightedDistance(child_centroid, parent_centroid, parentPV);
                    if (d <= 2 * epsilon) {
                        result = parent;
                        resultCardinality = parentCardinality;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Builds the cluster hierarchy
     *
     * @param distanceFunction the distance function
     * @param clusters         the sorted list of clusters
     * @param dimensionality   the dimensionality of the data
     * @param database         the fatabase containing the data objects
     */
    private void buildHierarchy(Database<V> database,
                                DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                List<HierarchicalAxesParallelCorrelationCluster> clusters, int dimensionality) {

        StringBuffer msg = new StringBuffer();
        for (int i = 0; i < clusters.size() - 1; i++) {
            HierarchicalAxesParallelCorrelationCluster c_i = clusters.get(i);
            int subspaceDim_i = dimensionality - c_i.getLevel();
            V ci_centroid = Util.centroid(database, c_i.getIDs(), c_i.getPreferenceVector());

            for (int j = i + 1; j < clusters.size(); j++) {
                HierarchicalAxesParallelCorrelationCluster c_j = clusters.get(j);
                int subspaceDim_j = dimensionality - c_j.getLevel();

                if (subspaceDim_i < subspaceDim_j) {
                    if (debug) {
                        msg.append("\n\nl_i=").append(subspaceDim_i).append(" pv_i=[").append(Util.format(database.dimensionality(), c_i.getPreferenceVector())).append("]");
                        msg.append("\nl_j=").append(subspaceDim_j).append(" pv_j=[").append(Util.format(database.dimensionality(), c_j.getPreferenceVector())).append("]");
                    }

                    // noise level reached
                    if (c_j.getLevel() == 0) {
                        // no parents exists -> parent is noise
                        if (c_i.getParents().isEmpty()) {
                            c_j.addChild(c_i);
                            c_i.addParent(c_j);
                            if (debug) {
                                msg.append("\n").append(c_j).append(" is parent of ").append(c_i);
                            }
                        }
                    }
                    else {
                        V cj_centroid = Util.centroid(database, c_j.getIDs(), c_j.getPreferenceVector());
                        PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(ci_centroid, cj_centroid, c_i.getPreferenceVector(), c_j.getPreferenceVector());
                        double d = distanceFunction.weightedDistance(ci_centroid, cj_centroid, distance.getCommonPreferenceVector());
                        if (debug) {
                            msg.append("\ndist ").append(distance.getCorrelationValue());
                        }

                        if (distance.getCorrelationValue() == subspaceDim_j) {
                            if (d <= 2 * epsilon) {
                                // no parent exists or c_j is not a parent of the already existing parents
                                if (c_i.getParents().isEmpty() || !isParent(database, distanceFunction, c_j, c_i.getParents())) {
                                    c_j.addChild(c_i);
                                    c_i.addParent(c_j);
                                    if (debug) {
                                        msg.append("\n").append(c_j).append(" is parent of ").append(c_i);
                                    }
                                }
                            }
                            else {
                                throw new RuntimeException("Should never happen: d = " + d);
                            }
                        }
                    }
                }
            }
        }
        if (debug) {
            debugFiner(msg.toString());
        }
    }

    /**
     * Returns true, if the specified parent cluster is a parent of one child of the children clusters.
     *
     * @param database         the database containing the objects
     * @param distanceFunction the distance function for distance computation between the clusters
     * @param parent           the parent to be tested
     * @param children         the list of children to be tested
     * @return true, if the specified parent cluster is a parent of one child of the children clusters,
     *         false otherwise
     */
    private boolean isParent(Database<V> database,
                             DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                             HierarchicalAxesParallelCorrelationCluster parent,
                             List<HierarchicalAxesParallelCorrelationCluster> children) {

        V parent_centroid = Util.centroid(database, parent.getIDs(), parent.getPreferenceVector());
        int dimensionality = database.dimensionality();
        int subspaceDim_parent = dimensionality - parent.getLevel();

        for (HierarchicalAxesParallelCorrelationCluster child : children) {
            V child_centroid = Util.centroid(database, child.getIDs(), child.getPreferenceVector());
            PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(parent_centroid, child_centroid, parent.getPreferenceVector(), child.getPreferenceVector());
            if (distance.getCorrelationValue() == subspaceDim_parent) {
                return true;
            }
        }

        return false;
    }

}
