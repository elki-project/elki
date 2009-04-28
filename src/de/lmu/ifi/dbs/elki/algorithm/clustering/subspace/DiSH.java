package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.AxesModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DiSHDistanceFunction;
import de.lmu.ifi.dbs.elki.preprocessing.DiSHPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p>
 * Algorithm for detecting subspace hierarchies.
 * </p>
 * <p>Reference:
 * <br>E. Achtert, C. B&ouml;hm, H.-P. Kriegel, P. Kr&ouml;ger, I. M&uuml;ller-Gorman, A. Zimek:
 * Detection and Visualization of Subspace Cluster Hierarchies.
 * <br>In Proc. 12th International Conference  on Database Systems for Advanced Applications (DASFAA), Bangkok, Thailand, 2007.
 * </p>
 *
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class DiSH<V extends RealVector<V, ?>> extends AbstractAlgorithm<V, Clustering<AxesModel>> implements ClusteringAlgorithm<Clustering<AxesModel>,V> {
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
    private OPTICS<V, PreferenceVectorBasedCorrelationDistance> optics = new OPTICS<V, PreferenceVectorBasedCorrelationDistance>();

    /**
     * Holds the result;
     */
    private Clustering<AxesModel> result;

    /**
     * Provides the DiSH algorithm,
     * adding parameters
     * {@link #EPSILON_PARAM} and {@link #MU_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public DiSH() {
        super();
        // FIXME apparently still in debug mode??? no output???
        //this.debug = true;

        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter mu
        addOption(MU_PARAM);
    }

    /**
     * Performs the DiSH algorithm on the given database.
     *
     */
    @Override
    protected Clustering<AxesModel> runInTime(Database<V> database) throws IllegalStateException {
        if (logger.isVerbose()) {
          logger.verbose("Run OPTICS algorithm.");
        }
        optics.run(database);

        if (logger.isVerbose()) {
          logger.verbose("Compute Clusters.");
        }
        computeClusters(database, optics.getResult());
        return result;
    }

    /**
     * Returns the result of the algorithm.
     *
     * @return the result of the algorithm
     */
    public Clustering<AxesModel> getResult() {
        return result;
    }

    public Description getDescription() {
        return new Description(
            "DiSH",
            "Detecting Subspace cluster Hierarchies",
            "Algorithm to find hierarchical correlation clusters in subspaces.",
            "E. Achtert, C. B\u00F6hm, H.-P. Kriegel, P. Kr\u00F6ger, I. M\u00FCller-Gorman, A. Zimek: " +
                "Detection and Visualization of Subspace Cluster Hierarchies. " +
                "In Proc. 12th International Conference  on Database Systems for Advanced Applications (DASFAA), Bangkok, Thailand, 2007."
        );
    }

    /**
     * Calls the super method
     * and sets additionally the value of the parameters
     * {@link #EPSILON_PARAM} and {@link #MU_PARAM}.
     * Then the parameters for the algorithm {@link #optics} are set.
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // epsilon
        epsilon = EPSILON_PARAM.getValue();

        // mu
        int minpts = MU_PARAM.getValue();

        // OPTICS
        // verbose and time
        optics.setVerbose(isVerbose());
        optics.setTime(isTime());

        // todo provide setters
        // parameters for optics
        List<String> opticsParameters = new ArrayList<String>();

        // epsilon for OPTICS
        OptionUtil.addParameter(opticsParameters, OPTICS.EPSILON_ID, DiSHDistanceFunction.INFINITY_PATTERN);

        // minpts for OPTICS
        OptionUtil.addParameter(opticsParameters, OPTICS.MINPTS_ID, Integer.toString(minpts));

        // distance function
        OptionUtil.addParameter(opticsParameters, OPTICS.DISTANCE_FUNCTION_ID, DiSHDistanceFunction.class.getName());

        // epsilon for distance function
        OptionUtil.addParameter(opticsParameters, DiSHDistanceFunction.EPSILON_ID, Double.toString(epsilon));

        // omit flag
        OptionUtil.addFlag(opticsParameters, PreprocessorHandler.OMIT_PREPROCESSING_ID);

        // preprocessor
        OptionUtil.addParameter(opticsParameters, PreprocessorHandler.PREPROCESSOR_ID, DiSHPreprocessor.class.getName());

        // preprocessor epsilon
        OptionUtil.addParameter(opticsParameters, DiSHPreprocessor.EPSILON_ID, Double.toString(epsilon));

        // preprocessor minpts
        OptionUtil.addParameter(opticsParameters, DiSHPreprocessor.MINPTS_ID, Integer.toString(minpts));

        // remaining parameters
        for (String parameter : remainingParameters) {
            opticsParameters.add(parameter);
        }

        remainingParameters = optics.setParameters(opticsParameters.toArray(new String[opticsParameters.size()]));
        rememberParametersExcept(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Calls the super method
     * and adds to the returned attribute settings the attribute settings of
     * the algorithm {@link #optics}.
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
    private void computeClusters(Database<V> database, ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> clusterOrder) {
        int dimensionality = database.dimensionality();

        DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction = (DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>>) optics.getDistanceFunction();

        // extract clusters
        Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> clustersMap = extractClusters(database, distanceFunction, clusterOrder);

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer("Step 1: extract clusters");
            for (List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> clusterList : clustersMap.values()) {
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : clusterList) {
                    msg.append("\n").append(FormatUtil.format(dimensionality, c.first)).append(" ids ").append(c.second.size());
                }
            }
            logger.debugFine(msg.toString());
        }

        // check if there are clusters < minpts
        checkClusters(database, distanceFunction, clustersMap);
        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer("Step 2: check clusters");
            for (List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> clusterList : clustersMap.values()) {
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : clusterList) {
                    msg.append("\n").append(FormatUtil.format(dimensionality, c.first)).append(" ids ").append(c.second.size());
                }
            }
            logger.debugFine(msg.toString());
        }

        // actualize the levels and indices and sort the clusters
        List<Cluster<AxesModel>> clusters = sortClusters(clustersMap, dimensionality);
        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer("Step 3: sort clusters");
            for (Cluster<AxesModel> c : clusters) {
                msg.append("\n").append(FormatUtil.format(dimensionality, c.getModel().getSubspaces())).append(" ids ").append(c.getIDs().size());
            }
            logger.debugFine(msg.toString());
        }

        // build the hierarchy
        buildHierarchy(database, distanceFunction, clusters, dimensionality);
        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer("Step 4: build hierarchy");
            for (Cluster<AxesModel> c : clusters) {
                msg.append("\n").append(FormatUtil.format(dimensionality, c.getModel().getSubspaces())).append(" ids ").append(c.getIDs().size());
              for (Cluster<AxesModel> cluster : c.getParents()) {
                  msg.append("\n   parent ").append(cluster);
              }
              for (Cluster<AxesModel> cluster : c.getChildren()) {
                  msg.append("\n   child ").append(cluster);
              }
            }
            logger.debugFine(msg.toString());
        }

        int lambdaMax = dimensionality - clusters.get(clusters.size() - 1).getModel().getSubspaces().cardinality();
        result = new Clustering<AxesModel>();
        for (Cluster<AxesModel> c : clusters) {
            if (dimensionality - c.getModel().getSubspaces().cardinality() == lambdaMax) {
                result.addCluster(c);
            }
        }
        
    }

    /**
     * Extracts the clusters from the cluster order.
     *
     * @param database         the database storing the objects
     * @param distanceFunction the distance function
     * @param clusterOrder     the cluster order to extract the clusters from
     * @return the extracted clusters
     */
    private Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> extractClusters(Database<V> database,
                                                                                          DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                                                                          ClusterOrderResult<PreferenceVectorBasedCorrelationDistance> clusterOrder) {

        FiniteProgress progress = new FiniteProgress("Extract Clusters", database.size());
        int processed = 0;
        Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> clustersMap = new HashMap<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>>();
        Map<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> entryMap = new HashMap<Integer, ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>>();
        Map<Integer, Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> entryToClusterMap = new HashMap<Integer, Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>();
        for (Iterator<ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance>> it = clusterOrder.iterator(); it.hasNext();)
        {
            ClusterOrderEntry<PreferenceVectorBasedCorrelationDistance> entry = it.next();
            entryMap.put(entry.getID(), entry);

            V object = database.get(entry.getID());
            BitSet preferenceVector = entry.getReachability().getCommonPreferenceVector();

            // get the list of (parallel) clusters for the preference vector
            List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parallelClusters = clustersMap.get(preferenceVector);
            if (parallelClusters == null) {
                parallelClusters = new ArrayList<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>();
                clustersMap.put(preferenceVector, parallelClusters);
            }

            // look for the proper cluster
            Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> cluster = null;
            for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : parallelClusters) {
                V c_centroid = DatabaseUtil.centroid(database, c.second.getIDs(), c.first);
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
                cluster = new Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>(preferenceVector, new DatabaseObjectGroupCollection<List<Integer>>(new ArrayList<Integer>()));
                parallelClusters.add(cluster);
            }
            cluster.second.ids.add(entry.getID());
            entryToClusterMap.put(entry.getID(), cluster);

            if (logger.isVerbose()) {
                progress.setProcessed(++processed);
                logger.progress(progress);
            }
        }

        if (logger.isDebugging()) {
            StringBuffer msg = new StringBuffer("Step 0");
            for (List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> clusterList : clustersMap.values()) {
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : clusterList) {
                    msg.append("\n").append(FormatUtil.format(database.dimensionality(), c.first)).append(" ids ").append(c.second.size());
                }
            }
            logger.debugFine(msg.toString());
        }

        // add the predecessor to the cluster
        for (BitSet pv : clustersMap.keySet()) {
            List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parallelClusters = clustersMap.get(pv);
            for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> cluster : parallelClusters) {
                if (cluster.second.getIDs().isEmpty()) {
                    continue;
                }
                Integer firstID = cluster.second.getIDs().get(0);
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

                Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> oldCluster = entryToClusterMap.get(predecessorID);
                oldCluster.second.ids.remove(predecessorID);
                cluster.second.ids.add(predecessorID);
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
    private List<Cluster<AxesModel>> sortClusters(Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> clustersMap, int dimensionality) {
        // actualize the levels and indices
        List<Cluster<AxesModel>> clusters = new ArrayList<Cluster<AxesModel>>();
        for (BitSet pv : clustersMap.keySet()) {
            List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parallelClusters = clustersMap.get(pv);
            for (int i = 0; i < parallelClusters.size(); i++) {
              Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c = parallelClusters.get(i);
              // TODO: re-add levels?
              clusters.add(new Cluster<AxesModel>(c.second,new AxesModel(c.first)));
            }
        }
        // TODO: re-add sorting.
        //Collections.sort(clusters);
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
                               Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> clustersMap) {

        // check if there are clusters < minpts
        // and add them to not assigned
        //noinspection unchecked
        int minpts = distanceFunction.getPreprocessor().getMinpts();
        List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> notAssigned = new ArrayList<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>();
        Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> newClustersMap = new HashMap<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>>();
        Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> noise = new Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>(new BitSet(), new DatabaseObjectGroupCollection<List<Integer>>(new ArrayList<Integer>()));
        for (BitSet pv : clustersMap.keySet()) {
            // noise
            if (pv.cardinality() == 0) {
                List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parallelClusters = clustersMap.get(pv);
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : parallelClusters) {
                    noise.second.ids.addAll(c.second.getIDs());
                }
            }
            // clusters
            else {
                List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parallelClusters = clustersMap.get(pv);
                List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> newParallelClusters = new ArrayList<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>(parallelClusters.size());
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : parallelClusters) {
                    if (!pv.equals(new BitSet()) && c.second.size() < minpts) {
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

        for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> c : notAssigned) {
            if (c.second.getIDs().isEmpty()) {
                continue;
            }
            Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> parent = findParent(database, distanceFunction, c, clustersMap);
            if (parent != null) {
                parent.second.ids.addAll(c.second.getIDs());
            }
            else {
                noise.second.ids.addAll(c.second.getIDs());
            }
        }

        List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> noiseList = new ArrayList<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>(1);
        noiseList.add(noise);
        clustersMap.put(noise.first, noiseList);
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
    private Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> findParent(Database<V> database,
                                                                  DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                                                  Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> child,
                                                                  Map<BitSet, List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>>> clustersMap) {
        V child_centroid = DatabaseUtil.centroid(database, child.second.getIDs(), child.first);

        Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> result = null;
        int resultCardinality = -1;

        BitSet childPV = child.first;
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
                List<Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>>> parentList = clustersMap.get(parentPV);
                for (Pair<BitSet, DatabaseObjectGroupCollection<List<Integer>>> parent : parentList) {
                    V parent_centroid = DatabaseUtil.centroid(database, parent.second.getIDs(), parentPV);
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
     * @param database         the database containing the data objects
     */
    private void buildHierarchy(Database<V> database,
                                DiSHDistanceFunction<V, DiSHPreprocessor<V, ?>> distanceFunction,
                                List<Cluster<AxesModel>> clusters, int dimensionality) {

        StringBuffer msg = new StringBuffer();
        for (int i = 0; i < clusters.size() - 1; i++) {
            Cluster<AxesModel> c_i = clusters.get(i);
            int subspaceDim_i = dimensionality - c_i.getModel().getSubspaces().cardinality();
            V ci_centroid = DatabaseUtil.centroid(database, c_i.getIDs(), c_i.getModel().getSubspaces());

            for (int j = i + 1; j < clusters.size(); j++) {
                Cluster<AxesModel> c_j = clusters.get(j);
                int subspaceDim_j = dimensionality - c_j.getModel().getSubspaces().cardinality();

                if (subspaceDim_i < subspaceDim_j) {
                    if (logger.isDebuggingFiner()) {
                        msg.append("\n\nl_i=").append(subspaceDim_i).append(" pv_i=[").append(FormatUtil.format(database.dimensionality(), c_i.getModel().getSubspaces())).append("]");
                        msg.append("\nl_j=").append(subspaceDim_j).append(" pv_j=[").append(FormatUtil.format(database.dimensionality(), c_j.getModel().getSubspaces())).append("]");
                    }

                    // noise level reached
                    if (c_j.getModel().getSubspaces().cardinality() == 0) {
                        // no parents exists -> parent is noise
                        if (c_i.getParents().isEmpty()) {
                            c_j.getChildren().add(c_i);
                            c_i.getParents().add(c_j);
                            if (logger.isDebuggingFiner()) {
                                msg.append("\n").append(c_j).append(" is parent of ").append(c_i);
                            }
                        }
                    }
                    else {
                        V cj_centroid = DatabaseUtil.centroid(database, c_j.getIDs(), c_j.getModel().getSubspaces());
                        PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(ci_centroid, cj_centroid, c_i.getModel().getSubspaces(), c_j.getModel().getSubspaces());
                        double d = distanceFunction.weightedDistance(ci_centroid, cj_centroid, distance.getCommonPreferenceVector());
                        if (logger.isDebuggingFiner()) {
                            msg.append("\ndist ").append(distance.getCorrelationValue());
                        }

                        if (distance.getCorrelationValue() == subspaceDim_j) {
                            if (d <= 2 * epsilon) {
                                // no parent exists or c_j is not a parent of the already existing parents
                                if (c_i.getParents().isEmpty() || !isParent(database, distanceFunction, c_j, c_i.getParents())) {
                                    c_j.getChildren().add(c_i);
                                    c_i.getParents().add(c_j);
                                    if (logger.isDebuggingFiner()) {
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
        if (logger.isDebuggingFiner()) {
          logger.debugFiner(msg.toString());
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
                             Cluster<AxesModel> parent,
                             List<Cluster<AxesModel>> children) {

        V parent_centroid = DatabaseUtil.centroid(database, parent.getIDs(), parent.getModel().getSubspaces());
        int dimensionality = database.dimensionality();
        int subspaceDim_parent = dimensionality - parent.getModel().getSubspaces().cardinality();

        for (Cluster<AxesModel> child : children) {
            V child_centroid = DatabaseUtil.centroid(database, child.getIDs(), child.getModel().getSubspaces());
            PreferenceVectorBasedCorrelationDistance distance = distanceFunction.correlationDistance(parent_centroid, child_centroid, parent.getModel().getSubspaces(), child.getModel().getSubspaces());
            if (distance.getCorrelationValue() == subspaceDim_parent) {
                return true;
            }
        }

        return false;
    }

}
