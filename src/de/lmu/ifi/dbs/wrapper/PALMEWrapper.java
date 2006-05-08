package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PALME;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.MultipleFileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.parser.SparseBitVectorLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Wrapper class for PALME algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALMEWrapper extends KDDTaskWrapper {
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

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public final static String INPUT_D = "<dirname>input directory to be parsed.";

  /**
   * Main method to run this wrapper.
   *
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    PALMEWrapper wrapper = new PALMEWrapper();
    try {
      wrapper.run(args);
    }
    catch (ParameterException e) {
      wrapper.logger.log(Level.SEVERE, wrapper.optionHandler.usage(e.getMessage()), e);
    }
  }

  public PALMEWrapper() {
    super();
    parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see KDDTaskWrapper#getParameters()
   */
  public List<String> getParameters() throws UnusedParameterException, NoParameterValueException {
    List<String> parameters = new ArrayList<String>();

    // input
    String input = optionHandler.getOptionValue(INPUT_P);
    List<File> files = new ArrayList<File>();
    getFiles(new File(input), files);
    if (files.isEmpty())
      throw new IllegalArgumentException("Input directory " + input + " contains no files!");
    Collections.sort(files);

    int representations = files.size();

    String inputFiles = "";
    for (File file : files) {
      if (inputFiles.length() == 0) inputFiles = file.getPath();
      else inputFiles += "," + file.getPath();
    }
    parameters.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.INPUT_P);
    parameters.add(inputFiles);

    // algorithm PALME
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    parameters.add(PALME.class.getName());

    // distance function
    parameters.add(OptionHandler.OPTION_PREFIX + RepresentationSelectingDistanceFunction.DISTANCE_FUNCTIONS_P);
//    String distanceFunctions = CosineDistanceFunction.class.getName();
    String distanceFunctions = EuklideanDistanceFunction.class.getName();
    for (int i = 1; i < representations; i++) {
//      distanceFunctions += "," + CosineDistanceFunction.class.getName();
      distanceFunctions += "," + EuklideanDistanceFunction.class.getName();
    }
    parameters.add(distanceFunctions);

    // normalization
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    params.add(MultiRepresentedObjectNormalization.class.getName());
//    params.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

    // normalizations
//    params.add(OptionHandler.OPTION_PREFIX + MultiRepresentedObjectNormalization.NORMALIZATION_P);
//    String normalizations = MultiRepresentedObjectNormalization.NO_NORMALIZATION;
//    for (int i = 1; i < representations; i++) {
//      normalizations += "," + AttributeWiseRealVectorNormalization.class.getName();
//    }
//    params.add(normalizations);

    // database connection
    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.DATABASE_CONNECTION_P);
    parameters.add(MultipleFileBasedDatabaseConnection.class.getName());

    // class label
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.CLASS_LABEL_INDEX_P);
    parameters.add("2");
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.CLASS_LABEL_CLASS_P);
    parameters.add(SimpleClassLabel.class.getName());

    // external id
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.EXTERNAL_ID_INDEX_P);
    parameters.add("1");

    // database
    parameters.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    parameters.add(SequentialDatabase.class.getName());

    // parsers
    parameters.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.PARSER_P);
    String parsers = SparseBitVectorLabelParser.class.getName();
    for (int i = 1; i < representations; i++) {
      parsers += "," + RealVectorLabelParser.class.getName();
    }
    parameters.add(parsers);

    return parameters;
  }

  private void getFiles(File dir, List<File> result) {
    File[] files = dir.listFiles();
    if (files == null) return;
    for (File file : files) {
      if (file.isDirectory()) getFiles(file, result);
      else result.add(file);
    }
  }
}
