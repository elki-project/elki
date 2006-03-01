package de.lmu.ifi.dbs.algorithm.result.clustering;

import de.lmu.ifi.dbs.algorithm.result.CorrelationAnalysisSolution;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.List;

/**
 * Provides a result of a clustering-algorithm that computes several clusters
 * and remaining noise and a correlation analysis for each cluster.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ClustersPlusNoisePlusCorrelationAnalysis extends ClustersPlusNoise<DoubleVector> {
  /**
   * An array of correlation analysis solutions for each cluster.
   */
  private CorrelationAnalysisSolution[] correlationAnalysisSolutions;

  /**
   * Number format for output accuracy.
   */
  private NumberFormat nf;

  /**
   * Provides a result of a clustering-algorithm that computes several clusters
   * and remaining noise and a correlation analysis for each cluster
   *
   * @param clustersAndNoise             an array of clusters and noise, respectively, where each array
   *                                     provides the object ids of its members
   * @param db                           the database containing the objects of clusters
   * @param correlationAnalysisSolutions an array of correlation analysis solutions for each cluster
   * @param nf                           number format for output accuracy
   */
  public ClustersPlusNoisePlusCorrelationAnalysis(Integer[][] clustersAndNoise,
                                                  Database<DoubleVector> db,
                                                  CorrelationAnalysisSolution[] correlationAnalysisSolutions,
                                                  NumberFormat nf) {
    super(clustersAndNoise, db);

    if (clustersAndNoise.length == 0 && correlationAnalysisSolutions.length != 0)
      throw new IllegalArgumentException("correlationAnalysisSolutions.length must be 0!");

    if (clustersAndNoise.length != correlationAnalysisSolutions.length + 1)
      throw new IllegalArgumentException("clustersAndNoise.length != correlationAnalysisSolutions.length + 1!");

    this.correlationAnalysisSolutions = correlationAnalysisSolutions;
    this.nf = nf;
  }

  /**
   * Provides a result of a clustering-algorithm that computes several clusters
   * and remaining noise and a correlation analysis for each cluster
   *
   * @param clustersAndNoise             an array of clusters and noise, respectively, where each array
   *                                     provides the object ids of its members
   * @param db                           the database containing the objects of clusters
   * @param correlationAnalysisSolutions an array of correlation analysis solutions for each cluster
   */
  public ClustersPlusNoisePlusCorrelationAnalysis(Integer[][] clustersAndNoise,
                                                  Database<DoubleVector> db,
                                                  CorrelationAnalysisSolution[] correlationAnalysisSolutions
  ) {
    this(clustersAndNoise, db, correlationAnalysisSolutions, null);
  }

  /**
   * @see Result#output(File, Normalization, List)
   */
  public void output(File out, Normalization<DoubleVector> normalization, List<AttributeSettings> settings) throws UnableToComplyException {
    for (int c = 0; c < this.clustersAndNoise.length; c++) {
      String marker;
      if (c < clustersAndNoise.length - 1) {
        marker = CLUSTER_MARKER + format(c + 1, clustersAndNoise.length - 1);
      }
      else {
        marker = NOISE_MARKER;
      }
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
        write(c, markedOut, normalization, settings);
      }
      catch (NonNumericFeaturesException e) {
        throw new UnableToComplyException(e);
      }
      markedOut.flush();
    }

  }

  /**
   * Returns an integer-string for the given input, that has as many leading
   * zeros as to match the length of the specified maximum.
   *
   * @param input   an integer to be formatted
   * @param maximum the maximum to adapt the format to
   * @return an integer-string for the given input, that has as many leading
   *         zeros as to match the length of the specified maximum
   */
  protected String format(int input, int maximum) {
    NumberFormat formatter = NumberFormat.getIntegerInstance();
    formatter.setMinimumIntegerDigits(Integer.toString(maximum).length());
    return formatter.format(input);
  }

  /**
   * Writes a cluster denoted by its cluster number to the designated print
   * stream.
   *
   * @param clusterIndex  the number of the cluster to be written
   * @param out           the print stream where to write
   * @param normalization a Normalization to restore original values for output - may
   *                      remain null
   * @param settings the settings to be written into the header
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vector is not compatible with values initialized
   *          during normalization
   */
  private void write(int clusterIndex,
                     PrintStream out,
                     Normalization<DoubleVector> normalization,
                     List<AttributeSettings> settings) throws NonNumericFeaturesException {

    if (clusterIndex != clustersAndNoise.length - 1) {
      CorrelationAnalysisSolution correlationAnalysisSolution = correlationAnalysisSolutions[clusterIndex];
      Matrix printSolution = correlationAnalysisSolution.getPrintSolutionMatrix(normalization);
      writeHeader(out, settings);
      out.println("### " + correlationAnalysisSolution.getClass().getSimpleName() + ":");
      out.println(printSolution.toString("###  ", nf));
      out.println("################################################################################");
    }

    for (int i = 0; i < clustersAndNoise[clusterIndex].length; i++) {
      DoubleVector v = db.get(clustersAndNoise[clusterIndex][i]);
      if (normalization != null) {
        v = normalization.restore(v);
      }
      out.println(v.toString() + SEPARATOR + db.getAssociation(AssociationID.LABEL, clustersAndNoise[clusterIndex][i]));
    }
  }

  /**
   * Returns the clusters of this result. Each array provides the object ids of one cluster.
   *
   * @return the clusters of this result
   */
  public Integer[][] getClusterAndNoiseArray() {
    if (clustersAndNoise.length <= 1) return new Integer[0][];

    Integer[][] clusters = new Integer[clustersAndNoise.length - 1][];
    System.arraycopy(clustersAndNoise, 0, clusters, 0, clustersAndNoise.length - 1);
    return clusters;
  }

}
