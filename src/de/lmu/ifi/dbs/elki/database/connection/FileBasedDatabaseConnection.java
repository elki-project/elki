package de.lmu.ifi.dbs.elki.database.connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.parser.DoubleVectorLabelParser;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {
  /**
   * Parameter that specifies the name of the input file to be parsed.
   * <p>
   * Key: {@code -dbc.in}
   * </p>
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("dbc.in", "The name of the input file to be parsed.");

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
   * @param in the input stream to parse from.
   */
  public FileBasedDatabaseConnection(Database<O> database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex, Parser<O> parser, Integer startid, Long seed, InputStream in) {
    super(database, classLabelIndex, classLabelClass, externalIdIndex, parser, startid, seed);
    this.in = in;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return FileBasedDatabaseConnection
   */
  public static <O extends DatabaseObject> FileBasedDatabaseConnection<O> parameterize(Parameterization config) {
    Parameters<O> p = getParameters(config, Parser.class, DoubleVectorLabelParser.class);

    if(config.hasErrors()) {
      return null;
    }

    return new FileBasedDatabaseConnection<O>(p.database, p.classLabelIndex, p.classLabelClass, p.externalIdIndex, p.parser, p.startid, p.seed, p.in);
  }

  /**
   * Convenience method for getting parameter values.
   * 
   * @param <O> the type of DatabaseObject to be provided
   * @param config the parameterization
   * @param parserRestrictionClass the restriction class for the parser
   * @param parserDefaultValue the default value for the parser
   * @return parameter values
   */
  public static <O extends DatabaseObject> Parameters<O> getParameters(Parameterization config, Class<?> parserRestrictionClass, Class<?> parserDefaultValueClass) {
    InputStreamDatabaseConnection.Parameters<O> p = InputStreamDatabaseConnection.getParameters(config, parserRestrictionClass, parserDefaultValueClass);

    // parameter in
    final FileParameter inputParam = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
    InputStream in = null;
    if(config.grab(inputParam)) {
      try {
        in = new FileInputStream(inputParam.getValue());
        in = FileUtil.tryGzipInput(in);
      }
      catch(IOException e) {
        config.reportError(new WrongParameterValueException(inputParam, inputParam.getValue().getPath(), e));
        in = null;
      }
    }

    return new Parameters<O>(p.database, p.classLabelIndex, p.classLabelClass, p.externalIdIndex, p.parser, p.startid, p.seed, in);
  }

  /**
   * Encapsulates the parameter values for an
   * {@link FileBasedDatabaseConnection}. Convenience class for getting
   * parameter values.
   * 
   * @param <O> the type of DatabaseObject to be provided
   */
  static class Parameters<O extends DatabaseObject> extends InputStreamDatabaseConnection.Parameters<O> {
    InputStream in;

    public Parameters(Database<O> database, Integer classLabelIndex, Class<? extends ClassLabel> classLabelClass, Integer externalIdIndex, Parser<O> parser, Integer startid, Long seed, InputStream in) {
      super(database, classLabelIndex, classLabelClass, externalIdIndex, parser, startid, seed);
      this.in = in;
    }
  }
}