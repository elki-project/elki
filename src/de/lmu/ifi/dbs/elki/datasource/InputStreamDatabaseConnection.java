package de.lmu.ifi.dbs.elki.datasource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
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
  Parser parser;

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
   * @param filters Filters to use
   * @param parser the parser to provide a database
   * @param startid the first object ID to use, can be null
   * @param seed a seed for randomly shuffling the rows of the database
   */
  public InputStreamDatabaseConnection(Database database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex, List<ObjectFilter> filters, Parser parser, Integer startid, Long seed) {
    super(database, classLabelIndex, classLabelClass, externalIdIndex, filters);
    this.parser = parser;
    this.startid = startid;
    this.seed = seed;
  }

  @Override
  public Database getDatabase() {
    try {
      if(logger.isDebugging()) {
        logger.debugFine("*** parse");
      }

      // parse
      MultipleObjectsBundle parsingResult = parser.parse(in);
      // normalize objects and transform labels
      MultipleObjectsBundle objects = transformLabels(parsingResult);

      /*
       * FIXME: re-add if(seed != null) { if(logger.isDebugging()) {
       * logger.debugFine("*** shuffle"); } Random random = new Random(seed);
       * Collections.shuffle(objectAndAssociationsList, random); }
       */

      // Add DBIDs
      // TODO: make this a "filter"?
      if(startid != null) {
        BundleMeta meta = new BundleMeta();
        meta.add(TypeUtil.DBID);
        for(int j = 0; j < objects.metaLength(); j++) {
          meta.add(objects.meta(j));
        }
        List<Object> ids = new ArrayList<Object>(objects.dataLength());
        for(int i = 0; i < objects.dataLength(); i++) {
          ids.add(DBIDUtil.importInteger(startid + i));
        }
        ArrayList<List<Object>> columns = new ArrayList<List<Object>>(meta.size());
        columns.add(ids);
        for(int j = 0; j < objects.metaLength(); j++) {
          columns.add(objects.getColumn(j));
        }
        // Replace result package
        objects = new MultipleObjectsBundle(meta, columns);
      }

      if(logger.isDebugging()) {
        logger.debugFine("*** insert");
      }
      // insert into database
      database.insert(objects);

      return database;
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
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
      configFilters(config);
    }

    protected void configParser(Parameterization config, Class<?> parserRestrictionClass, Class<?> parserDefaultValueClass) {
      ObjectParameter<Parser> parserParam = new ObjectParameter<Parser>(PARSER_ID, parserRestrictionClass, parserDefaultValueClass);
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
    protected InputStreamDatabaseConnection makeInstance() {
      return new InputStreamDatabaseConnection(database, classLabelIndex, classLabelClass, externalIdIndex, filters, parser, startid, seed);
    }
  }
}