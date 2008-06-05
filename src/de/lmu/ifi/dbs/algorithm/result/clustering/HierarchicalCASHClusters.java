package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.clustering.cash.CASHInterval;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.PrintStream;
import java.util.List;

/**
 * Provides a result of the CASH clustering algorithm that computes hierarchical
 * correlation clusters in arbitrary subspaces.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalCASHClusters extends HierarchicalClusters<HierarchicalCASHCluster, ParameterizationFunction> {
    /**
     * Indicating the interval of a cluster in the string representation.
     */
    public static String INTERVAL = "interval: ";

    /**
     * Provides a result of a clustering algorithm that computes
     * correlation clusters in arbitrary subspaces.
     *
     * @param rootCluster the root cluster
     * @param db          the database containing the objects of the clusters
     */

    public HierarchicalCASHClusters(HierarchicalCASHCluster rootCluster,
                                    Database<ParameterizationFunction> db) {
        super(rootCluster, db);
    }

    /**
     * Returns the root cluster.
     *
     * @return the root cluster
     */
    public HierarchicalCASHCluster getRootCluster() {
        return getRootClusters().get(0);
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
                               HierarchicalCASHCluster cluster) {

        super.writeHeader(out, settings, headerInformation, cluster);
        CASHInterval interval = cluster.getInterval();
        out.println("### " + INTERVAL);
        out.println(interval.toString("### ", Format.NF8));
    }
}
