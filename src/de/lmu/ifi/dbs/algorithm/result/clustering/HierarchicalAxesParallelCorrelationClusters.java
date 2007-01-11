package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.normalization.Normalization;

import java.io.PrintStream;
import java.io.File;
import java.util.BitSet;
import java.util.List;

/**
 * Provides a result of a clustering algorithm that computes hierarchical
 * axes parallel correlation clusters from a cluster order.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalAxesParallelCorrelationClusters<O extends RealVector, D extends Distance<D>> extends HierarchicalClusters<HierarchicalAxesParallelCorrelationCluster, O> {
  /**
   * Indicating the preference vector of a cluster in the string representation.
   */
  public static String PREFERENCE_VECTOR = "preference vector: ";

  /**
   * The cluster order.
   */
  private ClusterOrder<O, D> clusterOrder;

  /**
   * Provides a result of a clustering algorithm that computes hierarchical
   * axes parallel correlation clusters from a cluster order.
   *
   * @param rootClusters  the root cluster
   * @param db           the database containing the objects of the clusters
   * @param clusterOrder the cluster order
   */

  public HierarchicalAxesParallelCorrelationClusters(List<HierarchicalAxesParallelCorrelationCluster> rootClusters,
                                                     ClusterOrder<O, D> clusterOrder,
                                                     Database<O> db) {
    super(rootClusters, db);
    this.clusterOrder = clusterOrder;
  }

  /**
   * @see  de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.PrintStream, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    super.output(outStream, normalization, settings);
    clusterOrder.output(outStream, normalization, settings);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(File dir, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
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
  public final ClusterOrder<O, D> getClusterOrder() {
    return clusterOrder;
  }

}
