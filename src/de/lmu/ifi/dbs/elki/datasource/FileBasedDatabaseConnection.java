package de.lmu.ifi.dbs.elki.datasource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.Parser;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 */
public class FileBasedDatabaseConnection extends InputStreamDatabaseConnection {
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
   * @param filters Filters, can be null
   * @param parser the parser to provide a database
   * @param in the input stream to parse from.
   */
  public FileBasedDatabaseConnection(List<ObjectFilter> filters, Parser parser, InputStream in) {
    super(filters, parser);
    this.in = in;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends InputStreamDatabaseConnection.Parameterizer {
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
    protected FileBasedDatabaseConnection makeInstance() {
      return new FileBasedDatabaseConnection(filters, parser, inputStream);
    }
  }
}