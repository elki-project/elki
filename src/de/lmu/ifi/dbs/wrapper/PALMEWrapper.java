package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.PALME;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.SequentialDatabase;
import de.lmu.ifi.dbs.database.connection.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.database.connection.MultipleFileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.CosineDistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.RepresentationSelectingDistanceFunction;
import de.lmu.ifi.dbs.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.parser.SparseBitVectorLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper class for PALME algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class PALMEWrapper extends AbstractWrapper {

  /**
   * Runs the PALME algorithm.
   */
  public void runPALME() {
    ArrayList<String> params = getRemainingParameters();

    // input
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
    params.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.INPUT_P);
    params.add(inputFiles);

    // algorithm PALME
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
    params.add(PALME.class.getName());

    // distance function
    params.add(OptionHandler.OPTION_PREFIX + RepresentationSelectingDistanceFunction.DISTANCE_FUNCTIONS_P);
    String distanceFunctions = CosineDistanceFunction.class.getName();
    for (int i = 1; i < representations; i++) {
      distanceFunctions += "," + EuklideanDistanceFunction.class.getName();
    }
    params.add(distanceFunctions);

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
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.DATABASE_CONNECTION_P);
    params.add(MultipleFileBasedDatabaseConnection.class.getName());

    // class label
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.CLASS_LABEL_INDEX_P);
    params.add("2");
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.CLASS_LABEL_CLASS_P);
    params.add(SimpleClassLabel.class.getName());

    // external id
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.EXTERNAL_ID_INDEX_P);
    params.add("1");

    // database
    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabaseConnection.DATABASE_CLASS_P);
    params.add(SequentialDatabase.class.getName());

    // distance cache
//    params.add(OptionHandler.OPTION_PREFIX + AbstractDatabase.CACHE_F);

    // parsers
    params.add(OptionHandler.OPTION_PREFIX + MultipleFileBasedDatabaseConnection.PARSER_P);
    String parsers = SparseBitVectorLabelParser.class.getName();
    for (int i = 1; i < representations; i++) {
      parsers += "," + RealVectorLabelParser.class.getName();
    }
    params.add(parsers);

    // output
    params.add(OptionHandler.OPTION_PREFIX + KDDTask.OUTPUT_P);
    params.add(output);

    if (time) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.TIME_F);
    }

    if (verbose) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    KDDTask task = new KDDTask();
    task.setParameters(params.toArray(new String[params.size()]));
    task.run();
  }

  /**
   * Runs the COPAC algorithm accordingly to the specified parameters.
   *
   * @param args parameter list according to description
   */
  public void run(String[] args) {
    this.setParameters(args);
    this.runPALME();
  }

  public static void main(String[] args) {
    PALMEWrapper optics = new PALMEWrapper();
    try {
      optics.run(args);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
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
