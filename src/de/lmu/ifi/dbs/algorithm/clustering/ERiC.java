package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalCorrelationCluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalCorrelationClusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.BitDistance;
import de.lmu.ifi.dbs.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.GlobalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.*;

/**
 * Performs correlation clustering on the data
 * partitioned according to local correlation dimensionality and builds
 * a hierarchy of correlation clusters that allows multiple inheritance from the clustering result.
 *
 * @author Elke Achtert
 * @param <V> the type of Realvector handled by this Algorithm
 */
public class ERiC<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {
    /**
     * The COPAC clustering algorithm.
     */
    private COPAC<V> copacAlgorithm;

    /**
     * Holds the result.
     */
    private HierarchicalCorrelationClusters<V> result;

    /**
     * Performs the COPAC algorithm on the data and builds
     * a hierarchy of correlation clusters that allows multiple inheritance from the clustering result.
     */
    public ERiC() {
//    this.debug = true;
    }

    /**
     * The run method encapsulated in measure of runtime. An extending class
     * needs not to take care of runtime itself.
     *
     * @param database the database to run the algorithm on
     * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
     *                               setParameters(String[]) method has been failed to be called).
     */
    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException {
        int dimensionality = database.dimensionality();

        // run COPAC
        if (isVerbose()) {
            verbose("Step 1: Preprocessing local correlation dimensionalities and partitioning data...");
        }
        copacAlgorithm.run(database);

        // extract correlation clusters
        if (isVerbose()) {
            verbose("\nStep 2: Extract correlation clusters...");
        }
        SortedMap<Integer, List<HierarchicalCorrelationCluster<V>>> clusterMap = extractCorrelationClusters(database, dimensionality);
        if (this.debug) {
            StringBuffer msg = new StringBuffer("\n\nStep 2: Extract correlation clusters...");
            for (Integer corrDim : clusterMap.keySet()) {
                List<HierarchicalCorrelationCluster<V>> correlationClusters = clusterMap.get(corrDim);
                msg.append("\n\ncorrDim ").append(corrDim);
                for (HierarchicalCorrelationCluster<V> cluster : correlationClusters) {
                    msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size()).append(", level: ").append(cluster.getLevel()).append(", index: ").append(cluster.getLevelIndex());
//          msg.append("\n  basis " + cluster.getPCA().getWeakEigenvectors().toString("    ", NF) + "  ids " + cluster.getIDs().size());
                }
            }
            debugFine(msg.toString());
        }
        if (isVerbose()) {
            int clusters = 0;
            for (List<HierarchicalCorrelationCluster<V>> correlationClusters : clusterMap.values()) {
                clusters += correlationClusters.size();
            }
            verbose(clusters + " clusters extracted.");
        }

        // build hierarchy
        if (isVerbose()) {
            verbose("\nStep 3: Build hierarchy...");
        }
        buildHierarchy(dimensionality, clusterMap);
        if (this.debug) {
            StringBuffer msg = new StringBuffer("\n\nStep 3: Build hierarchy");
            for (Integer corrDim : clusterMap.keySet()) {
                List<HierarchicalCorrelationCluster<V>> correlationClusters = clusterMap.get(corrDim);
                for (HierarchicalCorrelationCluster<V> cluster : correlationClusters) {
                    msg.append("\n  cluster ").append(cluster).append(", ids: ").append(cluster.getIDs().size()).append(", level: ").append(cluster.getLevel()).append(", index: ").append(cluster.getLevelIndex());
                    for (int i = 0; i < cluster.getParents().size(); i++) {
                        msg.append("\n   parent ").append(cluster.getParents().get(i));
                    }
                    for (int i = 0; i < cluster.numChildren(); i++) {
                        msg.append("\n   child ").append(cluster.getChild(i));
                    }
                }
            }
            debugFine(msg.toString());
        }


        List<HierarchicalCorrelationCluster<V>> rootClusters = clusterMap.get(clusterMap.lastKey());
        result = new HierarchicalCorrelationClusters<V>(rootClusters, database);
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
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription() {
        return new Description(
            "ERiC",
            "ERiC",
            "Performs the DBSCAN algorithm on the data using a special distance function and builds " +
                "a hierarchy that allows multiple inheritance from the clustering result.",
            "E. Achtert, C. B\u00F6hm, H.-P. Kriegel, P. Kr\u00F6ger, and A. Zimek: On Exploring Complex Relkationships of Correlation Clusters. In Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007");
    }

