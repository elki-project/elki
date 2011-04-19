package de.lmu.ifi.dbs.elki.result;

import java.io.File;
import java.io.IOException;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.textwriter.MultipleFilesOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.SingleStreamOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.StreamFactory;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * Result handler that feeds the data into a TextWriter
 * 
 * @author Erich Schubert
 */
public class ResultWriter implements ResultHandler {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(ResultWriter.class);

  /**
   * Flag to control GZIP compression.
   * <p>
   * Key: {@code -out.gzip}
   * </p>
   */
  public static final OptionID GZIP_OUTPUT_ID = OptionID.getOrCreateOptionID("out.gzip", "Enable gzip compression of output files.");

  /**
   * Flag to suppress overwrite warning.
   * <p>
   * Key: {@code -out.silentoverwrite}
   * </p>
   */
  public static final OptionID OVERWRITE_OPTION_ID = OptionID.getOrCreateOptionID("out.silentoverwrite", "Silently overwrite output files.");

  /**
   * Holds the file to print results to.
   */
  private File out;

  /**
   * Whether or not to do gzip compression on output.
   */
  private boolean gzip = false;

  /**
   * Whether or not to warn on overwrite
   */
  private boolean warnoverwrite = true;

  /**
   * Constructor.
   * 
   * @param out
   * @param gzip
   * @param warnoverwrite
   */
  public ResultWriter(File out, boolean gzip, boolean warnoverwrite) {
    super();
    this.out = out;
    this.gzip = gzip;
    this.warnoverwrite = warnoverwrite;
  }

  /**
   * Process a single result.
   * 
   * @param db Database
   * @param result Result
   */
  @Override
  public void processResult(Database db, Result result) {
    TextWriter writer = new TextWriter();

    StreamFactory output;
    try {
      if(out == null) {
        output = new SingleStreamOutput(gzip);
      }
      else if(out.exists()) {
        if(out.isDirectory()) {
          if(warnoverwrite && out.listFiles().length > 0) {
            logger.warning("Output directory specified is not empty. Files will be overwritten and old files may be left over.");
          }
          output = new MultipleFilesOutput(out, gzip);
        }
        else {
          if(warnoverwrite) {
            logger.warning("Output file exists and will be overwritten!");
          }
          output = new SingleStreamOutput(out, gzip);
        }
      }
      else {
        // If it doesn't exist yet, make a MultipleFilesOutput.
        output = new MultipleFilesOutput(out, gzip);
      }
    }
    catch(IOException e) {
      throw new IllegalStateException("Error opening output.", e);
    }
    try {
      writer.output(db, result, output);
    }
    catch(IOException e) {
      throw new IllegalStateException("Input/Output error while writing result.", e);
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException("Unable to comply while writing result.", e);
    }
    output.closeAllStreams();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the file to print results to.
     */
    private File out = null;

    /**
     * Whether or not to do gzip compression on output.
     */
    private boolean gzip = false;

    /**
     * Whether or not to warn on overwrite
     */
    private boolean warnoverwrite = true;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(OptionID.OUTPUT, FileParameter.FileType.OUTPUT_FILE, true);
      if(config.grab(outputP)) {
        out = outputP.getValue();
      }

      Flag gzipF = new Flag(GZIP_OUTPUT_ID);
      if(config.grab(gzipF)) {
        gzip = gzipF.getValue();
      }

      Flag overwriteF = new Flag(OVERWRITE_OPTION_ID);
      if(config.grab(overwriteF)) {
        // note: inversed meaning
        warnoverwrite = !overwriteF.getValue();
      }
    }

    @Override
    protected ResultWriter makeInstance() {
      return new ResultWriter(out, gzip, warnoverwrite);
    }
  }
}