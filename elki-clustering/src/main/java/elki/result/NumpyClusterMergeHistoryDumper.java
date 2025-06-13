/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.logging.Logging;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * TODO
 * 
 * @author Andreas Lang
 */
public class NumpyClusterMergeHistoryDumper extends NumpyDumper implements ResultHandler {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NumpyClusterMergeHistoryDumper.class);

  /**
   * Output file.
   */
  private Path outputFile;

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   * @param append Append to output file (overwrite otherwise).
   * @param forceLabel Forced label to use for the output, may be {@code null}.
   */
  public NumpyClusterMergeHistoryDumper(Path outputFile) {
    super();
    this.outputFile = outputFile;
  }

  @Override
  public void processNewResult(Object newResult) {
    List<ClusterMergeHistory> cs = ClusterMergeHistory.getMergeHistoryResults(newResult);
    if(cs.isEmpty()) {
      return;
    }
    if (cs.size() > 1){
      LOG.error("More than one Dendrogram to write, old one will be overwritten");
    }
    for(ClusterMergeHistory c : cs) {
try (FileChannel channel = FileChannel.open(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING)) {
        writeDendrogram(channel,c);
      }
      catch(IOException e) {
        throw new AbortException("IO error writing numpy file", e);
      }
    }
  }

  public void writeDendrogram(FileChannel file, ClusterMergeHistory clustering) throws IOException {
    int count = clustering.numMerges();
    double[][] data = new double[count][4];
    for(int i=0; i < count;i++){
      data[i][0] = clustering.getMergeA(i);
      data[i][1] = clustering.getMergeB(i);
      data[i][2] = clustering.getMergeHeight(i);
      data[i][3] = clustering.getSize(i);
    }
    ByteBuffer bbuf = ByteBuffer.allocateDirect(data.length * 4 * Double.BYTES);
    bbuf.order(ByteOrder.LITTLE_ENDIAN);
    DoubleBuffer buf = bbuf.asDoubleBuffer();
    for(int i=0; i<data.length;i++){
      for (int j=0; j<data[i].length;j++){
        buf.put(data[i][j]);
      }
    }
    Map<String,String> header= new HashMap<>();
    header.put("descr", "'<f8'");
    header.put("fortran_order", "False");
    header.put("shape", "(" + count +", " + 4 +")");
    writeNumpyArray(file, header, bbuf);
  }


  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static class Par implements Parameterizer {
    /**
     * Output file name parameter.
     */
    public static final OptionID OUT_ID = new OptionID("dendrogram.output", "Output file name. When not given, the result will be written to stdout.");

    /**
     * Output file.
     */
    private Path outputFile;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(OUT_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> outputFile = Paths.get(x));
    }

    @Override
    public NumpyClusterMergeHistoryDumper make() {
      return new NumpyClusterMergeHistoryDumper(outputFile);
    }
  }
}
