package elki.result;

/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.logging.Logging;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Dumps the cluster merge history to a file or stdout, useful for debugging and
 * for further analysis in other tools.
 */
public class ClusterMergeHistoryDumper implements ResultHandler {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClusterMergeHistoryDumper.class);

  /**
   * Output file.
   */
  private Path outputFile;

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   */
  public ClusterMergeHistoryDumper(Path outputFile) {
    super();
    this.outputFile = outputFile;
  }

  @Override
  public void processNewResult(Object newResult) {
    List<ClusterMergeHistory> cs = Metadata.hierarchyOf(newResult).iterDescendantsSelf()//
        .<ClusterMergeHistory> filter(ClusterMergeHistory.class)//
        .collect(new ArrayList<ClusterMergeHistory>());
    if(cs.isEmpty()) {
      return;
    }
    if(cs.size() > 1) {
      LOG.warning("Found more than one cluster merge history result, they will be merged into the same output.");
    }

    // Open output stream - or use stdout.
    if(outputFile != null) {
      OpenOption opt = StandardOpenOption.TRUNCATE_EXISTING;
      try (BufferedWriter writer = Files.newBufferedWriter(outputFile, opt)) {
        // TODO: dump settings, too, for documentation?
        for(ClusterMergeHistory c : cs) {
          dumpClusterMergeHistory(writer, c);
        }
      }
      catch(IOException e) {
        LOG.exception("Error writing to output stream.", e);
      }
    }
    else {
      for(ClusterMergeHistory c : cs) {
        try {
          dumpClusterMergeHistory(System.out, c);
        }
        catch(IOException e) {
          LOG.exception("Error writing to output stream.", e);
        }
      }
    }
  }

  /**
   * Dump a cluster merge history.
   *
   * @param writer Output writer
   * @param history Cluster merge history
   * @throws IOException on IO error
   */
  protected void dumpClusterMergeHistory(Appendable writer, ClusterMergeHistory history) throws IOException {
    writer.append("# ClusterMergeHistory: size=").append(Integer.toString(history.size())).append(" merges=").append(Integer.toString(history.numMerges())).append(" squared=").append(Boolean.toString(history.isSquared())).append('\n');
    writer.append("# merge\tleft\tright\theight\tsize\n");
    for(int i = 0, n = history.numMerges(); i < n; i++) {
      writer.append(Integer.toString(i)).append('\t')//
          .append(Integer.toString(history.getMergeA(i))).append('\t') //
          .append(Integer.toString(history.getMergeB(i))).append('\t') //
          .append(Double.toString(history.getMergeHeight(i))).append('\t') //
          .append(Integer.toString(history.getSize(i))).append('\n');
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Output file name parameter.
     */
    public static final OptionID OUT_ID = new OptionID("clustering.output", "Output file name. When not given, the result will be written to stdout.");

    /**
     * Append flag.
     */
    public static final OptionID APPEND_ID = new OptionID("clustering.output.append", "Always append to the output file.");

    /**
     * Force label parameter.
     */
    public static final OptionID FORCE_LABEL_ID = new OptionID("clustering.label", "Parameter to override the clustering label, mostly to give a more descriptive label.");

    /**
     * Output file.
     */
    private Path outputFile;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(OUT_ID, FileParameter.FileType.OUTPUT_FILE) //
          .setOptional(true) //
          .grab(config, x -> outputFile = Paths.get(x));
    }

    @Override
    public ClusterMergeHistoryDumper make() {
      return new ClusterMergeHistoryDumper(outputFile);
    }
  }
}
