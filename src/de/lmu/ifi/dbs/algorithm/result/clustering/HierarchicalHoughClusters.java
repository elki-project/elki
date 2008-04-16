package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;
import de.lmu.ifi.dbs.algorithm.clustering.cash.HoughInterval;
import de.lmu.ifi.dbs.database.Database;

import java.util.List;
import java.io.PrintStream;

/**
 * Provides a result of a hough based clustering algorithm that computes hierarchical
 * correlation clusters in arbitrary subspaces.
 *
 * @author Elke Achtert
 */
public class HierarchicalHoughClusters extends HierarchicalClusters<HierarchicalHoughCluster<ParameterizationFunction>, ParameterizationFunction> {
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

  public HierarchicalHoughClusters(HierarchicalHoughCluster<ParameterizationFunction> rootCluster,
                                   Database<ParameterizationFunction> db) {
    super(rootCluster, db);
  }

  /**
   * Returns the root cluster.
   * @return the root cluster
   */
  public HierarchicalHoughCluster<ParameterizationFunction> getRootCluster() {
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
                             HierarchicalHoughCluster<ParameterizationFunction> cluster) {

    super.writeHeader(out, settings, headerInformation, cluster);
    HoughInterval interval = cluster.getInterval();
    out.println("### " + INTERVAL);
    out.println(interval.toString("### ", Format.NF8));
  }
}
