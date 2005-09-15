package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.linearalgebra.Matrix;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;

/**
 * Provides a result of a clustering-algorithm that computes several clusters
 * and remaining noise and a correlation analysis for each cluster.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ClustersPlusNoisePlusCorrelationAnalysis<T extends MetricalObject> extends ClustersPlusNoise<T> {
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
   * @param epsilon                      the epsilon parameter of DBSCAN
   * @param minPts                       the minPts parameter of DBSCAN
   * @param correlationAnalysisSolutions an array of correlation analysis solutions for each cluster
   * @param nf                           number format for output accuracy
   */
  public ClustersPlusNoisePlusCorrelationAnalysis(Integer[][] clustersAndNoise,
                                                  Database<T> db,
                                                  Distance epsilon,
                                                  int minPts,
                                                  CorrelationAnalysisSolution[] correlationAnalysisSolutions,
                                                  NumberFormat nf) {
    super(clustersAndNoise, db, epsilon, minPts);

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
   * @param epsilon                      the epsilon parameter of DBSCAN
   * @param minPts                       the minPts parameter of DBSCAN
   * @param correlationAnalysisSolutions an array of correlation analysis solutions for each cluster
   */
  public ClustersPlusNoisePlusCorrelationAnalysis(Integer[][] clustersAndNoise, Database<T> db,
                                                  Distance epsilon, int minPts,
                                                  CorrelationAnalysisSolution[] correlationAnalysisSolutions) {
    this(clustersAndNoise, db, epsilon, minPts, correlationAnalysisSolutions, null);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.result.Result#output(java.io.File, de.lmu.ifi.dbs.normalization.Normalization)
   */
  public void output(File out, Normalization<T> normalization) throws UnableToComplyException {
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
        write(c, markedOut, normalization);
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
   * @throws de.lmu.ifi.dbs.normalization.NonNumericFeaturesException
   *          if feature vector is not compatible with values initialized
   *          during normalization
   */
  private void write(int clusterIndex, PrintStream out, Normalization<T> normalization) throws NonNumericFeaturesException, UnableToComplyException {
    if (clusterIndex != clustersAndNoise.length - 1) {
      Matrix printSolution;
      if (normalization != null) {
        try {
          printSolution = normalization.transform(
          correlationAnalysisSolutions[clusterIndex].getSolutionMatrix()).gaussJordanElimination();
        }
        catch (NonNumericFeaturesException e) {
          throw new UnableToComplyException(e);
        }
      }
      else {
        printSolution = correlationAnalysisSolutions[clusterIndex].getSolutionMatrix().copy();
      }

      out.println("######################################################################################");
      writeHeader(out, normalization);
      if (this.nf == null) {
        out.println(printSolution.toString("###  "));
        out.println("lower deviations: " + Util.format(correlationAnalysisSolutions[clusterIndex].getLowerDeviations()));
        out.println("upper deviations: " + Util.format(correlationAnalysisSolutions[clusterIndex].getUpperDeviations()));
        out.println(printSolution.toString("###  "));
      }
      else {
        out.println(printSolution.toString(nf, "###  "));
        out.println("lower deviations: " + Util.format(correlationAnalysisSolutions[clusterIndex].getLowerDeviations(), nf));
        out.println("upper deviations: " + Util.format(correlationAnalysisSolutions[clusterIndex].getUpperDeviations(), nf));
        out.println(printSolution.toString(nf, "###  "));
      }
      out.println("######################################################################################");
    }

    for (int i = 0; i < clustersAndNoise[clusterIndex].length; i++) {
      T mo = db.get(clustersAndNoise[clusterIndex][i]);
      if (normalization != null) {
        mo = normalization.restore(mo);
      }
      out.println(mo.toString() + SEPARATOR + db.getAssociation(Database.ASSOCIATION_ID_LABEL, clustersAndNoise[clusterIndex][i]));
    }
  }

  /**
   * Returns the database to which this clustering result belongs to.
   *
   * @return the database to which this clustering result belongs to
   */
  public Database<T> getDatabase() {
    return db;
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
