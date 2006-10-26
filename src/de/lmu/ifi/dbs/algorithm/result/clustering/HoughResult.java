package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileDescriptor;
import java.util.*;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HoughResult extends AbstractResult<ParameterizationFunction> {
  /**
   * Marker for a file name of a cluster.
   */
  public static final String CLUSTER_MARKER = "cluster";

  /**
   * Marker for a file name of noise.
   */
  public static final String NOISE_MARKER = "noise";

  /**
   * Extension for txt-files.
   */
  public static final String FILE_EXTENSION = ".txt";

  private Map<Integer, Set<Integer>> clusters;

  private int dimensionality;

  /**
   * todo
   *
   * @param db
   */
  public HoughResult(Database<ParameterizationFunction> db,
                     Map<Integer, Set<Integer>> clusters,
                     int dimensionality) {
    super(db);
    this.clusters = clusters;
    this.dimensionality = dimensionality;
  }

  /**
   * todo
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
  public void output(PrintStream outStream, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    writeHeader(outStream, settings, null);

    for (Integer d : clusters.keySet()) {
      String marker = d == dimensionality ?
                      CLUSTER_MARKER + Format.format(d, dimensionality - 1) + FILE_EXTENSION :
                      NOISE_MARKER + FILE_EXTENSION;

      PrintStream markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
      markedOut.println(marker + ":");
      try {
        write(d, markedOut, normalization, settings);
      }
      catch (NonNumericFeaturesException e) {
        throw new UnableToComplyException(e);
      }
      markedOut.flush();
    }
  }

  /**
   * Writes a cluster denoted by its cluster number to the designated print
   * stream.
   *
   * @param clusterDimensionality  the dimensionality of the cluster to be written
   * @param out           the print stream where to write
   * @param normalization a Normalization to restore original values for output - may
   *                      remain null
   * @param settings      the settings to be written into the header
   * @throws NonNumericFeaturesException if feature vector is not compatible with values initialized
   *                                     during normalization
   */
  private void write(int clusterDimensionality, PrintStream out, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException {
    List<String> header = new ArrayList<String>();
    Set<Integer> clusterIDs = clusters.get(clusterDimensionality);

    if (clusterDimensionality < dimensionality)
      header.add("cluster size = " + clusterIDs.size());
    else
      header.add("noise size = " + clusterIDs.size());
    writeHeader(out, settings, header);

    for (Integer id: clusterIDs) {
      ParameterizationFunction f = db.get(id);
      if (normalization != null) {
        f = normalization.restore(f);
      }
      out.print(f.toString());
      Map<AssociationID, Object> associations = db.getAssociations(id);
      List<AssociationID> keys = new ArrayList<AssociationID>(associations.keySet());
      Collections.sort(keys);
      for (AssociationID associationID : keys) {
        if (associationID == AssociationID.CLASS || associationID == AssociationID.LABEL || associationID == AssociationID.LOCAL_DIMENSIONALITY) {
          out.print(SEPARATOR);
          out.print(associationID.getName());
          out.print("=");
          out.print(associations.get(associationID));
        }
      }
      out.println();
    }
  }
}
