/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import elki.logging.Logging;
import elki.result.textwriter.MultipleFilesOutput;
import elki.result.textwriter.SingleStreamOutput;
import elki.result.textwriter.StreamFactory;
import elki.result.textwriter.TextWriter;
import elki.utilities.Priority;
import elki.utilities.io.FileUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameterization.UnParameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.PatternParameter;
import elki.workflow.OutputStep;

/**
 * Result handler that feeds the data into a TextWriter.
 * <p>
 * Note: these classes need to be rewritten. Contributions welcome!
 *
 * @author Erich Schubert
 * @since 0.2
 */
@Priority(Priority.IMPORTANT)
public class ResultWriter implements ResultHandler {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(ResultWriter.class);

  /**
   * Holds the file to print results to.
   */
  private Path out;

  /**
   * Whether or not to do gzip compression on output.
   */
  private boolean gzip = false;

  /**
   * Whether or not to warn on overwrite
   */
  private boolean warnoverwrite = true;

  /**
   * Result filter pattern. Optional!
   */
  private Pattern filter = null;

  /**
   * Constructor.
   *
   * @param out Output file
   * @param gzip Gzip compression
   * @param warnoverwrite Warn before overwriting files
   * @param filter Filter pattern
   */
  public ResultWriter(Path out, boolean gzip, boolean warnoverwrite, Pattern filter) {
    super();
    this.out = out;
    this.gzip = gzip;
    this.warnoverwrite = warnoverwrite;
    this.filter = filter;
  }

  @Override
  public void processNewResult(Object result) {
    TextWriter writer = new TextWriter();

    try (StreamFactory output = openStreamFactory()) {
      writer.output(ResultUtil.findDatabase(result), result, output, filter);
    }
    catch(IOException e) {
      throw new IllegalStateException("Input/Output error while writing result.", e);
    }
  }

  private StreamFactory openStreamFactory() throws IOException {
    if(out == null) {
      return new SingleStreamOutput();
    }
    // If it does not exist, make a folder.
    final String ext = FileUtil.getFilenameExtension(out);
    if(!(Files.exists(out) || "gz".equals(ext) || "csv".equals(ext) || "ascii".equals(ext) || "txt".equals(ext))) {
      LOG.info("Creating output directory: " + out);
      Files.createDirectories(out);
    }
    if(Files.isDirectory(out)) {
      if(warnoverwrite && Files.list(out).findFirst().isPresent()) {
        LOG.warning("Output directory specified is not empty. Files will be overwritten and old files may be left over.");
      }
      return new MultipleFilesOutput(out, gzip);
    }
    else {
      if(warnoverwrite && Files.exists(out) && Files.size(out) > 0) {
        LOG.warning("Output file exists and will be overwritten!");
      }
      return new SingleStreamOutput(out, gzip);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Flag to control GZIP compression.
     */
    public static final OptionID GZIP_OUTPUT_ID = new OptionID("out.gzip", "Enable gzip compression of output files.");

    /**
     * Flag to suppress overwrite warning.
     */
    public static final OptionID OVERWRITE_OPTION_ID = new OptionID("out.silentoverwrite", "Silently overwrite output files.");

    /**
     * Pattern to filter the output
     */
    public static final OptionID FILTER_PATTERN_ID = new OptionID("out.filter", "Filter pattern for output selection. Only output streams that match the given pattern will be written.");

    /**
     * Holds the file to print results to.
     */
    private Path out = null;

    /**
     * Whether or not to do gzip compression on output.
     */
    private boolean gzip = false;

    /**
     * Whether or not to warn on overwrite
     */
    private boolean warnoverwrite = true;

    /**
     * Result filter pattern. Optional!
     */
    private Pattern filter = null;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(OutputStep.Par.OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE) //
          .setOptional(true) //
          .grab(config, x -> out = Paths.get(x));
      if(out != null || config instanceof UnParameterization /* for documentation */) {
        new Flag(GZIP_OUTPUT_ID).grab(config, x -> gzip = x);
        new Flag(OVERWRITE_OPTION_ID).grab(config, x -> warnoverwrite = !x);
      }
      new PatternParameter(FILTER_PATTERN_ID) //
          .setOptional(true) //
          .grab(config, x -> filter = x);
    }

    @Override
    public ResultWriter make() {
      return new ResultWriter(out, gzip, warnoverwrite, filter);
    }
  }
}
