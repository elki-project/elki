package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary clustering algorithm over the partitions.
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class COPAC<V extends RealVector<V, ?>> extends COPAA<V> implements Clustering<V> {
    /**
     * Sets the specific parameters additionally to the parameters set by the
     * super-class.
     */
    public COPAC() {
        super();

        // change description and restriction class of PARTITION_ALGORITHM_PARAM
        OptionID.COPAA_PARTITION_ALGORITHM.setDescription("algorithm to apply to each partition " +
            Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
            ".");
        // noinspection unchecked
        PARTITION_ALGORITHM_PARAM.setRestrictionClass(Clustering.class);
    }

    /**
     * @see Clustering#getResult()
     */
    @Override
    public ClusteringResult<V> getResult() {
        return (ClusteringResult<V>) super.getResult();
    }

    /**
     * @see Algorithm#getDescription()
     */
    @Override
    public Description getDescription() {
        return new Description(
            "COPAC",
            "COrrelation PArtition Clustering",
            "Partitions a database according to the correlation dimension of its objects and performs a clustering algorithm over the partitions.",
            "Achtert E., B\u00F6hm C., Kriegel H.-P., Kr\u00F6ger P., Zimek A.: " +
                "Robust, Complete, and Efficient Correlation Clustering. " +
                "In Proceedings of the 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007");
    }

    /**
     * @see COPAA#runPartitionAlgorithm(de.lmu.ifi.dbs.database.Database, java.util.Map)
     */
    @Override
    protected PartitionResults<V> runPartitionAlgorithm(Database<V> database,
                                                        Map<Integer, List<Integer>> partitionMap) {
        try {
            Map<Integer, Database<V>> databasePartitions = database.partition(partitionMap,
                partitionDatabase,
                partitionDatabaseParameters);
            Map<Integer, ClusteringResult<V>> results = new Hashtable<Integer, ClusteringResult<V>>();
            Clustering<V> partitionAlgorithm = (Clustering<V>) getPartitionAlgorithm();
            for (Integer partitionID : databasePartitions.keySet()) {
                // noise partition
                if (partitionID == database.dimensionality()) {
                    Database<V> noiseDB = databasePartitions.get(partitionID);
                    Integer[][] noise = new Integer[1][noiseDB.size()];
                    int i = 0;
                    for (Iterator<Integer> it = noiseDB.iterator(); it.hasNext();) {
                        noise[0][i++] = it.next();
                    }
                    ClusteringResult<V> r = new ClustersPlusNoise<V>(noise, noiseDB);
                    results.put(partitionID, r);
                }
                else {
                    if (isVerbose()) {
                        verbose("\nRunning " +
                            partitionAlgorithm.getDescription().getShortTitle() +
                            " on partition " +
                            partitionID);
                    }
                    partitionAlgorithm.run(databasePartitions.get(partitionID));
                    results.put(partitionID, partitionAlgorithm.getResult());
                }
            }
            return new PartitionClusteringResults<V>(database,
                results,
                database.dimensionality());
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException(e);
        }
    }
}
