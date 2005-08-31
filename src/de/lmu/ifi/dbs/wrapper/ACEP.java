package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.*;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

/**
 * TODO: comment
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ACEP extends AbstractWrapper {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>minpts";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected String minpts;

  /**
   * The class of the distance function.
   */
  private Class distanceFunctionClass;

  /**
   * Sets epsilon and minimum points to the optionhandler additionally to the
   * parameters provided by super-classes. Since ACEP is a non-abstract
   * class, finally optionHandler is initialized.
   */
  public ACEP() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());

    distanceFunctionClass = EuklideanDistanceFunction.class;
  }

  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description("ACEP", "", "Wrapper class for derivating dependencies.", "");
  }

  /**
   * Sets the parameters epsilon and minpts additionally to the parameters set
   * by the super-class' method. Both epsilon and minpts are required
   * parameters.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      epsilon = optionHandler.getOptionValue(EPSILON_P);
      minpts = optionHandler.getOptionValue(MINPTS_P);
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException(e);
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return remainingParameters;
  }

  /**
   * Runs the COPAC algorithm.
   */
  public void runCOPAC() {
    if (output == null)
      throw new IllegalArgumentException("Parameter -output is not set!");

    ArrayList<String> params = new ArrayList<String>();

    params.add("-algorithm");
    params.add(COPAC.class.getName());

    params.add("-partAlg");
    params.add(DBSCAN.class.getName());

    params.add("-preprocessor");
    params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

    params.add("-epsilon");
    params.add(epsilon);

    params.add("-minpts");
    params.add(minpts);

    params.add("-distancefunction");
    params.add(distanceFunctionClass.getName());

    params.add("-norm");
    params.add(AttributeWiseDoubleVectorNormalization.class.getName());

    params.add("-in");
    params.add(input);

    params.add("-out");
    params.add(output);

    if (time) {
      params.add("-time");
    }

    if (verbose) {
      params.add("-verbose");
      params.add("-verbose");
    }

    KDDTask copacTask = new KDDTask();
    copacTask.setParameters(params.toArray(new String[params.size()]));
    copacTask.run();
  }

  /**
   * Runs the Dependency Derivating algorithm.
   */
  private void runDependencyDerivator() {
    FileFilter fileFilter = new FileFilter() {
      public boolean accept(File pathname) {
        return (pathname.getName().indexOf("Cluster") != -1);
      }
    };

    File dir = new File(output);
    if (! dir.isDirectory())
      throw new IllegalArgumentException(dir + " is no directory!");

    File[] subDirs = dir.listFiles();
    for (File subDir : subDirs) {
      File[] clusters = subDir.listFiles(fileFilter);

      for (File cluster : clusters) {
        ArrayList<String> params = new ArrayList<String>();

        params.add("-algorithm");
        params.add(DependencyDerivator.class.getName());

//        params.add("-in");
//        params.add(output + "/" + subDir.getName() + "/" + cluster.getName());
//        params.add(cluster.getAbsolutePath());

        System.out.println(params);
        System.out.println(output + "/" + subDir.getName() + "/" + cluster.getName());

        params.add("-out");
        params.add(cluster.getAbsolutePath() + "_Dependency");

        if (verbose) {
          params.add("-verbose");
        }

        KDDTask dependencyTask = new KDDTask();
        dependencyTask.setParameters(params.toArray(new String[params.size()]));
        dependencyTask.run();
      }

    }


  }

  /**
   * Runs a KDD task accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public static void main(String[] args) {
    ACEP acep = new ACEP();
    try {
      acep.setParameters(args);
      acep.runCOPAC();
      acep.runDependencyDerivator();
    }
    catch (AbortException e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }
    catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    catch (IllegalStateException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }


}
