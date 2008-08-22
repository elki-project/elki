package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a result of a clustering algorithm that computes hierarchical
 * axes parallel correlation clusters from a cluster order.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Result
 * @param <D> the type of Distance used by this Result
 */
public class HierarchicalAxesParallelCorrelationClusters<V extends RealVector<V, ?>, D extends Distance<D>> extends HierarchicalClusters<HierarchicalAxesParallelCorrelationCluster, V> {
    /**
     * Indicating the preference vector of a cluster in the string representation.
     */
    public static String PREFERENCE_VECTOR = "preference vector: ";

    /**
     * The cluster order.
     */
    private ClusterOrder<V, D> clusterOrder;

    /**
     * Provides a result of a clustering algorithm that computes hierarchical
     * axes parallel correlation clusters from a cluster order.
     *
     * @param rootClusters the root cluster
     * @param db           the database containing the objects of the clusters
     * @param clusterOrder the cluster order
     */

    public HierarchicalAxesParallelCorrelationClusters(List<HierarchicalAxesParallelCorrelationCluster> rootClusters,
                                                       ClusterOrder<V, D> clusterOrder,
                                                       Database<V> db) {
        super(rootClusters, db);
        this.clusterOrder = clusterOrder;
    }

    @Override
    public void output(PrintStream outStream, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        super.output(outStream, normalization, settings);
        clusterOrder.output(outStream, normalization, settings);
    }

    @Override
    public void output(File dir, Normalization<V> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
        super.output(dir, normalization, settings);
        clusterOrder.output(new File(dir.getAbsolutePath() + File.separator + "clusterOrder"),
            normalization,
            settings);
    }

    /**
     * Writes a header for the specified cluster providing information concerning the underlying database
     * and the specified parameter-settings.
     *
     * @param out               the print stream where to write
     * @param settings          the settings to be written into the header
     * @param headerInformation additional information to be printed in the header, each entry
     *                          will be printed in one separate line
     * @param cluster           the cluster to write the header for
     */
    @Override
    protected void writeHeader(PrintStream out,
                               List<AttributeSettings> settings,
                               List<String> headerInformation,
                               HierarchicalAxesParallelCorrelationCluster cluster) {

        super.writeHeader(out, settings, headerInformation, cluster);
        BitSet preferenceVector = cluster.getPreferenceVector();
        out.println("### " + PREFERENCE_VECTOR + Util.format(getDatabase().dimensionality(), preferenceVector));
    }

    /**
     * Returns the cluster order.
     *
     * @return the cluster order
     */
    public final ClusterOrder<V, D> getClusterOrder() {
        return clusterOrder;
    }

}
