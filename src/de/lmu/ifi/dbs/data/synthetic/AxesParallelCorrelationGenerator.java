package de.lmu.ifi.dbs.data.synthetic;

import de.lmu.ifi.dbs.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.wrapper.StandAloneWrapper;

import java.io.*;
import java.util.logging.Logger;

/**
 * Provides automatic generation of axes parallel hyperplanes
 * of arbitrary correlation dimensionalities.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

public class AxesParallelCorrelationGenerator extends StandAloneWrapper {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"unused", "UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  static {
    OUTPUT_D = "<filename>the file to write the generated correlation hyperplane in, " +
               "if the file already exists, the generated points will be appended to this file.";
  }

  /**
   * Parameter for dimensionality.
   */
  public static final String DIM_P = "dim";

  /**
   * Description for parameter dim.
   */
  public static final String DIM_D = "<int>the dimensionality of the feature space.";

  /**
   * Parameter for correlation dimensionality.
   */
  public static final String CORRDIM_P = "corrdim";

  /**
   * Description for parameter corrdim.
   */
  public static final String CORRDIM_D = "<int>the correlation dimensionality of the correlation hyperplane.";

  /**
   * Parameter for number of points.
   */
  public static final String NUMBER_P = "number";

  /**
   * Description for parameter number.
   */
  public static final String NUMBER_D = "<int>the (positive) number of points in the correlation hyperplane.";

  /**
   * Parameter for label.
   */
  public static final String LABEL_P = "label";

  /**
   * Description for parameter label.
   */
  public static final String LABEL_D = "<string>a label specifiying the correlation hyperplane, " +
                                       "default is no label.";


  /**
   * Generates an axes parallel dependency. The first dataDim - corrDim variables
   * are the dependent variables, the last corrDim variables are the independent variables.
   * The generated data points are in each dimension in the range of [start, start+1].
   *
   * @param noPoints  number of points to be generated
   * @param corrDim   the dimensionality of the correlation to be generated
   * @param dataDim   the dimensionality of the data
   * @param label     the label of the correlation
   * @param start     the start value in each dimension
   * @param outStream the output stream to write to
   * @throws java.io.IOException
   */
  private static void generateAxesParallelDependency(int noPoints, int corrDim,
                                                     int dataDim, String label,
                                                     double start,
                                                     PrintStream outStream) throws IOException {
    /*
    if (corrDim >= dataDim)
      throw new IllegalArgumentException("corrDim >= dataDim");

    outStream.println("########################################################");
    outStream.println("### corrDim " + corrDim);
    outStream.println("########################################################");

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
        featureVectors[n][d] = featureVectors[n][d] + start;
      }
    }

    for (int n = 0; n < noPoints; n++) {
      for (int d = 0; d < dataDim; d++) {
        outStream.print(featureVectors[n][d] + " ");
      }
      outStream.println(label);
    }
  }

  private static void correlationClusterDim(int size, int minDim, int increment, int steps) {
    try {
      for (int i = 0; i < steps; i++) {
        int dataDim = minDim + i * increment;
        System.out.println("dim " + dataDim);

        File output = new File(DIRECTORY + "/dimensionality/data_neu/dim" + dataDim);
        System.out.println(output);
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

  private static void clusterSize(int dataDim, int minSize, int increment, int steps) {
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
        generateUniformDistribution(size, dataDim, 0.0, 100.0, out);
        out.flush();
        out.close();
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
        */
}
}
