package de.lmu.ifi.dbs.elki.database.connection;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Associations;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.NonNumericFeaturesException;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.ParsingResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a database connection expecting input from an input stream such as
 * stdin.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public class InputStreamDatabaseConnection<O extends DatabaseObject> extends AbstractDatabaseConnection<O> implements Parameterizable {
  /**
   * OptionID for {@link #PARSER_PARAM}
   */
  public static final OptionID PARSER_ID = OptionID.getOrCreateOptionID("dbc.parser", "Parser to provide the database.");

  /**
   * OptionID for {@link #SEED_PARAM}.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("dbc.seed", "Seed for randomly shuffling the rows for the database. If the parameter is not set, no shuffling will be performed.");

  /**
   * Parameter to specify a seed for randomly shuffling the rows of the
   * database. If unused, no shuffling will be performed. Shuffling takes time
   * linearly dependent from the size of the database.
   * <p>
   * Key: {@code -dbc.seed}
   * </p>
   */
  private final LongParameter SEED_PARAM = new LongParameter(SEED_ID, true);
  
  /**
   * Parameter to specify the parser to provide a database, must extend
   * {@link Parser}.
   * <p>
   * Default value: {@link DoubleVectorLabelParser}
   * </p>
   * <p>
   * Key: {@code -dbc.parser}
   * </p>
   */
  private final ObjectParameter<Parser<O>> PARSER_PARAM = new ObjectParameter<Parser<O>>(PARSER_ID, Parser.class, DoubleVectorLabelParser.class);

  /**
   * Holds the instance of the parser specified by {@link #PARSER_PARAM}.
   */
  Parser<O> parser;

  /**
   * The input stream to parse from.
   */
  InputStream in = System.in;

  /**
   * Adds parameters {@link #PARSER_PARAM} and {@link #SEED_PARAM} to the option
   * handler additionally to parameters of super class.
   */
  public InputStreamDatabaseConnection(Parameterization config) {
    super(config, false);
    if (config.grab(PARSER_PARAM)) {
      parser = PARSER_PARAM.instantiateClass(config);
    }
  }

  public Database<O> getDatabase(Normalization<O> normalization) {
    try {
      if(logger.isDebugging()) {
        logger.debugFine("*** parse");
      }

      // parse
      ParsingResult<O> parsingResult = parser.parse(in);
      // normalize objects and transform labels
      List<Pair<O, Associations>> objectAndAssociationsList = normalizeAndTransformLabels(parsingResult.getObjectAndLabelList(), normalization);

      if(SEED_PARAM.isDefined()) {
        if(logger.isDebugging()) {
          logger.debugFine("*** shuffle");
        }
        Random random = new Random(SEED_PARAM.getValue());
        Collections.shuffle(objectAndAssociationsList, random);
      }

      if(logger.isDebugging()) {
        logger.debugFine("*** insert");
      }
      // insert into database
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

  @Override
  public Description getDescription() {
    return new Description(this.getClass().getName(), "Input-Stream based database connection", "Parse an input stream such as STDIN into a database.");
  }
}
