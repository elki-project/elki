/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.result;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.textwriter.MultipleFilesOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.SingleStreamOutput;
import de.lmu.ifi.dbs.elki.result.textwriter.StreamFactory;
import de.lmu.ifi.dbs.elki.result.textwriter.TextWriter;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.workflow.OutputStep;

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
  public ResultWriter(File out, boolean gzip, boolean warnoverwrite, Pattern filter) {
    super();
    this.out = out;
    this.gzip = gzip;
    this.warnoverwrite = warnoverwrite;
    this.filter = filter;
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result result) {
    TextWriter writer = new TextWriter();

    try (StreamFactory output = openStreamFactory()) {
      writer.output(ResultUtil.findDatabase(hier), result, output, filter);
    }
    catch(IOException e) {
      throw new IllegalStateException("Input/Output error while writing result.", e);
    }
  }

  private StreamFactory openStreamFactory() throws IOException {
    if(out == null) {
      return new SingleStreamOutput(gzip);
    }
    // If it does not exist, make a folder.
    final String ext = FileUtil.getFilenameExtension(out);
    if(!(out.exists() || "gz".equals(ext) || "csv".equals(ext) || "ascii".equals(ext) || "txt".equals(ext))) {
      LOG.info("Creating output directory: " + out);
      out.mkdirs();
    }
    if(out.isDirectory()) {
      if(warnoverwrite && out.listFiles().length > 0) {
        LOG.warning("Output directory specified is not empty. Files will be overwritten and old files may be left over.");
      }
      return new MultipleFilesOutput(out, gzip);
    }
    else {
      if(warnoverwrite && out.exists() && out.length() > 0) {
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
  public static class Parameterizer extends AbstractParameterizer {
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
    private File out = null;

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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      FileParameter outputP = new FileParameter(OutputStep.Parameterizer.OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE, true);
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

      PatternParameter filterP = new PatternParameter(FILTER_PATTERN_ID) //
          .setOptional(true);
      if(config.grab(filterP)) {
        filter = filterP.getValue();
      }
    }

    @Override
    protected ResultWriter makeInstance() {
      return new ResultWriter(out, gzip, warnoverwrite, filter);
    }
  }
}
