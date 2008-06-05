package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.AbstractResult;
import de.lmu.ifi.dbs.data.ParameterizationFunction;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Associations;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.output.Format;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * TODO: comment
 *
 * @author Elke Achtert
 */
public class CASHResult extends AbstractResult<ParameterizationFunction> {
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

  /**
   * The mapping between subspace dimensions and clusters.
   */
  private SubspaceClusterMap clusterMap;

  /**
   * The dimensionality of the feature space.
   */
  private int dimensionality;

  /**
   * todo
   *
   * @param db
   */
  public CASHResult(Database<ParameterizationFunction> db,
                     SubspaceClusterMap clusterMap,
                     int dimensionality) {
    super(db);
    this.clusterMap = clusterMap;
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
    for (Integer d : clusterMap.subspaceDimensionalities()) {
      List<Set<Integer>> ids_d = clusterMap.getCluster(d);
      List<LinearEquationSystem> dependencies_d = clusterMap.getDependencies(d);
      for (int i = 0; i < ids_d.size(); i++) {
        Set<Integer> ids = ids_d.get(i);
        LinearEquationSystem dependencies = dependencies_d != null ? dependencies_d.get(i) : null;
        String marker = d == dimensionality ?
                        CLUSTER_MARKER + Format.format(d, dimensionality - 1) + "_" + i + FILE_EXTENSION :
                        NOISE_MARKER + FILE_EXTENSION;

        PrintStream markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
        markedOut.println(marker + ":");
        try {
          write(d, ids, dependencies, markedOut, normalization, settings);
        }
        catch (NonNumericFeaturesException e) {
          throw new UnableToComplyException(e);
        }
        markedOut.flush();
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization, java.util.List)
   */
  public void output(File out, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    for (Integer d : clusterMap.subspaceDimensionalities()) {
      List<Set<Integer>> ids_d = clusterMap.getCluster(d);
      List<LinearEquationSystem> dependencies_d = clusterMap.getDependencies(d);
      for (int i = 0; i < ids_d.size(); i++) {
        Set<Integer> ids = ids_d.get(i);
        LinearEquationSystem dependencies = dependencies_d != null ? dependencies_d.get(i) : null;
        String marker = d != dimensionality ?
                        CLUSTER_MARKER + Format.format(d, dimensionality - 1) + "_" + i + FILE_EXTENSION :
                        NOISE_MARKER + FILE_EXTENSION;

        PrintStream markedOut;
        try {
          File markedFile = new File(out.getAbsolutePath() + File.separator + marker);
          markedFile.getParentFile().mkdirs();
          markedOut = new PrintStream(new FileOutputStream(markedFile));
        }
        catch (Exception e) {
          markedOut = new PrintStream(new FileOutputStream(FileDescriptor.out));
          markedOut.println(marker + ":");
        }
        try {
          write(d, ids, dependencies, markedOut, normalization, settings);
        }
        catch (NonNumericFeaturesException e) {
          throw new UnableToComplyException(e);
        }
        markedOut.flush();
      }
    }

  }

  /**
   * Writes a cluster to the designated print stream.
   *
   * @param clusterDimensionality the dimensionality of the cluster to be written
   * @param clusterIDs            the ids belonging to the cluster to be written
   * @param clusterDependency     the dependencies of the cluster
   * @param out                   the print stream where to write
   * @param normalization         a Normalization to restore original values for output - may
   *                              remain null
   * @param settings              the settings to be written into the header
   * @throws NonNumericFeaturesException if feature vector is not compatible with values initialized
   *                                     during normalization
   */
  private void write(int clusterDimensionality, Set<Integer> clusterIDs, LinearEquationSystem clusterDependency,
                     PrintStream out, Normalization<ParameterizationFunction> normalization, List<AttributeSettings> settings) throws NonNumericFeaturesException {
    List<String> header = new ArrayList<String>();

    if (clusterDimensionality < dimensionality) {
      header.add("cluster size = " + clusterIDs.size());
      header.add("cluster dimensionality = " + clusterDimensionality);
    }
    else {
      header.add("noise size = " + clusterIDs.size());
      header.add("noise dimensionality = " + clusterDimensionality);
    }
    if (clusterDependency != null) {
      header.add("basis vectors " + clusterDependency.equationsToString("### ", 6));
    }
    writeHeader(out, settings, header);

    for (Integer id : clusterIDs) {
      ParameterizationFunction f = db.get(id);
      if (normalization != null) {
        f = normalization.restore(f);
      }
      out.print(Format.format(f.getRowVector().getRowPackedCopy(), SEPARATOR));
      Associations associations = db.getAssociations(id);
      List<AssociationID> keys = new ArrayList<AssociationID>(associations.keySet());
      Collections.sort(keys);
      for (AssociationID<?> associationID : keys) {
        if (associationID == AssociationID.CLASS || associationID == AssociationID.LABEL || associationID == AssociationID.LOCAL_DIMENSIONALITY)
        {
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
