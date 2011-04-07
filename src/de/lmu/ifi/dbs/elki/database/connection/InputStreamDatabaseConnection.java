package de.lmu.ifi.dbs.elki.database.connection;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a database connection expecting input from an input stream such as
 * stdin.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses Parser oneway - - runs
 * @apiviz.uses ParsingResult oneway - - processes
 * 
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
@Title("Input-Stream based database connection")
@Description("Parse an input stream such as STDIN into a database.")
public class InputStreamDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(InputStreamDatabaseConnection.class);

  /**
   * Parameter to specify the parser to provide a database.
   * <p>
   * Key: {@code -dbc.parser}
   * </p>
   */
  public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID("dbc.parser", "Parser to provide the database.");

  /**
   * Optional parameter to specify a seed for randomly shuffling the rows of the
   * database. If unused, no shuffling will be performed. Shuffling takes time
   * linearly dependent from the size of the database.
   * <p>
   * Key: {@code -dbc.seed}
   * </p>
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("dbc.seed", "Seed for randomly shuffling the rows for the database. If the parameter is not set, no shuffling will be performed.");

  /**
   * Optional parameter to specify the first object ID to use.
   * <p>
   * Key: {@code -dbc.startid}
   * </p>
   */
  public static final OptionID IDSTART_ID = OptionID.getOrCreateOptionID("dbc.startid", "Object ID to start counting with");

  /**
   * Holds the instance of the parser.
   */
  Parser<O> parser;

  /**
   * The input stream to parse from.
   */
  InputStream in = System.in;

  /**
   * ID to start enumerating objects with.
   */
  Integer startid = null;

  /**
   * Seed for randomly shuffling the rows of the database. If null, no shuffling
   * will be performed. Shuffling takes time linearly dependent from the size of
   * the database.
   */
  Long seed = null;

  /**
   * Constructor.
   * 
   * @param database the instance of the database
   * @param classLabelIndex the index of the label to be used as class label,
   *        can be null
   * @param classLabelClass the association of occurring class labels
   * @param externalIdIndex the index of the label to be used as external id,
   *        can be null
   * @param parser the parser to provide a database
   * @param startid the first object ID to use, can be null
   * @param seed a seed for randomly shuffling the rows of the database
   */
  public InputStreamDatabaseConnection(Database<O> database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex, Parser<O> parser, Integer startid, Long seed) {
    super(database, classLabelIndex, classLabelClass, externalIdIndex);
    this.parser = parser;
    this.startid = startid;
    this.seed = seed;
  }

  @Override
  public Database<O> getDatabase(Normalization<O> normalization) {
    try {
      if(logger.isDebugging()) {
        logger.debugFine("*** parse");
      }

      // parse
      ParsingResult<O> parsingResult = parser.parse(in);
      // normalize objects and transform labels
      List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList = normalizeAndTransformLabels(parsingResult.getObjectAndLabelList(), normalization);

      if(seed != null) {
        if(logger.isDebugging()) {
          logger.debugFine("*** shuffle");
        }
        Random random = new Random(seed);
        Collections.shuffle(objectAndAssociationsList, random);
      }
      if(startid != null) {
        int id = startid;
        for(Pair<O, DatabaseObjectMetadata> pair : objectAndAssociationsList) {
          pair.first.setID(DBIDUtil.importInteger(id));
          id++;
        }
      }

      if(logger.isDebugging()) {
        logger.debugFine("*** insert");
      }
      // insert into database
      database.setObjectFactory(parsingResult.getObjectFactory());
      database.insert(objectAndAssociationsList);

      return database;
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
    catch(NonNumericFeaturesException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends AbstractDatabaseConnection.Parameterizer<O> {
    Parser<O> parser = null;

    Integer startid = null;

    Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configParser(config, Parser.class, DoubleVectorLabelParser.class);
      configClassLabel(config);
      configExternalId(config);
      configSeed(config);
      configStartid(config);
      configDatabase(config);
    }

    protected void configParser(Parameterization config, Class<?> parserRestrictionClass, Class<?> parserDefaultValueClass) {
      ObjectParameter<Parser<O>> parserParam = new ObjectParameter<Parser<O>>(PARSER_ID, parserRestrictionClass, parserDefaultValueClass);
      if(config.grab(parserParam)) {
        parser = parserParam.instantiateClass(config);
      }
    }

    protected void configSeed(Parameterization config) {
      LongParameter seedParam = new LongParameter(SEED_ID, true);
      if(config.grab(seedParam)) {
        seed = seedParam.getValue();
      }
    }

    protected void configStartid(Parameterization config) {
      IntParameter startidParam = new IntParameter(IDSTART_ID, true);
      if(config.grab(startidParam)) {
        startid = startidParam.getValue();
      }
    }

    @Override
    protected InputStreamDatabaseConnection<O> makeInstance() {
      return new InputStreamDatabaseConnection<O>(database, classLabelIndex, classLabelClass, externalIdIndex, parser, startid, seed);
    }
  }
}