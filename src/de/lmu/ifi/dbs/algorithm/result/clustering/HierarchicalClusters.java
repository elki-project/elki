package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides a result of a clustering algorithm that computes hierarchical
 * clusters.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HierarchicalClusters<C extends HierarchicalCluster<C>, O extends DatabaseObject> extends AbstractResult<O> {
  /**
   * Indicating the children of a cluster in the string representation.
   */
  public static String CHILDREN = "children: ";

  /**
   * Indicating the parents of a cluster in the string representation.
   */
  public static String PARENTS = "parents: ";

  /**
   * Indicating the level of a cluster in the string representation.
   */
  public static String LEVEL = "level: ";

  /**
   * Indicating the index within the level of a cluster in the string representation.
   */
  public static String LEVEL_INDEX = "level index: ";

  /**
   * The root cluster.
   */
  private C rootCluster;

  /**
   * Provides a result of a clustering algorithm that computes hierarchical
   * clusters from a cluster order.
   *
   * @param rootCluster the root cluster
   * @param db          the database containing the objects of the clusters
   */
  public HierarchicalClusters(C rootCluster, Database<O> db) {
    super(db);
    this.rootCluster = rootCluster;
  }

  /**
   * Writes the cluster order to the given stream.
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
    try {
      write(rootCluster, null, outStream, normalization, settings, new HashMap<C, Boolean>());
    }
    catch (FileNotFoundException e) {
      throw new UnableToComplyException(e);
    }
    catch (NonNumericFeaturesException e) {
      throw new UnableToComplyException(e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(File dir,
                     Normalization<O> normalization,
                     List<AttributeSettings> settings) throws UnableToComplyException {

    dir.mkdirs();
    try {
      File outFile = new File(dir.getAbsolutePath() + File.separator + rootCluster.toString());
      PrintStream outStream = new PrintStream(new FileOutputStream(outFile, false));
      write(rootCluster, dir, outStream, normalization, settings, new HashMap<C, Boolean>());
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
   * @param written       the already written clusters
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vector is not compatible with values initialized
   *          during normalization
   */
  private void write(C cluster,
                     File dir,
                     PrintStream out,
                     Normalization<O> normalization,
                     List<AttributeSettings> settings,
                     Map<C, Boolean> written) throws NonNumericFeaturesException, FileNotFoundException {

    writeHeader(out, settings, null, cluster);
    out.print("### " + HierarchicalClusters.CHILDREN);
    for (int i = 0; i < cluster.numChildren(); i++) {
      C c = cluster.getChild(i);
      out.print(c);
      if (i < cluster.getChildren().size() - 1) {
        out.print(":");
      }

    }
    out.println();
    out.print("### " + HierarchicalClusters.PARENTS);
    for (int i = 0; i < cluster.numParents(); i++) {
      C c = cluster.getParent(i);
      out.print(c);
      if (i < cluster.getParents().size() - 1) {
        out.print(":");
      }
    }
    out.println();
    out.println("### " + HierarchicalClusters.LEVEL + cluster.getLevel());
    out.println("### " + HierarchicalClusters.LEVEL_INDEX + cluster.getLevelIndex());
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
    List<C> children = cluster.getChildren();
    for (C child : children) {
      Boolean done = written.get(child);
      if (done != null && done) {
        continue;
      }

      if (dir != null) {
        File outFile = new File(dir.getAbsolutePath() + File.separator + child.toString());
        PrintStream outStream = new PrintStream(new FileOutputStream(outFile, false), true);
        write(child, dir, outStream, normalization, settings, written);
      }
      else {
        write(child, dir, out, normalization, settings, written);
      }
    }
  }

  /**
   * Returns the root cluster.
   *
   * @return the root cluster
   */
  public final C getRootCluster() {
    return rootCluster;
  }

  /**
   * Returns a breadth first enumeration over the clusters.
   *
   * @return a breadth first enumeration over the clusters
   */
  public final BreadthFirstEnumeration<C> breadthFirstEnumeration() {
    return new BreadthFirstEnumeration<C>(rootCluster);
  }

  /**
   * Writes a header for the specified cluster providing information concerning the underlying database
   * and the specified parameter-settings. Subclasses may need to overwrite this method.
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
                             C cluster) {
    writeHeader(out, settings, headerInformation);
  }

}
