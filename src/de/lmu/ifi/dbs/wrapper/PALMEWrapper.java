package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PALME;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.MultipleFileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.CosineDistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.parser.SparseBitVectorLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper class for PALME algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALMEWrapper extends AbstractAlgorithmWrapper {
  public static void main(String[] args) {
    PALMEWrapper wrapper = new PALMEWrapper();
    try {
      wrapper.run(args);
    }
    catch (WrongParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (NoParameterValueException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
    catch (UnusedParameterException e) {
      System.err.println(wrapper.optionHandler.usage(e.getMessage()));
    }
  }

  /**
   * @see AbstractAlgorithmWrapper#addParameters(java.util.List<java.lang.String>)
   */
  public void addParameters(List<String> parameters) {
    // remove file based db-connection
    int index = parameters.indexOf(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.INPUT_P);
    parameters.remove(index);
    parameters.remove(index);

    // input
    List<File> files = new ArrayList<File>();
    getFiles(new File(getInput()), files);
    if (files.isEmpty())
      throw new IllegalArgumentException("Input directory " + getInput() + " contains no files!");
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
    String distanceFunctions = CosineDistanceFunction.class.getName();
    for (int i = 1; i < representations; i++) {
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
