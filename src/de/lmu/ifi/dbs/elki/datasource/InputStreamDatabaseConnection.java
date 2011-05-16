package de.lmu.ifi.dbs.elki.datasource;

import java.io.InputStream;
import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Provides a database connection expecting input from an input stream such as
 * stdin.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses Parser oneway - - runs
 * @apiviz.uses ParsingResult oneway - - processes
 * 
 */
@Title("Input-Stream based database connection")
@Description("Parse an input stream such as STDIN into a database.")
public class InputStreamDatabaseConnection extends AbstractDatabaseConnection {
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
   * Holds the instance of the parser.
   */
  Parser parser;

  /**
   * The input stream to parse from.
   */
  InputStream in = System.in;

  /**
   * Constructor.
   * 
   * @param filters Filters to use
   * @param parser the parser to provide a database
   */
  public InputStreamDatabaseConnection(List<ObjectFilter> filters, Parser parser) {
    super(filters);
    this.parser = parser;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    // Run parser
    if(logger.isDebugging()) {
      logger.debugFine("Invoking parsers.");
    }
    MultipleObjectsBundle parsingResult = parser.parse(in);

    // normalize objects and transform labels
    if(logger.isDebugging()) {
      logger.debugFine("Invoking filters.");
    }
    MultipleObjectsBundle objects = invokeFilters(parsingResult);
    return objects;
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    Parser parser = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configParser(config, Parser.class, DoubleVectorLabelParser.class);
      configFilters(config);
    }

    protected void configParser(Parameterization config, Class<?> parserRestrictionClass, Class<?> parserDefaultValueClass) {
      ObjectParameter<Parser> parserParam = new ObjectParameter<Parser>(PARSER_ID, parserRestrictionClass, parserDefaultValueClass);
      if(config.grab(parserParam)) {
        parser = parserParam.instantiateClass(config);
      }
    }

    @Override
    protected InputStreamDatabaseConnection makeInstance() {
      return new InputStreamDatabaseConnection(filters, parser);
    }
  }
}