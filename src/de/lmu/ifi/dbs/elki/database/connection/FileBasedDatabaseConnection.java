package de.lmu.ifi.dbs.elki.database.connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
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
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends DatabaseObject> extends InputStreamDatabaseConnection.Parameterizer<O> {
    protected InputStream inputStream;

    @Override
    protected void makeOptions(Parameterization config) {
      // Add the input file first, for usability reasons.
      final FileParameter inputParam = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(inputParam)) {
        try {
          inputStream = new FileInputStream(inputParam.getValue());
          inputStream = FileUtil.tryGzipInput(inputStream);
        }
        catch(IOException e) {
          config.reportError(new WrongParameterValueException(inputParam, inputParam.getValue().getPath(), e));
          inputStream = null;
        }
      }
      super.makeOptions(config);
    }

    @Override
    protected FileBasedDatabaseConnection<O> makeInstance() {
      return new FileBasedDatabaseConnection<O>(database, classLabelIndex, classLabelClass, externalIdIndex, parser, startid, seed, inputStream);
    }
  }
}