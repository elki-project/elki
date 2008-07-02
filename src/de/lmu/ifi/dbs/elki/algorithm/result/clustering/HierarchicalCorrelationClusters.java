package de.lmu.ifi.dbs.elki.algorithm.result.clustering;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.output.Format;

import java.io.PrintStream;
import java.util.List;

/**
 * Provides a result of a clustering algorithm that computes hierarchical
 * correlation clusters in arbitrary subspaces.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Result
 */
public class HierarchicalCorrelationClusters<V extends RealVector<V, ?>>
    extends HierarchicalClusters<HierarchicalCorrelationCluster<V>, V> {
    /**
     * Indicating the basis vectors of a cluster in the string representation.
     */
    public static String BASIS_VECTORS = "basis vectors: ";

    /**
     * Provides a result of a clustering algorithm that computes
     * correlation clusters in arbitrary subspaces.
     *
     * @param rootClusters the root clusters
     * @param db           the database containing the objects of the clusters
     */

    public HierarchicalCorrelationClusters(List<HierarchicalCorrelationCluster<V>> rootClusters,
                                           Database<V> db) {
        super(rootClusters, db);
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
                               HierarchicalCorrelationCluster<V> cluster) {

        super.writeHeader(out, settings, headerInformation, cluster);
        Matrix basisVectors = cluster.getPCA().getWeakEigenvectors();
        out.println("### " + BASIS_VECTORS);
        out.println(basisVectors.toString("### ", Format.NF8));
    }
}
