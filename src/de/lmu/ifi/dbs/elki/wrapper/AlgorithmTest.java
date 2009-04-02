package de.lmu.ifi.dbs.elki.wrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Class that runs all specified algorithms with default parametrization.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class AlgorithmTest extends AbstractParameterizable {
  /**
   * OptionID for {@link #ALGORITHMS_PARAM}
   */
  public static final OptionID ALGORITHMS_ID = OptionID.getOrCreateOptionID("test.algorithms", "A comma separated list of classnames specifying the algorithms to be run with default parametrization " + Properties.ELKI_PROPERTIES.restrictionString(Algorithm.class) + ". If this parameter is not set all algorithms as specified in the property file are used.");

  /**
   * Optional parameter to specify the algorithms to be run with default
   * parametrization, must extend {@link Algorithm}. If this parameter is not
   * set all algorithms as specified in the property file are used.
   * <p>
   * Key: {@code -test.algorithms}
   * </p>
   */
  private final ClassListParameter<Algorithm<?, ?>> ALGORITHMS_PARAM = new ClassListParameter<Algorithm<?, ?>>(ALGORITHMS_ID, Algorithm.class, true);

  /**
   * Holds the instances of the algorithms specified by
   * {@link #ALGORITHMS_PARAM}.
   */
  private List<Algorithm<?, ?>> algorithms;

  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("test.in", "The name of the input file to be parsed. " + "This file is used for all specified algorithms as input file.");

  /**
   * Parameter that specifies the name of the input file to be parsed. This file
   * is used for all specified algorithms as input file.
   * <p>
   * Key: {@code -test.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * Holds the value of {@link #INPUT_PARAM}.
   */
  private File input;

  /**
   * OptionID for {@link #OUTPUT_PARAM}
   */
  public static final OptionID OUTPUT_ID = OptionID.getOrCreateOptionID("test.out", "Name of the directory to write the results of the specified algorithms in. " + "The result of each algorithm is written into a file/directory with the name of " + "the class of the according algorithm.");

  /**
   * Parameter to specify the directory to write the results of the specified
   * algorithms in. The result of each algorithm is written into a
   * file/directory with the name of the class of the according algorithm.
   * <p>
   * Key: {@code -test.out}
   * </p>
   */
  private final FileParameter OUTPUT_PARAM = new FileParameter(OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE);

  /**
   * Holds the value of {@link #OUTPUT_PARAM}.
   */
  private File output;

  /**
   * Main method to run this test class.
   * 
   * @param args the arguments to run this test class
   */
  public static void main(String[] args) {
    AlgorithmTest algorithmTest = new AlgorithmTest();
    try {
      algorithmTest.setParameters(args);
    }
    catch(Exception e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      LoggingUtil.exception(algorithmTest.optionHandler.usage(e.getMessage()), cause);
    }

    algorithmTest.run();
  }

  /**
   * Provides a class that runs all specified algorithms with default
   * parametrization, adding parameters {@link #ALGORITHMS_PARAM},
   * {@link #INPUT_PARAM}, and {@link #OUTPUT_PARAM} to the option handler
   * additionally to parameters of super class.
   */
  public AlgorithmTest() {
    super();
    // parameter parser
    addOption(ALGORITHMS_PARAM);
    // parameter input
    addOption(INPUT_PARAM);
    // parameter output
    addOption(OUTPUT_PARAM);
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable#setParameters(String[])}
   * AbstractParameterizable#setParameters(args)} and sets additionally the
   * values of the parameters {@link #INPUT_PARAM}, and {@link #OUTPUT_PARAM}
   * and instantiates {@link #algorithms} according to the value of parameter
   * {@link #ALGORITHMS_PARAM} .
   * 
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // input
    input = INPUT_PARAM.getValue();

    // output
    output = OUTPUT_PARAM.getValue();
    if(!output.isDirectory()) {
      throw new WrongParameterValueException("Required parameter " + OUTPUT_PARAM.getName() + " is no (valid) directory!");
    }

    // algorithms
    if(ALGORITHMS_PARAM.isSet()) {
      algorithms = ALGORITHMS_PARAM.instantiateClasses();
    }
    else {
      List<Class<?>> subclasses = Properties.ELKI_PROPERTIES.subclasses(Algorithm.class);
      this.algorithms = new ArrayList<Algorithm<?, ?>>(subclasses.size());
      for(Class<?> subclass : subclasses) {
        try {
          this.algorithms.add(ClassGenericsUtil.instantiate(Algorithm.class, subclass.getName()));
        }
        catch(UnableToComplyException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return remainingParameters;
  }

  /**
   * Calls the super method and adds to the returned attribute settings the
   * attribute settings of all instances of {@link #algorithms}.
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    for(Algorithm<?, ?> algorithm : algorithms) {
      attributeSettings.addAll(algorithm.getAttributeSettings());
    }
    return attributeSettings;
  }

  /**
   * Runs the specified algorithms with default parametrization.
   */
  private void run() {
    for(Algorithm<?, ?> algorithm : algorithms) {
      // fehler drin
      if(!algorithm.getClass().getSimpleName().startsWith("Dependency")) {
        continue;
      }

      // if (! algorithm.getClass().getSimpleName().startsWith("DiSH")) {
      // continue;
      // }

      logger.verbose("*******************************************************************" + "\nRunning " + algorithm.getClass().getName());

      FileBasedDatabaseConnectionWrapper<?> wrapper;
      try {
        wrapper = ClassGenericsUtil.instantiate(FileBasedDatabaseConnectionWrapper.class, algorithm.getClass().getSimpleName() + "Wrapper");
      }
      catch(UnableToComplyException e) {
        logger.warning("No wrapper class for " + algorithm.getClass() + " available (" + e.getMessage() + ")", e);
        continue;
      }

      try {
        wrapper.setParameters(buildParameters(algorithm));
        wrapper.run();
      }
      catch(UnableToComplyException e) {
        logger.warning(algorithm.getClass() + " is unable to comply: " + e.getMessage(), e);
      }
      catch(ParameterException e) {
        logger.warning(algorithm.getClass() + " has a parameter exception: " + e.getMessage(), e);
      }

    }

  }

  /**
   * Returns the array of parameters for the specified algorithm.
   * 
   * @param algorithm the algorithm object
   * @return the array of parameters for the specified algorithm
   */
  private String[] buildParameters(Algorithm<?, ?> algorithm) {
    List<String> parameters = new ArrayList<String>();

    // in
    OptionUtil.addParameter(parameters, FileBasedDatabaseConnection.INPUT_ID, input.getPath());
    // out
    OptionUtil.addParameter(parameters, OptionID.OUTPUT, output.getPath() + File.separator + algorithm.getClass().getSimpleName());

    // verbose
    OptionUtil.addFlag(parameters, OptionID.ALGORITHM_VERBOSE);

    return parameters.toArray(new String[parameters.size()]);
  }

}
