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
package elki.result;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import elki.data.*;
import elki.data.model.Model;
import elki.database.ids.*;
import elki.evaluation.classification.ConfusionMatrixEvaluationResult;
import elki.logging.Logging;
import elki.math.geometry.XYCurve;
import elki.result.textwriter.*;
import elki.result.textwriter.writers.*;
import elki.utilities.HandlerList;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.StringParameter;
import elki.utilities.pairs.DoubleDoublePair;
import elki.utilities.pairs.Pair;
import elki.workflow.OutputStep;

/**
 * TODO
 * 
 * @author Andreas Lang
 */
public class ClusteringSummaryDumper implements ResultHandler {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClusteringSummaryDumper.class);

  /**
   * Output path.
   */
  private Path outputPath;

  /**
   * Optional label to force for this output.
   */
  private String forceLabel;

  /**
   * Whether or not to do gzip compression on output.
   */
  private boolean gzip = false;

  /**
   * Fallback writer for unknown objects.
   */
  private TextWriterWriterInterface<?> fallback = new TextWriterObjectComment();

  /**
   * Hash map for supported classes in writer.
   */
  public static final HandlerList<TextWriterWriterInterface<?>> writers = new HandlerList<>();

  /**
   * Add some default handlers
   */
  static {
    TextWriterObjectInline trivialwriter = new TextWriterObjectInline();
    writers.insertHandler(Pair.class, new TextWriterPair());
    writers.insertHandler(DoubleDoublePair.class, new TextWriterDoubleDoublePair());
    writers.insertHandler(FeatureVector.class, trivialwriter);
    writers.insertHandler(double[].class, new TextWriterDoubleArray());
    writers.insertHandler(int[].class, new TextWriterIntArray());
    // these object can be serialized inline with toString()
    writers.insertHandler(String.class, trivialwriter);
    writers.insertHandler(Double.class, trivialwriter);
    writers.insertHandler(Integer.class, trivialwriter);
    writers.insertHandler(String[].class, new TextWriterObjectArray<String>());
    writers.insertHandler(Double[].class, new TextWriterObjectArray<Double>());
    writers.insertHandler(Integer[].class, new TextWriterObjectArray<Integer>());
    writers.insertHandler(SimpleClassLabel.class, trivialwriter);
    writers.insertHandler(HierarchicalClassLabel.class, trivialwriter);
    writers.insertHandler(LabelList.class, trivialwriter);
    writers.insertHandler(DBID.class, trivialwriter);
    writers.insertHandler(XYCurve.class, new TextWriterXYCurve());
    // Objects that have an own writeToText method.
    writers.insertHandler(TextWriteable.class, new TextWriterTextWriteable());
    writers.insertHandler(ConfusionMatrixEvaluationResult.class, new TextWriterConfusionMatrixResult());
  }

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   * @param append Append to output file (overwrite otherwise).
   * @param forceLabel Forced label to use for the output, may be {@code null}.
   */
  public ClusteringSummaryDumper(Path outputPath, boolean gzip, String forceLabel) {
    super();
    this.outputPath = outputPath;
    this.forceLabel = forceLabel;
    this.gzip = gzip;
  }

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   * @param append Append to output file (overwrite otherwise).
   */
  public ClusteringSummaryDumper(Path outputFile, boolean append) {
    this(outputFile, append, null);
  }

  @Override
  public void processNewResult(Object newResult) {
    List<Clustering<? extends Model>> cs = Clustering.getClusteringResults(newResult);

    if(cs.isEmpty()) {
      return;
    }
    if(forceLabel != null && forceLabel.length() > 0 && cs.size() > 1) {
      LOG.warning("Found more than one clustering result, they will have the same (forced) label.");
    }

    // Open output stream - or use stdout.
    if(outputPath != null) {
      StreamFactory sf = new MultipleFilesOutput(outputPath, gzip);
      int i = 0;
      for(Clustering<?> cing : cs) {
        for(Cluster<?> c : cing.getAllClusters()) {
          try (PrintStream outStream = sf.openStream("Cluster_" + i++)) {
            TextWriterStream out = new TextWriterStream(outStream, writers, fallback);
            // TODO: dump settings, too?
            c.writeToText(out, null);
            out.flush();
            sf.closeStream(outStream);
          }
          catch(IOException e) {
            LOG.exception("Error writing to output stream.", e);
          }
        }
      }
      try {
        sf.close();
      }
      catch(IOException e) {
        LOG.exception("Error writing to output stream.", e);
      }
    }
    else { // TODO implement
      try (StreamFactory sf = new SingleStreamOutput();) {
        PrintStream outStream = sf.openStream("Clusters");
        TextWriterStream out = new TextWriterStream(outStream, writers, fallback);
        for(Clustering<?> cing : cs) {
          for(Cluster<?> c : cing.getAllClusters()) {
            c.writeToText(out, null);
            out.flush();
          }
        }
        sf.closeStream(outStream);
      }
      catch(IOException e) {
        LOG.exception("Error writing to output stream.", e);
      }
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
     * Force label parameter.
     */
    public static final OptionID FORCE_LABEL_ID = new OptionID("clustering.label", "Parameter to override the clustering label, mostly to give a more descriptive label.");

    /**
     * Output file.
     */
    private Path outputFile = null;

    /**
     * Optional label to force for this output.
     */
    private String forceLabel;

    /**
     * Whether or not to do gzip compression on output.
     */
    private boolean gzip = false;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(OutputStep.Par.OUTPUT_ID, FileParameter.FileType.OUTPUT_FILE) //
          .setOptional(true) //
          .grab(config, x -> outputFile = Paths.get(x));
      new Flag(GZIP_OUTPUT_ID).grab(config, x -> gzip = x);
      new StringParameter(FORCE_LABEL_ID) //
          .setOptional(true) //
          .grab(config, x -> forceLabel = x);
    }

    @Override
    public ClusteringSummaryDumper make() {
      return new ClusteringSummaryDumper(outputFile, gzip, forceLabel);
    }
  }
}
