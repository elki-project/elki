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
import elki.data.type.TypeUtil;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDRange;
import elki.database.relation.Relation;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Output a cluster merge history in a scipy compatible format into a Numpy file.
 * 
 * @author Andreas Lang
 */
public class NumpyClusterMergeHistoryDumper extends NumpyDumper implements ResultHandler {

  /**
   * Output file.
   */
  private Path outputFile;

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   */
  public NumpyClusterMergeHistoryDumper(Path outputFile) {
    super();
    this.outputFile = outputFile;
  }

  @Override
  public void processNewResult(Object newResult) {
    List<ClusterMergeHistory> cmhs = ClusterMergeHistory.getMergeHistoryResults(newResult);
    if(cmhs.isEmpty()) {
      return;
    }
    int resultNo = 0;
    for(ClusterMergeHistory cmh : cmhs) {
      String fileName = outputFile.getFileName().toString();
      int lastDotIndex = fileName.lastIndexOf('.');
      String nameWithoutExtension;
      if(lastDotIndex == -1) {
        nameWithoutExtension = fileName;
      }
      else {
        nameWithoutExtension = fileName.substring(0, lastDotIndex);
      }
      Path result_path;
      if(resultNo > 0) {
        result_path = outputFile.getParent().resolve(nameWithoutExtension + resultNo + "." + extension);
      }
      else {
        result_path = outputFile.getParent().resolve(nameWithoutExtension + "." + extension);
      }

      try (FileChannel channel = FileChannel.open(result_path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING)) {
        writeMergeHistory(channel,cmh, newResult);
        resultNo++;
      }
      catch(IOException e) {
        throw new AbortException("IO error writing numpy file", e);
      }
    }
  }

/**
 * Writes the merge history information from a ClusterMergeHistory object to a specified file.
 *
 * @param file The FileChannel where the dendrogram will be written.
 * @param cmh The ClusterMergeHistory object containing the dendrogram data to write.
 * @throws IOException If an I/O error occurs while writing to the file.
 */
  public void writeMergeHistory(FileChannel file, ClusterMergeHistory cmh, Object database) throws IOException {
    int count = cmh.numMerges();
    double[][] data = new double[count][4];
    if (cmh.getDBIDs() instanceof DBIDRange){
      for(int i = 0; i < count; i++) {
        data[i][0] = cmh.getMergeA(i);
        data[i][1] = cmh.getMergeB(i);
        data[i][2] = cmh.getMergeHeight(i);
        data[i][3] = cmh.getSize(i);
      }
    } else {
      System.err.println("DBID type is not DBIDRange");
      StaticArrayDatabase db = (StaticArrayDatabase) database;
      DBIDRange ids = (DBIDRange) db.getRelation(TypeUtil.ANY).getDBIDs();
      DBIDArrayIter aiter = cmh.getDBIDs().iter();

      for(int i = 0; i < count; i++) {
        final int mCountA = cmh.getMergeA(i);
        data[i][0] = mCountA <= count ? ids.index(aiter.seek(mCountA)): mCountA;
        final int mCountB = cmh.getMergeB(i);
        data[i][1] = mCountB <= count ? ids.index(aiter.seek(mCountB)): mCountB;
        data[i][2] = cmh.getMergeHeight(i);
        data[i][3] = cmh.getSize(i);
      }
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
    public static final OptionID OUT_ID = new OptionID("clusterhistory.output", "Output file name.");

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
