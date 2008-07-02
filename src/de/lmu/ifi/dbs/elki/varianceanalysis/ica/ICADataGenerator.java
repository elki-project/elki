package de.lmu.ifi.dbs.elki.varianceanalysis.ica;

import de.lmu.ifi.dbs.elki.data.synthetic.ArbitraryCorrelationGenerator;
import de.lmu.ifi.dbs.elki.data.synthetic.AxesParallelCorrelationGenerator;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;

import java.io.File;
import java.util.ArrayList;

/**
 * Only for debugging!!!
 * todo: delete in ICA-classes, after debugging was successful
 *
 * @author Elke Achtert 
 */
public class ICADataGenerator {
  private static String prefix;

  static {
    if (new File("tmp").getAbsolutePath().startsWith("H:")) {
      ICADataGenerator.prefix = "P:";
    }
    else {
      ICADataGenerator.prefix = "";
    }
  }

//  public final static String DIRECTORY = ICADataGenerator.prefix + "/nfs/infdbs/Publication/ICDM06-HiSC/experiments/synthetic/";
  public final static String DIRECTORY = "";

  private static double JITTER = 0.005;

  public static void main(String[] args) {
    ICADataGenerator generator = new ICADataGenerator();

    try {
      generator.synthetic1();
    }
    catch (Exception e) {
        // todo exception handling
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void synthetic1() {
    String output = ICADataGenerator.DIRECTORY + "ica_1.txt";
    File file = new File(output);
    if (file.exists()) {
      file.delete();
    }


    {
      double[][] b = new double[][]{{1,1}};
      double[] min = new double[]{-10, -10};
      double[] max = new double[]{10, 10};
      double[] point = new double[]{0,0};
      runGenerator(500, point, b, "g1", min, max, 0, output);
    }
    {
      double[][] b = new double[][]{{-0.5, 0.25}};
      double[] min = new double[]{-10, -10};
      double[] max = new double[]{10, 10};
      double[] point = new double[]{0,0};
      runGenerator(500, point, b, "g2", min, max, 0, output);
    }
//    {
//      double[][] b = new double[][]{{1, 0}, {0, 1}};
//      double[] min = new double[]{0, 0};
//      double[] max = new double[]{10, 10};
//      double[] point = new double[]{5, 5};
//      runGenerator(50, point, b, "_00", min, max, ICADataGenerator.JITTER, output);
//    }

  }

  private void synthetic2() {
    String output = ICADataGenerator.DIRECTORY + "ica_2.txt";
    File file = new File(output);
    if (file.exists()) {
      file.delete();
    }

    {
      double[][] b = new double[][]{{1,1.1}};
      double[] min = new double[]{0, 0};
      double[] max = new double[]{1, 1};
      double[] point = new double[]{0.5,0.5};
      runGenerator(500, point, b, "g1", min, max, 0, output);
    }
    {
//      double[][] b = new double[][]{{-0.5, 0.25}};
//      double[] min = new double[]{-10, -10};
//      double[] max = new double[]{10, 10};
//      double[] point = new double[]{0,-5};
//      runGenerator(500, point, b, "g2", min, max, 0, output);
    }

  }

  public static void runGenerator(int numberOfPoints,
                                  double[] point,
                                  double[][] basis,
                                  String label, double[] min, double[] max,
                                  double jitter, String output) {
    ArrayList<String> parameters = new ArrayList<String>();

    // numberOfPoints
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.NUMBER_P);
    parameters.add(Integer.toString(numberOfPoints));

    // corrDim
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.CORRDIM_P);
    parameters.add(Integer.toString(basis.length));

    // dataDim
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.DIM_P);
    parameters.add(Integer.toString(basis[0].length));

    // model point
    parameters.add(OptionHandler.OPTION_PREFIX + ArbitraryCorrelationGenerator.POINT_P);
    parameters.add(Util.format(point, ",", 8));

    // basis
    parameters.add(OptionHandler.OPTION_PREFIX + ArbitraryCorrelationGenerator.BASIS_P);
    parameters.add(Util.format(basis, ":", ",", 8));

    // label
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.LABEL_P);
    parameters.add(label);

    // minima
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.MIN_P);
    String minParameter = "";
    for (int i = 0; i < min.length; i++) {
      if (i > 0) minParameter += ",";
      minParameter += Double.toString(min[i]);
    }
    parameters.add(minParameter);

    // maxima
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.MAX_P);
    String maxParameter = "";
    for (int i = 0; i < max.length; i++) {
      if (i > 0) maxParameter += ",";
      maxParameter += Double.toString(max[i]);
    }
    parameters.add(maxParameter);

    // jitter
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.JITTER_P);
    parameters.add(Double.toString(jitter));

    // output
    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.OUTPUT_P);
    parameters.add(output);

    // verbose
//    parameters.add(OptionHandler.OPTION_PREFIX + AxesParallelCorrelationGenerator.VERBOSE_F);

    ArbitraryCorrelationGenerator.main(parameters.toArray(new String[parameters.size()]));
  }

}
