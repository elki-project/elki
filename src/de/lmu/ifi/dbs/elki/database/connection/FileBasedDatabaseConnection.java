package de.lmu.ifi.dbs.elki.database.connection;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides a file based database connection based on the parser to be set.
 * 
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject to be provided by the implementing
 *        class as element of the supplied database
 */
public class FileBasedDatabaseConnection<O extends DatabaseObject> extends InputStreamDatabaseConnection<O> {
  /**
   * OptionID for {@link #INPUT_PARAM}
   */
  public static final OptionID INPUT_ID = OptionID.getOrCreateOptionID("dbc.in", "The name of the input file to be parsed.");

  /**
   * Parameter that specifies the name of the input file to be parsed.
   * <p>
   * Key: {@code -dbc.in}
   * </p>
   */
  private final FileParameter INPUT_PARAM = new FileParameter(INPUT_ID, FileParameter.FileType.INPUT_FILE);

  /**
   * Provides a file based database connection based on the parser to be set,
   * adding parameter {@link #INPUT_PARAM} to the option handler additionally to
   * parameters of super class.
   */
  public FileBasedDatabaseConnection() {
    super();
    addOption(INPUT_PARAM);
  }

  /**
   * Calls the super method InputStreamDatabaseConnection#setParameters(args)}
   * and sets additionally the value of the parameter {@link #INPUT_PARAM}.
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    try {
      in = new FileInputStream(INPUT_PARAM.getValue());
      in = tryGzipInput(in);
    }
    catch(IOException e) {
      throw new WrongParameterValueException(INPUT_PARAM, INPUT_PARAM.getValue().getPath(), e);
    }

    return remainingParameters;
  }
  
  /**
   * Try to open a stream as gzip, if it starts with the gzip magic.
   * 
   * TODO: move to utils package.
   * 
   * @param in original input stream
   * @return old input stream or a {@link GZIPInputStream} if appropriate.
   * @throws IOException
   */
  public static InputStream tryGzipInput(InputStream in) throws IOException {
    // try autodetecting gzip compression.
    if (!in.markSupported()) {
      PushbackInputStream pb = new PushbackInputStream(in, 16);
      in = pb;
      // read a magic from the file header
      byte[] magic = {0, 0};
      pb.read(magic);
      pb.unread(magic);
      if (magic[0] == 31 && magic[1] == -117) {
        in = new GZIPInputStream(pb);
      }
    } else
    if (in.markSupported()) {
      in.mark(16);
      if (in.read() == 31 && in.read() == -117) {
        in.reset();
        in = new GZIPInputStream(in);
      } else {
        // just rewind the stream
        in.reset();
      }
    }
    return in;
  }
}