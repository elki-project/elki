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
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import elki.clustering.hierarchical.ClusterMergeHistory;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.Model;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDs;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.utilities.datastructures.iterator.It;
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
public class NumpyClusteringVectorDumper extends NumpyDumper implements ResultHandler {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(NumpyClusteringVectorDumper.class);

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
  public NumpyClusteringVectorDumper(Path outputFile) {
    super();
    this.outputFile = outputFile;
  }

  @Override
  public void processNewResult(Object newResult) {
    List<Clustering<? extends Model>> cs = Clustering.getClusteringResults(newResult);
    if(cs.isEmpty()) {
      return;
    }
    if (cs.size() > 1){
      LOG.error("More than one Dendrogram to write, old one will be overwritten");
    }
    for(Clustering<? extends Model> c : cs) {
try (FileChannel channel = FileChannel.open(outputFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.TRUNCATE_EXISTING)) {
        writeClustering(channel,c);
      }
      catch(IOException e) {
        throw new AbortException("IO error writing numpy file", e);
      }
    }
  }

  public void writeClustering(FileChannel file, Clustering<? extends Model> c) throws IOException {
        DBIDRange ids = null;
    for(It<Relation<?>> iter = Metadata.hierarchyOf(c).iterParents().filter(Relation.class); iter.valid(); iter.advance()) {
      DBIDs pids = iter.get().getDBIDs();
      if(pids instanceof DBIDRange) {
        ids = (DBIDRange) pids;
        break;
      }
      LOG.warning("Parent result " + Metadata.of(iter.get()).getLongName() + " has DBID type " + pids.getClass());
    }
    // Fallback: try to locate a database.
    if(ids == null) {
      for(It<Database> iter = Metadata.hierarchyOf(c).iterAncestors().filter(Database.class); iter.valid(); iter.advance()) {
        DBIDs pids = iter.get().getRelation(TypeUtil.ANY).getDBIDs();
        if(pids instanceof DBIDRange) {
          ids = (DBIDRange) pids;
          break;
        }
        LOG.warning("Parent result " + Metadata.of(iter.get()).getLongName() + " has DBID type " + pids.getClass());
      }
    }
    if(ids == null) {
      LOG.warning("Cannot dump cluster assignment, as I do not have a well-defined DBIDRange to use for a unique column assignment. DBIDs must be a continuous range.");
      return;
    }

    WritableIntegerDataStore map = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP);
    int cnum = 0;
    for(Cluster<?> clu : c.getAllClusters()) {
      for(DBIDIter iter = clu.getIDs().iter(); iter.valid(); iter.advance()) {
        map.putInt(iter, cnum);
      }
      ++cnum;
    }
    int count = ids.size();

    ByteBuffer bbuf = ByteBuffer.allocateDirect(count * Integer.BYTES);
    bbuf.order(ByteOrder.LITTLE_ENDIAN);
    IntBuffer buf = bbuf.asIntBuffer();
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      buf.put(map.intValue(iter));
    }
    Map<String,String> header= new HashMap<>();
    header.put("descr", "'<i4'");
    header.put("fortran_order", "False");
    header.put("shape", "(" + count + ", )");
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
    public NumpyClusteringVectorDumper make() {
      return new NumpyClusteringVectorDumper(outputFile);
    }
  }
}
