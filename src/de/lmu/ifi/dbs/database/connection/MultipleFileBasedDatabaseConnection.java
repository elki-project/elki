package de.lmu.ifi.dbs.database.connection;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.database.ObjectAndAssociations;
import de.lmu.ifi.dbs.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.parser.ObjectAndLabels;
import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.ParsingResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;


/**
 * Provides a database connection based on multiple files and parsers to be set.
 *
 * @author Elke Achtert(<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MultipleFileBasedDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<MultiRepresentedObject<O>> {
  /**
   * A sign to separate components of a label.
   */
  public static final String LABEL_CONCATENATION = " ";

  /**
   * Default parser.
   */
  public final static String DEFAULT_PARSER = DoubleVectorLabelParser.class.getName();

  /**
   * Label for parameter parser.
   */
  public final static String PARSER_P = "parser";

  /**
   * Description of parameter parser.
   */
  public final static String PARSER_D = "<classname_1,...,classname_n>a comma separated list of parsers to provide a database (default: " + DEFAULT_PARSER + ")";

  /**
   * Label for parameter input.
   */
  public final static String INPUT_P = "in";

  /**
   * Description for parameter input.
   */
  public final static String INPUT_D = "<filename_1,...,filename_n>a comma separated list of input files to be parsed.";

  /**
   * A pattern defining a comma.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * The parsers.
   */
  private List<Parser<O>> parsers;

  /**
   * The input files to parse from.
   */
  private List<FileInputStream> inputStreams;

  /**
   * The name of the input files.
   */
  private String[] inputFiles;

  /**
   * Provides a database connection expecting input from several files.
   */
  public MultipleFileBasedDatabaseConnection() {
    super();
    forceExternalID = true;
    parameterToDescription.put(PARSER_P + OptionHandler.EXPECTS_VALUE, PARSER_D);
    parameterToDescription.put(INPUT_P + OptionHandler.EXPECTS_VALUE, INPUT_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see DatabaseConnection#getDatabase(de.lmu.ifi.dbs.normalization.Normalization)
   */
  public Database<MultiRepresentedObject<O>> getDatabase(Normalization<MultiRepresentedObject<O>> normalization) {
    try {
      // number of representations
      final int numberOfRepresentations = inputStreams.size();

      // comparator to sort the ObjectAndLabels lists provided by the parsers.
      Comparator<ObjectAndLabels<O>> comparator = new Comparator<ObjectAndLabels<O>>() {
        public int compare(ObjectAndLabels<O> o1, ObjectAndLabels<O> o2) {
          String classLabel1 = o1.getLabels().get(classLabelIndex);
          String classLabel2 = o2.getLabels().get(classLabelIndex);
          return classLabel1.compareTo(classLabel2);
        }
      };

      // parse
      List<ParsingResult<O>> parsingResults = new ArrayList<ParsingResult<O>>(numberOfRepresentations);
      int numberOfObjects = 0;
      for (int r = 0; r < numberOfRepresentations; r++) {
        ParsingResult<O> parsingResult = parsers.get(r).parse(inputStreams.get(r));
        parsingResults.add(parsingResult);
        numberOfObjects = Math.max(parsingResult.getObjectAndLabelList().size(), numberOfObjects);
        // sort the representations according to the external ids
        List<ObjectAndLabels<O>> objectAndLabelsList = parsingResult.getObjectAndLabelList();
        Collections.sort(objectAndLabelsList, comparator);
      }

      // build the multi-represented objects and their labels
      List<ObjectAndLabels<MultiRepresentedObject<O>>> objectAndLabelsList = new ArrayList<ObjectAndLabels<MultiRepresentedObject<O>>>();
      for (int i = 0; i < numberOfObjects; i++) {
        List<O> representations = new ArrayList<O>(numberOfRepresentations);
        List<String> labels = new ArrayList<String>();
        for (int r = 0; r < numberOfRepresentations; r++) {
          ParsingResult<O> parsingResult = parsingResults.get(r);
          representations.add(parsingResult.getObjectAndLabelList().get(i).getObject());
          List<String> rep_labels = parsingResult.getObjectAndLabelList().get(i).getLabels();
          for (String l : rep_labels) {
            if (! labels.contains(l))
              labels.add(l);
          }
        }
        objectAndLabelsList.add(new ObjectAndLabels<MultiRepresentedObject<O>>(new MultiRepresentedObject<O>(representations), labels));
      }

      // normalize objects and transform labels
      List<ObjectAndAssociations<MultiRepresentedObject<O>>> objectAndAssociationList = normalizeAndTransformLabels(objectAndLabelsList, normalization);

      // insert into database
      database.insert(objectAndAssociationList);

      return database;
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch (NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   *      todo
   */
  public String description() {
    return "";
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      // input files
      String input = optionHandler.getOptionValue(INPUT_P);
      inputFiles = SPLIT.split(input);
      if (inputFiles.length == 0) {
        throw new IllegalArgumentException("No input files specified.");
      }
      inputStreams = new ArrayList<FileInputStream>(inputFiles.length);
      for (String inputFile : inputFiles) {
        inputStreams.add(new FileInputStream(inputFile));
      }

      // parsers
      if (optionHandler.isSet(PARSER_P)) {
        String parsers = optionHandler.getOptionValue(PARSER_P);
        String[] parserClasses = SPLIT.split(parsers);
        if (parserClasses.length == 0) {
          throw new IllegalArgumentException("No parsers specified.");
        }
        if (parserClasses.length != inputStreams.size()) {
          throw new IllegalArgumentException("Number of parsers and input files does not match (" +
                                             parserClasses.length + " != " + inputFiles.length + ")!");
        }
        this.parsers = new ArrayList<Parser<O>>(parserClasses.length);
        for (String parserClass : parserClasses) {
          this.parsers.add(Util.instantiate(Parser.class, parserClass));
        }
      }
      else {
        this.parsers = new ArrayList<Parser<O>>(inputFiles.length);
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < inputFiles.length; i++) {
          this.parsers.add(Util.instantiate(Parser.class, DEFAULT_PARSER));
        }
      }

      // set parameters of parsers
      for (Parser<O> parser : this.parsers) {
        remainingParameters = parser.setParameters(remainingParameters);
      }
    }
    catch (FileNotFoundException e) {
      throw new IllegalArgumentException(e);
    }

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings attributeSettings = result.get(0);

    attributeSettings.addSetting(PARSER_P, parsers.toString());
    attributeSettings.addSetting(INPUT_P, Arrays.asList(inputFiles).toString());

    return result;
  }
}
