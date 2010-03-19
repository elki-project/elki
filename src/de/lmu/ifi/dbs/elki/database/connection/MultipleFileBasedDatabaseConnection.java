package de.lmu.ifi.dbs.elki.database.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileListParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a database connection based on multiple files and parsers to be set.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public class MultipleFileBasedDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<MultiRepresentedObject<O>> {
  /**
   * OptionID for {@link #PARSERS_PARAM}
   */
  public static final OptionID PARSERS_ID = OptionID.getOrCreateOptionID("multipledbc.parsers", "Comma separated list of classnames specifying the parsers to provide a database. If this parameter is not set, " + DoubleVectorLabelParser.class.getName() + " is used as parser for all input files.");

  /**
   * Optional parameter to specify the parsers to provide a database, must
   * extend {@link Parser}. If this parameter is not set,
   * {@link DoubleVectorLabelParser} is used as parser for all input files.
   * <p>
   * Key: {@code -multipledbc.parsers}
   * </p>
   */
  private final ClassListParameter<Parser<O>> PARSERS_PARAM = new ClassListParameter<Parser<O>>(PARSERS_ID, Parser.class, true);

  /**
   * Holds the instances of the parsers specified by {@link #PARSERS_PARAM}.
   */
  private List<Parser<O>> parsers;

  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("multipledbc.in", "A comma separated list of the names of the input files to be parsed.");

  /**
   * Parameter that specifies the names of the input files to be parsed.
   * <p>
   * Key: {@code -multipledbc.in}
   * </p>
   */
  private final FileListParameter INPUT_PARAM = new FileListParameter(INPUT_ID, FileListParameter.FilesType.INPUT_FILES);

  /**
   * The input files to parse from as specified by {@link #INPUT_PARAM}.
   */
  private List<InputStream> inputStreams;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public MultipleFileBasedDatabaseConnection(Parameterization config) {
    super(config, true);

    config.grab(INPUT_PARAM);
    config.grab(PARSERS_PARAM);

    // parameter file list
    if(INPUT_PARAM.isDefined()) {
      // input files
      List<File> inputFiles = INPUT_PARAM.getValue();
      inputStreams = new ArrayList<InputStream>(inputFiles.size());
      for(File inputFile : inputFiles) {
        try {
          inputStreams.add(FileUtil.tryGzipInput(new FileInputStream(inputFile)));
        }
        catch(FileNotFoundException e) {
          config.reportError(new WrongParameterValueException(INPUT_PARAM, inputFiles.toString(), e));
        }
        catch(IOException e) {
          config.reportError(new WrongParameterValueException(INPUT_PARAM, inputFiles.toString(), e));
        }
      }
      // parameter parser
      if(PARSERS_PARAM.isDefined()) {
        parsers = PARSERS_PARAM.instantiateClasses(config);

        if(parsers.size() != inputStreams.size()) {
          config.reportError(new WrongParameterValueException("Number of parsers and input files does not match (" + parsers.size() + " != " + inputStreams.size() + ")!"));
        }
      }
      else {
        this.parsers = new ArrayList<Parser<O>>(inputStreams.size());
        for(int i = 0; i < inputStreams.size(); i++) {
          Parser<O> parser = ClassGenericsUtil.castWithGenericsOrNull(Parser.class, new DoubleVectorLabelParser(config));
          this.parsers.add(i, parser);
        }
      }
    }
  }

  public Database<MultiRepresentedObject<O>> getDatabase(Normalization<MultiRepresentedObject<O>> normalization) {
    try {
      // number of representations
      final int numberOfRepresentations = inputStreams.size();

      // comparator to sort the ObjectAndLabels lists provided by the
      // parsers.
      Comparator<Pair<O, List<String>>> comparator = new Comparator<Pair<O, List<String>>>() {
        public int compare(Pair<O, List<String>> o1, Pair<O, List<String>> o2) {
          String classLabel1 = o1.getSecond().get(classLabelIndex);
          String classLabel2 = o2.getSecond().get(classLabelIndex);
          return classLabel1.compareTo(classLabel2);
        }
      };

      // parse
      List<ParsingResult<O>> parsingResults = new ArrayList<ParsingResult<O>>(numberOfRepresentations);
      int numberOfObjects = 0;
      for(int r = 0; r < numberOfRepresentations; r++) {
        ParsingResult<O> parsingResult = parsers.get(r).parse(inputStreams.get(r));
        parsingResults.add(parsingResult);
        numberOfObjects = Math.max(parsingResult.getObjectAndLabelList().size(), numberOfObjects);
        // sort the representations according to the external ids
        List<Pair<O, List<String>>> objectAndLabelsList = parsingResult.getObjectAndLabelList();
        Collections.sort(objectAndLabelsList, comparator);
      }

      // build the multi-represented objects and their labels
      List<Pair<MultiRepresentedObject<O>, List<String>>> objectAndLabelsList = new ArrayList<Pair<MultiRepresentedObject<O>, List<String>>>();
      for(int i = 0; i < numberOfObjects; i++) {
        List<O> representations = new ArrayList<O>(numberOfRepresentations);
        List<String> labels = new ArrayList<String>();
        for(int r = 0; r < numberOfRepresentations; r++) {
          ParsingResult<O> parsingResult = parsingResults.get(r);
          representations.add(parsingResult.getObjectAndLabelList().get(i).getFirst());
          List<String> rep_labels = parsingResult.getObjectAndLabelList().get(i).getSecond();
          for(String l : rep_labels) {
            if(!labels.contains(l)) {
              labels.add(l);
            }
          }
        }
        objectAndLabelsList.add(new Pair<MultiRepresentedObject<O>, List<String>>(new MultiRepresentedObject<O>(representations), labels));
      }

      // normalize objects and transform labels
      List<Pair<MultiRepresentedObject<O>, Associations>> objectAndAssociationList = normalizeAndTransformLabels(objectAndLabelsList, normalization);

      // insert into database
      database.insert(objectAndAssociationList);

      return database;
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch(NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }
}
