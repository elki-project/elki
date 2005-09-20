package de.lmu.ifi.dbs.algorithm.result;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;

/**
 * Provides a result of a clustering-algorithm that computes several clusters
 * and remaining noise.
 *
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ClustersPlusNoise<T extends MetricalObject> implements Result<T> {
  /**
   * Marker for a file name of a cluster.
   */
  public static final String CLUSTER_MARKER = "cluster";

  /**
   * Marker for a file name of noise.
   */
  public static final String NOISE_MARKER = "noise";

  /**
   * An array of clusters and noise, respectively, where each array provides
   * the object ids of its members
   */
  protected Integer[][] clustersAndNoise;

  /**
   * The database containing the objects of clusters.
   */
  protected Database<T> db;

  /**
   * The epsilon parameter.
   */
  Distance epsilon;

  /**
   * The minPts parameter.
   */
  int minPts;


  /**
   * Provides a result of a clustering-algorithm that computes several
   * clusters and remaining noise.
   *
   * @param clustersAndNoise an array of clusters and noise, respectively, where each array
   *                         provides the object ids of its members
   * @param db               the database containing the objects of clusters
   * @param epsilon          the epsilon parameter of DBSCAN
   * @param minPts           the minPts parameter of DBSCAN
   */
  public ClustersPlusNoise(Integer[][] clustersAndNoise, Database<T> db, Distance epsilon, int minPts) {
    this.clustersAndNoise = clustersAndNoise;
    this.db = db;
    this.epsilon = epsilon;
    this.minPts = minPts;
  }

  /**
   * @see Result#output(File, Normalization)
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
   * @throws NonNumericFeaturesException if feature vector is not compatible with values initialized
   *                                     during normalization
   */
  private void write(int clusterIndex, PrintStream out, Normalization<T> normalization) throws NonNumericFeaturesException {
    writeHeader(out, normalization);

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
   * Returns the array of clusters and noise, respectively, where each array provides
   * the object ids of its members.
   *
   * @return the array of clusters and noise
   */
  public Integer[][] getClusterAndNoiseArray() {
    return clustersAndNoise;
  }

  /**
   * Writes a header with the epsilon and minPts parameters and the
   * minima and maxima values of the normalization.
   *
   * @param out           the print stream where to write
   * @param normalization a Normalization to restore original values for output - may
   *                      remain null
   */
  protected void writeHeader(PrintStream out, Normalization<T> normalization) throws NonNumericFeaturesException {
    out.println("###  data dimensionality = " + db.dimensionality());
    out.println("###  epsilon = " + epsilon.toString());
    out.println("###  minPts = " + minPts);

    if (normalization != null) {
      out.println("###");
      out.println(normalization.toString("### "));
      out.println("###");
    }
  }

}
