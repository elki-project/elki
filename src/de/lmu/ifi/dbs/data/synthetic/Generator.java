package de.lmu.ifi.dbs.data.synthetic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Provides methods for generating synthetic data.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class Generator {
//   private static Random RANDOM = new Random(231265);
  private static Random RANDOM = new Random();

  private static double MAX = 100;

  private static double MAX_JITTER_PCT = 0.5;

  private static String FILE_NAME = "3D_1Ebene_4Geraden_Noise.txt";

  // private static String FILE_NAME = "gerade1.txt";
  // private static String FILE_NAME = "gerade1.txt";

  private static String DIRECTORY;

  static {
    String prefix = "";
//    String directory = "/nfs/infdbs/Publication/RECOMB06-ACEP/experiments/data/synthetic/runtime/";
//    String directory = "/nfs/infdbs/Publication/PAKDD06-DeliClu/experiments/data/synthetic/runtime/";
    String directory = "/data/synthetic/correlation/";
    String user = System.getProperty("user.name");
    // String os = System.getProperty("os.name");
    if ((user.equals("achtert") || user.equals("schumm"))) {
//      prefix = "P:";
      prefix = "H:";
    }
    DIRECTORY = prefix + directory;
  }

  public static List<Double[]> randomGauss(int corrDim, int dataDim, Random random) {
    if (corrDim > dataDim || corrDim < 1 || dataDim < 1) {
      throw new IllegalArgumentException("Illegal arguments: corrDim=" + corrDim + " - dataDim=" + dataDim + ".");
    }

    double multiplier = 1000;
    List<Double[]> gauss = new ArrayList<Double[]>(corrDim);
    for (int i = 0; i < corrDim; i++) {
      Double[] coefficients = new Double[dataDim + 1];
      for (int d = 0; d < coefficients.length; d++) {
        coefficients[d] = random.nextDouble();
        if (d == dataDim) {
          coefficients[d] *= corrDim * multiplier;
        }
      }
      gauss.add(coefficients);
    }
    return gauss;
  }

  public static void correlationClusterSize(int dataDim, int minSize, int increment, int steps) {
    try {
      for (int i = 0; i < steps; i++) {

        int size = minSize + i * increment;
        File output = new File(DIRECTORY + "size/size" + size);
        output.getParentFile().mkdirs();
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(output));

        int sizePerPartition = size / (dataDim - 1);

        int x0 = 2;
        for (int d = 1; d < dataDim; d++) {
          if (d == dataDim - 1) {
            sizePerPartition = sizePerPartition + size % (dataDim - 1);
          }

          int sizePerPartitionCluster = sizePerPartition / (i + 1);
          for (int c = 0; c <= i; c++) {
            if (c == i) {
              sizePerPartitionCluster = sizePerPartitionCluster + sizePerPartition % (i + 1);
            }
            generateAxesParallelDependency(sizePerPartitionCluster, d, dataDim, "corrdim" + d + "_" + c,
                                           x0, out);
            x0 += 2;
          }
        }
        out.flush();
        out.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void correlationClusterDim(int size, int minDim, int increment, int steps) {
    try {
      for (int i = 0; i < steps; i++) {
        int dataDim = minDim + i * increment;

        File output = new File(DIRECTORY + "dimensionality/dim" + dataDim);
        output.getParentFile().mkdirs();
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(output));

        int sizePerPartition = size / (dataDim - 1);

        for (int d = 1; d < dataDim; d++) {
          if (d == dataDim - 1) {
            sizePerPartition = sizePerPartition + size % (dataDim - 1);
          }
          generateAxesParallelDependency(sizePerPartition, d, dataDim, "corrdim" + d, d * 2, out);
        }

        out.flush();
        out.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void clusterSize(int dataDim, int minSize, int increment, int steps) {
    try {
      for (int i = 0; i < steps; i++) {
        int size = minSize + i * increment;
        File output = new File(DIRECTORY + (size / 1000) + "_T_" + dataDim + ".txt");
        output.getParentFile().mkdirs();
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(output));

//        final double[] radii = new double[]{2.0, 5.0, 10.0, 15.0, 20.0};
//        final double[] radii = new double[]{5.0, 5.0, 5.0, 5.0, 5.0};
//        final double[] radii = new double[]{10};

//        generateClusters(size, dataDim, radii, 0.0, false, 0, 100, out);
//        generateElkiClusters(size, dataDim, out);
        generateRandom(size, dataDim, 0.0, 100.0, out);
        out.flush();
        out.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void combined() {
    try {
      List<Double[]> gauss = new ArrayList<Double[]>();
      gauss.add(new Double[]{1.0, 10.0, -30.0, 100.0});

      int dim = gauss.get(0).length - 1;
      double[] minima = new double[dim];
      double[] maxima = new double[dim];
      for (int i = 0; i < dim; i++) {
        minima[i] = Double.MAX_VALUE;
        maxima[i] = -Double.MAX_VALUE;
      }

      OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(DIRECTORY + FILE_NAME));
      generateDependency(2000, gauss, "e1", true, 400, 0,  minima, maxima, out);

//      gauss = new ArrayList<Double[]>();
//      gauss.add(new Double[]{1.0, -30.0, -5.0, 100.0});
//      generateDependency(2000, gauss, "e2", true, -100, 50, minima, maxima, out);

//      gauss = new ArrayList<Double[]>();
//      gauss.add(new Double[]{10.0, 10.0, 30.0, 300.0});
//      generateDependency(2000, gauss, "e3", true, 100, 150, minima, maxima, out);

      gauss = new ArrayList<Double[]>();
      gauss.add(new Double[]{1.0, 0.0, -2.0, -100.0});
      gauss.add(new Double[]{0.0, 1.0, 2.0, -200.0});
      generateDependency(2000, gauss, "g1", true, 50, 50,  minima, maxima, out);

      gauss = new ArrayList<Double[]>();
      gauss.add(new Double[]{1.0, 0.0, -3.0, 300.0});
      gauss.add(new Double[]{0.0, 1.0, 15.0, -400.0});
      generateDependency(1000, gauss, "g2", true, 50, 50 ,minima, maxima, out);

      gauss = new ArrayList<Double[]>();
      gauss.add(new Double[]{1.0, 0.0, 20.0, -400.0});
      gauss.add(new Double[]{0.0, 1.0, -6.0, 500.0});
      generateDependency(1000, gauss, "g3", true, 100, 0, minima, maxima, out);

      gauss = new ArrayList<Double[]>();
      gauss.add(new Double[]{1.0, 0.0, -5.0, -200.0});
      gauss.add(new Double[]{0.0, 1.0, 5.0, 200.0});
      generateDependency(1000, gauss, "g4", true, 75, -50, minima, maxima, out);

      generateNoise(500, minima, maxima, "noise", out);

      out.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void main(String[] args) {
    combined();
//    correlationClusterSize(20, 10000, 10000, 10);
//    dim(10000, 5, 5, 10);
//    clusterSize(2, 1000, 1, 5);

//    int dim = 10;
//    double[] minima = new double[dim];
//    double[] maxima = new double[dim];
//    Arrays.fill(maxima, 1);
//    File output = new File("1_T_"+dim);
//    try {
//      generateNoise(1000, minima, maxima, "noise", new OutputStreamWriter(new FileOutputStream(output)));
//    }
//    catch (IOException e) {
//      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//    }
  }

  /**
   * Generates data that follows the specified dependencies.
   *
   * @param noPoints the number of points to be generated
   * @param gauss    the matrix holding the dependencies, has to be in
   *                 'zeilenstufenform'
   * @param label    the label for the generated data
   * @param jitter   if true, the generated data has jitter
   * @param out      the outputstream to write to
   * @throws IOException
   */
  private static void generateDependency(int noPoints, List<Double[]> gauss,
                                         String label, boolean jitter,
                                         double max, double x_0,
                                         double[] minima, double[] maxima,
                                         OutputStreamWriter out) throws IOException {
    if (gauss.size() == 0)
      throw new IllegalArgumentException("gauss.size == 0");

    int dim = gauss.get(0).length - 1;

    // get the independent variables
    int independentCount = 0;
    List<Integer> independentIndices = new ArrayList<Integer>();
    for (int i = 0; i < gauss.size(); i++) {
      Double[] dependency = gauss.get(i);
      if (dependency.length != dim + 1)
        throw new IllegalArgumentException("dependency.length != dim + 1");

      if (dependency[i + independentCount] == 0) {
        independentIndices.add(i + independentCount);
        independentCount++;
      }
    }
    while (gauss.size() + independentCount != dim) {
      independentCount++;
      independentIndices.add(gauss.size() + independentCount - 1);
    }

    out.write("########################################################\n");
    if (jitter) {
      out.write("### Jitter max. +/- " + MAX_JITTER_PCT + " %\n");
      out.write("###\n");
    }
    for (Double[] g : gauss) {
      out.write("### " + Arrays.asList(g) + "\n");
    }
    out.write("########################################################\n");

    double[][] featureVectors = new double[noPoints][dim];
    // generate the feature vectors
    for (int n = 0; n < noPoints; n++) {
      // randomize the independent values
      for (int i = 0; i < independentCount; i++) {
        int index = independentIndices.get(i);
        Double[] values = new Double[dim + 1];
        for (int d = 0; d < dim + 1; d++) {
          if (d == index)
            values[d] = 1.0;
          else if (d == dim) {
            values[d] = (-1) * RANDOM.nextDouble() * max + x_0;
          }
          else
            values[d] = 0.0;
        }
        if (n != 0)
          gauss.remove(index);
        gauss.add(index, values);
      }

//      for (int i = 0; i < gauss.size(); i++) {
//        Double[] dependency = gauss.get(i);
//        System.out.println(Arrays.asList(dependency));
//      }
//      System.out.println(gauss);

      for (int i = dim - 1; i >= 0; i--) {
        Double[] dependency = gauss.get(i);
        for (int d = dim - 1; d >= i; d--) {
          if (d == dim - 1) {
            featureVectors[n][i] += dependency[d + 1];
          }
          else {
            featureVectors[n][i] += (-1) * dependency[d + 1] * featureVectors[n][d + 1];
          }

          if (minima[i] > featureVectors[n][i])
            minima[i] = featureVectors[n][i];
          if (maxima[i] < featureVectors[n][i])
            maxima[i] = featureVectors[n][i];
        }
      }
    }

    if (jitter) {
      for (int n = 0; n < noPoints; n++) {
        for (int i = dim - 1; i >= 0; i--) {
          double j = (RANDOM.nextDouble() * 2 - 1) * (MAX_JITTER_PCT * (maxima[i] - minima[i]) / 100.0);
          featureVectors[n][i] = featureVectors[n][i] + j;
        }
      }
    }

    for (int n = 0; n < noPoints; n++) {
      for (int i = 0; i < dim; i++) {
        out.write(featureVectors[n][i] + " ");
      }
      out.write(label + "\n");
    }
  }

  private static void generateNoise(int noPoints, double[] minima, double[] maxima, String label, OutputStreamWriter out) throws IOException {

    if (minima.length != maxima.length) {
      throw new IllegalArgumentException("minima.length != maxima.length");
    }

    int dim = minima.length;
    for (int i = 0; i < noPoints; i++) {
      double[] values = new double[dim];
      for (int d = 0; d < dim; d++) {
        values[d] = RANDOM.nextDouble() * (maxima[d] - minima[d]) + minima[d];
        out.write(values[d] + " ");
      }
      out.write(label + "\n");
    }
  }

  private static void generateAxesParallelDependency(int noPoints, int corrDim,
                                                     int dataDim, String label,
                                                     double min,
                                                     OutputStreamWriter out) throws IOException {
    if (corrDim >= dataDim)
      throw new IllegalArgumentException("corrDim >= dataDim");

    out.write("########################################################\n");
    out.write("### corrDim " + corrDim + "\n");
    out.write("########################################################\n");

    // randomize the dependent values
    Double[] dependentValues = new Double[dataDim - corrDim];
    for (int d = 0; d < dataDim - corrDim; d++) {
      dependentValues[d] = RANDOM.nextDouble();
    }

    // generate the feature vectors
    double[][] featureVectors = new double[noPoints][dataDim];
    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
        if (d < dataDim - corrDim)
          featureVectors[n][d] = dependentValues[d];
        else
          featureVectors[n][d] = RANDOM.nextDouble();
      }
    }

    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
        featureVectors[n][d] = featureVectors[n][d] + min;
      }
    }

    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
        out.write(featureVectors[n][d] + " ");
      }
      out.write(label + "\n");
    }
  }

  private static void generateClusters(int noPoints, int dim, double[] radii,
                                       double noisePct, boolean overlap,
                                       double min, double max, OutputStreamWriter out) throws IOException {

    Double[][] featureVectors = new Double[noPoints][dim];
    String[] labels = new String[noPoints];

    // number of clusters
    int noCluster = radii.length;
    System.out.println("noCluster " + noCluster);

    // noNoise of points in each cluster
    int pointsPerCluster = (int) ((1.0 - noisePct) * noPoints / noCluster);
    System.out.println("pointsPerCluster " + pointsPerCluster);

    // number of noise points
    int noNoise = noPoints - noCluster * pointsPerCluster;
    System.out.println("noNoise " + noNoise);

    // determine centroids of clusters
    List<Double[]> centroids = new ArrayList<Double[]>();
    for (int c = 0; c < noCluster; c++) {
      Double[] centroid = new Double[dim];
      for (int d = 0; d < dim; d++) {
        centroid[d] = radii[c] + ((max - min) - 2.0 * radii[c]) * RANDOM.nextDouble();
      }

      boolean ok = true;
      if (!overlap) {
        for (int j = 0; j < c; j++) {
          Double[] otherCenter = centroids.get(j);
          double l = 0;
          for (int a = 0; a < dim; a++) {
            l += (centroid[a] - otherCenter[a]) * (centroid[a] - otherCenter[a]);
          }
          l = Math.sqrt(l);
          if (l <= radii[c] + radii[j]) {
            ok = false;
            break;
          }
        }
      }
      if (ok) {
        System.out.println("center " + c + " ok");
        centroids.add(centroid);
      }
      else {
        c--;
      }
    }
    System.out.println("all center ok");

    // create clusters
    int n = 0;
    for (int c = 0; c < noCluster; c++) {
      for (int j = 0; j < pointsPerCluster; j++) {
        Double[] featureVector = new Double[dim];

        double l = 0;
        for (int d = 0; d < dim; d++) {
          Double[] centroid = centroids.get(c);
          double value = centroid[d] +
                         (2.0 * RANDOM.nextDouble() - 1.0) * radii[c];
          featureVector[d] = value;

          l += (centroid[d] - value) * (centroid[d] - value);
        }
        l = Math.sqrt(l);

        if (overlap || l <= radii[c]) {
          featureVectors[n++] = featureVector;
          labels[n - 1] = "cluster_" + c;
        }
        else {
          j--;
        }
      }
      System.out.println("Cluster " + (c + 1) + " ok");
    }

    // create noise
    for (int i = 0; i < noNoise; i++) {
      Double[] featureVector = new Double[dim];

      for (int d = 0; d < dim; d++) {
        featureVector[d] = (max - min) * RANDOM.nextDouble();
      }

      featureVectors[n++] = featureVector;
      labels[n - 1] = "noise";
    }

    /*
    Double[] minVector = new Double[dim];
    for (int d = 0; d < dim; d++) {
      minVector[d] = min;
    }
    featureVectors[n++] = minVector;

    Double[] maxVector = new Double[dim];
    for (int d = 0; d < dim; d++) {
      maxVector[d] = max;
    }
    featureVectors[n++] = maxVector;
    */

    // write to out
    for (n = 0; n < noPoints; n++) {
      for (int d = 0; d < dim; d++) {
        out.write(featureVectors[n][d] + " ");
      }
      out.write(labels[n] + "\n");
    }
  }

  private static void generateRandom(int noPoints, int dim, double min, double max,
                                     OutputStreamWriter out) throws IOException {

    Double[][] featureVectors = new Double[noPoints][dim];

    Double[] minVector = new Double[dim];
    for (int d = 0; d < dim; d++) {
      minVector[d] = min;
    }
    featureVectors[0] = minVector;

    Double[] maxVector = new Double[dim];
    for (int d = 0; d < dim; d++) {
      maxVector[d] = max;
    }
    featureVectors[1] = maxVector;

    for (int n = 2; n < noPoints; n++) {
      Double[] featureVector = new Double[dim];
      for (int d = 0; d < dim; d++) {
        featureVector[d] = RANDOM.nextDouble() * (max - min) + min;
      }

      featureVectors[n] = featureVector;
    }

    // write to out
    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dim; d++) {
        if (d < dim - 1)
          out.write(featureVectors[n][d] + " ");
        else
          out.write(featureVectors[n][d] + "\n");
      }
    }
  }

}
