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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
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
   * Holds the file to print results to.
   */
  private File out;

  /**
   * Normalization to use.
   */
  private Normalization<O> normalization;

  /**
   * Constructor.
   */
  public ResultWriter() {
    super();
    // parameter output file
    addOption(OUTPUT_PARAM);
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // output
    if(OUTPUT_PARAM.isSet()) {
      out = OUTPUT_PARAM.getValue();
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.result.ResultHandler#processResult(de.lmu.ifi.dbs.elki.database.Database, de.lmu.ifi.dbs.elki.result.Result, java.util.List)
   */
  public void processResult(Database<O> db, Result result, List<AttributeSettings> settings) {
    TextWriter<O> writer = new TextWriter<O>();
    if(normalization != null) {
      writer.setNormalization(normalization);
    }

    StreamFactory output;
    try {
      if(out == null) {
        output = new SingleStreamOutput();
      }
      else if(out.exists()) {
        if(out.isDirectory()) {
          if(out.listFiles().length > 0)
            warning("Output directory specified is not empty. Files will be overwritten and old files may be left over.");
          output = new MultipleFilesOutput(out);
        }
        else {
          warning("Output file exists and will be overwritten!");
          output = new SingleStreamOutput(out);
        }
      }
      else {
        output = new MultipleFilesOutput(out);
      }
    }
    catch(IOException e) {
      throw new IllegalStateException("Error opening output.", e);
    }
    try {
      writer.output(db, result, output, settings);
    }
    catch(IOException e) {
      throw new IllegalStateException("Input/Output error while writing result.", e);
    }
    catch(UnableToComplyException e) {
      throw new IllegalStateException("Unable to comply while writing result.", e);
    }
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.result.ResultHandler#setNormalization(de.lmu.ifi.dbs.elki.normalization.Normalization)
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