    /**
     * Passes remaining parameters to the clustering algorithm.
     *
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // copac algorithm
        copacAlgorithm = new COPAC<V>();
        remainingParameters = copacAlgorithm.setParameters(remainingParameters);
        copacAlgorithm.setTime(isTime());
        copacAlgorithm.setVerbose(isVerbose());
        setParameters(args, remainingParameters);

        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> attributeSettings = super.getAttributeSettings();
        attributeSettings.addAll(copacAlgorithm.getAttributeSettings());
        return attributeSettings;
    }

    /**
     * Sets whether the time should be assessed.
     *
     * @param time whether the time should be assessed
     */
    @Override
    public void setTime(boolean time) {
        super.setTime(time);
        copacAlgorithm.setTime(time);
    }

    /**
     * Sets whether verbose messages should be printed while executing the
     * algorithm.
     *
     * @param verbose whether verbose messages should be printed while executing the
     *                algorithm
     */
    @Override
    public void setVerbose(boolean verbose) {
        super.setVerbose(verbose);
        copacAlgorithm.setVerbose(verbose);
    }

    /**
     * Extracts the correlation clusters and noise from the copac result
     * and returns a mapping of correlation dimension to maps of clusters within
     * this correlation dimension. Each cluster is defined by the basis vectors defining
     * the subspace in which the cluster appears.
     *
     * @param database       the database containing the objects
     * @param dimensionality the dimensionality of the feature space
     * @return a mapping of correlation dimension to maps of clusters
     */
    private SortedMap<Integer, List<HierarchicalCorrelationCluster<V>>> extractCorrelationClusters(Database<V> database,
                                                                                                   int dimensionality) {
        try {
            // result
            SortedMap<Integer, List<HierarchicalCorrelationCluster<V>>> clusterMap = new TreeMap<Integer, List<HierarchicalCorrelationCluster<V>>>();

            // result of COPAC algorithm
            PartitionClusteringResults<V> copacResult = (PartitionClusteringResults<V>) copacAlgorithm.getResult();
            Integer noiseDimension = copacResult.getNoiseID();
            // noise cluster containing all noise objects over all partitions
            Set<Integer> noiseIDs = new HashSet<Integer>();

            // iterate over correlation dimensions
            for (Iterator<Integer> it = copacResult.partitionsIterator(); it.hasNext();) {
                Integer correlationDimension = it.next();
                ClusteringResult<V> clusteringResult = copacResult.getResult(correlationDimension);

                // clusters and noise within one corrDim
                if (noiseDimension == null || correlationDimension != noiseDimension) {
                    List<HierarchicalCorrelationCluster<V>> correlationClusters = new ArrayList<HierarchicalCorrelationCluster<V>>();
                    clusterMap.put(correlationDimension, correlationClusters);
                    Map<SimpleClassLabel, Database<V>> clustering = clusteringResult.clustering(SimpleClassLabel.class);
                    // clusters
                    for (Database<V> db : clustering.values()) {
                        // run pca
                        LocalPCA<V> pca = new LinearLocalPCA<V>();
                        pca.setParameters(pcaParameters(correlationDimension));
                        pca.run(Util.getDatabaseIDs(db), db);
                        // create a new correlation cluster
                        Set<Integer> ids = new HashSet<Integer>(Util.getDatabaseIDs(db));
                        HierarchicalCorrelationCluster<V> correlationCluster = new HierarchicalCorrelationCluster<V>(pca, ids,
                            "[" + correlationDimension + "_" + correlationClusters.size() + "]",
                            dimensionality - correlationDimension,
                            correlationClusters.size());
                        // put cluster to result
                        correlationClusters.add(correlationCluster);
                    }
                    // noise
                    Database<V> noiseDB = clusteringResult.noise();
                    noiseIDs.addAll(Util.getDatabaseIDs(noiseDB));
                }

                // partition containing noise
                else {
                    // clusters in noise partition
                    Map<SimpleClassLabel, Database<V>> clustering = clusteringResult.clustering(SimpleClassLabel.class);
                    for (Database<V> db : clustering.values()) {
                        noiseIDs.addAll(Util.getDatabaseIDs(db));
                    }
                    // noise in noise partition
                    Database<V> noiseDB = clusteringResult.noise();
                    noiseIDs.addAll(Util.getDatabaseIDs(noiseDB));
                }
            }

            // create noise cluster containing all noise objects over all partitions
            if (!noiseIDs.isEmpty()) {
                LocalPCA<V> pca = new LinearLocalPCA<V>();
                pca.setParameters(pcaParameters(dimensionality));
                pca.run(noiseIDs, database);
                List<HierarchicalCorrelationCluster<V>> noiseClusters = new ArrayList<HierarchicalCorrelationCluster<V>>();
                HierarchicalCorrelationCluster<V> noiseCluster = new HierarchicalCorrelationCluster<V>(pca,
                    noiseIDs,
                    "[noise]", 0, 0);
                noiseClusters.add(noiseCluster);
                clusterMap.put(dimensionality, noiseClusters);
            }

            // set the centroids
            for (List<HierarchicalCorrelationCluster<V>> correlationClusters : clusterMap.values()) {
                for (HierarchicalCorrelationCluster<V> cluster : correlationClusters) {
                    cluster.setCentroid(Util.centroid(database, cluster.getIDs()));
                }
            }

            return clusterMap;
        }

        catch (ParameterException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the parameters for the PCA for the specified correlation dimension.
     *
     * @param correlationDimension the correlation dimension
     * @return the parameters for the PCA for the specified correlation dimension
     */
    private String[] pcaParameters(int correlationDimension) {
        List<String> parameters = new ArrayList<String>();

        // eigenpair filter
        parameters.add(OptionHandler.OPTION_PREFIX + GlobalPCA.EIGENPAIR_FILTER_PARAM.getName());
        parameters.add(FirstNEigenPairFilter.class.getName());

        // n
        parameters.add(OptionHandler.OPTION_PREFIX + FirstNEigenPairFilter.N_P);
        parameters.add(Integer.toString(correlationDimension));

        return parameters.toArray(new String[parameters.size()]);
    }

    private void buildHierarchy(int dimensionality,
                                SortedMap<Integer, List<HierarchicalCorrelationCluster<V>>> clusterMap) {

        StringBuffer msg = new StringBuffer();

        DBSCAN dbscan = (DBSCAN) copacAlgorithm.getPartitionAlgorithm();
        ERiCDistanceFunction distanceFunction = (ERiCDistanceFunction) dbscan.getDistanceFunction();
        Integer lambda_max = clusterMap.lastKey();

        for (Integer childCorrDim : clusterMap.keySet()) {
            List<HierarchicalCorrelationCluster<V>> children = clusterMap.get(childCorrDim);
            SortedMap<Integer, List<HierarchicalCorrelationCluster<V>>> parentMap = clusterMap.tailMap(childCorrDim + 1);
            if (debug) {
                msg.append("\n\ncorrdim ").append(childCorrDim);
                msg.append("\nparents ").append(parentMap.keySet());
            }

            for (HierarchicalCorrelationCluster<V> child : children) {
                for (Integer parentCorrDim : parentMap.keySet()) {
                    List<HierarchicalCorrelationCluster<V>> parents = parentMap.get(parentCorrDim);
                    for (HierarchicalCorrelationCluster<V> parent : parents) {
                        int subspaceDim_parent = dimensionality - parent.getLevel();
                        if (subspaceDim_parent == lambda_max && child.getParents().isEmpty()) {
                            parent.addChild(child);
                            child.addParent(parent);
                            if (debug) {
                                msg.append("\n").append(parent).append(" is parent of ").append(child);
                            }
                        }
                        else {
                            //noinspection unchecked
                            BitDistance dist = distanceFunction.distance(parent.getCentroid(), child.getCentroid(), parent.getPCA(), child.getPCA());
                            if (!dist.isBit() && (child.getParents().isEmpty() ||
                                !isParent(distanceFunction, parent, child.getParents()))) {
                                parent.addChild(child);
                                child.addParent(parent);
                                if (debug) {
                                    msg.append("\n").append(parent).append(" is parent of ").append(child);
                                }
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
     * @param distanceFunction the distance function for distance computation between the clusters
     * @param parent           the parent to be tested
     * @param children         the list of children to be tested
     * @return true, if the specified parent cluster is a parent of one child of the children clusters,
     *         false otherwise
     */
    private boolean isParent(ERiCDistanceFunction distanceFunction,
                             HierarchicalCorrelationCluster<V> parent,
                             List<HierarchicalCorrelationCluster<V>> children) {

        StringBuffer msg = new StringBuffer();

        for (HierarchicalCorrelationCluster<V> child : children) {
            if (parent.getPCA().getCorrelationDimension() == child.getPCA().getCorrelationDimension())
                return false;

            //noinspection unchecked
            BitDistance dist = distanceFunction.distance(parent.getCentroid(), child.getCentroid(), parent.getPCA(), child.getPCA());
            if (debug) {
                msg.append("\ndist(").append(child).append(" - ").append(parent).append(") = ").append(dist);

            }
            if (!dist.isBit()) {
                if (debug) {
                    debugFine(msg.toString());
                }
                return true;
            }
        }

        if (debug) {
            debugFiner(msg.toString());
        }
        return false;
    }
}
