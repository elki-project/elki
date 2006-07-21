package de.lmu.ifi.dbs.algorithm.result;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

/**
 * // todo comments ueberarbeiten
 * Provides a result of a clustering-algorithm that computes hierarchical
 * clusters and a preference vectors for each cluster.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalClusters<O extends RealVector, D extends Distance<D>> extends AbstractResult<O> {
  public static String PREFERENCE_VECTOR = "preference vector: ";
  public static String CHILDREN = "children: ";
  public static String PARENTS = "parents: ";
  public static String LEVEL = "level: ";
  public static String LEVEL_INDEX = "level index: ";

  /**
   * The root cluster.
   */
  private HierarchicalCluster rootCluster;

  /**
   * The cluster order.
   */
  private ClusterOrder<O, D> clusterOrder;

  /**
   * Provides a result of a clustering-algorithm that computes several
   * clusters and remaining noise and a preference vectors for each cluster and noise.
   *
   * @param rootCluster  the root cluster
   * @param db           the database containing the objects of clusters
   * @param clusterOrder the cluster order
   */
  public HierarchicalClusters(HierarchicalCluster rootCluster,
                              ClusterOrder<O, D> clusterOrder,
                              Database<O> db) {
    super(db);
    this.rootCluster = rootCluster;
    this.clusterOrder = clusterOrder;
  }

  /**
   * Writes the clustering result to the given stream.
   *
   * @param outStream     the stream to write to
   * @param normalization Normalization to restore original values according to, if this action is supported
   *                      - may remain null.
   * @param settings      the settings to be written into the header, if this parameter is <code>null</code>,
   *                      no header will be written
   * @throws de.lmu.ifi.dbs.utilities.UnableToComplyException
   *          if any feature vector is not compatible with values initialized during normalization
   */
  public void output(PrintStream outStream, Normalization<O> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    clusterOrder.output(outStream, normalization, settings);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(File dir,
                     Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {

    dir.mkdirs();
    clusterOrder.output(new File(dir.getAbsolutePath() + File.separator + "clusterOrder"),
                        normalization,
                        settings);

    try {
      File outFile = new File(dir.getAbsolutePath() + File.separator + rootCluster.toString());
      PrintStream outStream = new PrintStream(new FileOutputStream(outFile));
      write(rootCluster, dir, outStream, normalization, settings, new HashMap<HierarchicalCluster, Boolean>());
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
  }

  /**
   * Writes a cluster to the designated print stream.
   *
   * @param cluster       the cluster to be written
   * @param dir           the directory where to write
   * @param normalization a Normalization to restore original values for output - may
   *                      remain null
   * @param settings      the settings to be written into the header
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vector is not compatible with values initialized
   *          during normalization
   */
  private void write(HierarchicalCluster cluster,
                     File dir,
                     PrintStream out,
                     Normalization<O> normalization,
                     List<AttributeSettings> settings,
                     Map<HierarchicalCluster, Boolean> written) throws NonNumericFeaturesException, FileNotFoundException {

    BitSet preferenceVector = cluster.getPreferenceVector();
    writeHeader(out, settings, null);
    out.println("### " + PREFERENCE_VECTOR + Util.format(getDatabase().dimensionality(), preferenceVector));
    out.print("### " + CHILDREN);
    for (int i = 0; i < cluster.getChildren().size(); i++) {
      HierarchicalCluster c = cluster.getChildren().get(i);
      out.print(c);
      if (i < cluster.getChildren().size() - 1)
        out.print(":");

    }
    out.println();    
    out.print("### " + PARENTS);
    for (int i = 0; i < cluster.getParents().size(); i++) {
      HierarchicalCluster c = cluster.getParents().get(i);
      out.print(c);
      if (i < cluster.getParents().size() - 1)
        out.print(":");
    }
    out.println();
    out.println("### " + LEVEL + cluster.getLevel());
    out.println("### " + LEVEL_INDEX + cluster.getLevelIndex());
    out.println("################################################################################");

    List<Integer> ids = cluster.getIDs();
    for (Integer id : ids) {
      O v = db.get(id);
      if (normalization != null) {
        v = normalization.restore(v);
      }
      out.println(v.toString()
                  + SEPARATOR
                  + db.getAssociation(AssociationID.LABEL, id));
    }
    out.flush();
    written.put(cluster, true);

    // write the children
    List<HierarchicalCluster> children = cluster.getChildren();
    for (HierarchicalCluster child : children) {
      Boolean done = written.get(child);
      if (done != null && done) continue;


      File outFile = new File(dir.getAbsolutePath() + File.separator + child.toString());
      outFile.getParentFile().mkdirs();
      PrintStream outStream = new PrintStream(new FileOutputStream(outFile, true), true);
      write(child, dir, outStream, normalization, settings, written);
    }
  }

  /**
   * Returns the root cluster.
   *
   * @return the root cluster
   */
  public HierarchicalCluster getRootCluster() {
    return rootCluster;
  }

  /**
   * Returns the cluster order.
   *
   * @return the cluster order
   */
  public ClusterOrder<O, D> getClusterOrder() {
    return clusterOrder;
  }
}
