package de.lmu.ifi.dbs.elki.result;

import java.io.File;
import java.io.IOException;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.result.textwriter.MultipleFilesOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.SingleStreamOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.StreamFactory;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriter;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;

/**
 * Result handler that feeds the data into a TextWriter
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public class ResultWriter<O extends DatabaseObject> extends AbstractParameterizable implements ResultHandler<O, Result> {
  /**
   * Optional Parameter to specify the file to write the obtained results in. If
   * this parameter is omitted, per default the output will sequentially be
   * given to STDOUT.
   * <p>
   * Key: {@code -out}
   * </p>
   */
  private final FileParameter OUTPUT_PARAM = new FileParameter(OptionID.OUTPUT, FileParameter.FileType.OUTPUT_FILE, true);

  /**
   * GZIP compression flag.
   * 
   */
  private final OptionID GZIP_OUTPUT = OptionID.getOrCreateOptionID("out.gzip", "Enable gzip compression of output files.");
  
  /**
   * Flag to control GZIP compression.
   * <p>Key: {@code -out.gzip}</p>
   */
  private final Flag GZIP_FLAG = new Flag(GZIP_OUTPUT);
  
  /**
   * Suppress overwrite warning.
   * 
   */
  private final OptionID OVERWRITE_OPTION = OptionID.getOrCreateOptionID("out.silentoverwrite", "Silently overwrite output files.");
  
  /**
   * Flag to suppress overwrite warning.
   * <p>Key: {@code -out.silentoverwrite}</p>
   */
  private final Flag OVERWRITE_FLAG = new Flag(OVERWRITE_OPTION);
  
  /**
   * Holds the file to print results to.
   */
  private File out;

  /**
   * Normalization to use.
   */
  private Normalization<O> normalization;
  
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
   */
  public ResultWriter() {
    super();
    // parameter output file
    addOption(OUTPUT_PARAM);
    addOption(GZIP_FLAG);
    addOption(OVERWRITE_FLAG);
  }

  /**
   * set Parameters
   */
  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    // output
    if(OUTPUT_PARAM.isSet()) {
      out = OUTPUT_PARAM.getValue();
    }
    gzip = GZIP_FLAG.isSet();
    warnoverwrite = OVERWRITE_FLAG.isSet() != true; // inversed!
    rememberParametersExcept(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Process a single result.
   * 
   * @param db Database 
   * @param result Result
   */
  public void processResult(Database<O> db, Result result) {
    TextWriter<O> writer = new TextWriter<O>();
    if(normalization != null) {
      writer.setNormalization(normalization);
    }

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
          if (warnoverwrite) {
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
   * @param normalization Normalization to use
   * @see de.lmu.ifi.dbs.elki.result.ResultHandler#setNormalization
   */
  public void setNormalization(Normalization<O> normalization) {
    this.normalization = normalization;
  }

  /**
   * Getter for normalization
   * 
   * @return normalization object
   */
  public Normalization<O> getNormalization() {
    return normalization;
  }
}