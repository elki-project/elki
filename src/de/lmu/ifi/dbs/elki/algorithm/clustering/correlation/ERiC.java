package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.result.Result;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalCorrelationCluster;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.HierarchicalCorrelationClusters;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.BitDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ERiCDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.varianceanalysis.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.elki.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.elki.varianceanalysis.LocalPCA;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Performs correlation clustering on the data
 * partitioned according to local correlation dimensionality and builds
 * a hierarchy of correlation clusters that allows multiple inheritance from the clustering result.
 * <p>Reference:
 * E. Achtert, C. Boehm, H.-P. Kriegel, P. Kröger, and A. Zimek:
 * On Exploring Complex Relationships of Correlation Clusters.
 * <br>In Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007.
 * </p>
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
        copacAlgorithm = new COPAC<V>();
    }

    /**
     * Performs the ERiC algorithm on the given database.
     *
     * @see de.lmu.ifi.dbs.elki.algorithm.Algorithm#run(de.lmu.ifi.dbs.elki.database.Database)
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

    @Override
    public String description() {
        StringBuilder description = new StringBuilder();
        description.append(super.description());
        description.append('\n');
        description.append(ERiC.class.getName());
        description.append(" requires parametrization of underlying partitioning algorithm:\n");
        description.append(copacAlgorithm.description());
        return description.toString();
    }

    /**
     * Returns a description of the algorithm.
     *
     * @return a description of the algorithm
     */
    public Description getDescription() {
        return new Description(
            "ERiC",
            "Exploring Relationships among Correlation Clusters",
            "Performs the DBSCAN algorithm on the data using a special distance function taking into account correlations among attributes and builds " +
                "a hierarchy that allows multiple inheritance from the correlation clustering result.",
            "E. Achtert, C. B\u00F6hm, H.-P. Kriegel, P. Kr\u00F6ger, and A. Zimek: " +
                "On Exploring Complex Relationships of Correlation Clusters. " +
                "In Proc. 19th International Conference on Scientific and Statistical Database Management (SSDBM 2007), Banff, Canada, 2007");
    }

    /**
     * Passes remaining parameters to the clustering algorithm.
     *
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // copac algorithm
        String[] copacAlgorithmParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, copacAlgorithmParameters, 0, remainingParameters.length);
        copacAlgorithm.setVerbose(isVerbose());
        copacAlgorithm.setTime(isTime());
        remainingParameters = copacAlgorithm.setParameters(copacAlgorithmParameters);
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
        Util.addParameter(parameters, OptionID.PCA_EIGENPAIR_FILTER, FirstNEigenPairFilter.class.getName());

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
