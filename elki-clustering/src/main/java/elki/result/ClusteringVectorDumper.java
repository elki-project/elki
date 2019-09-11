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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import elki.data.Cluster;
import elki.data.Clustering;
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
import elki.datasource.parser.ClusteringVectorParser;
import elki.logging.Logging;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.Flag;
import elki.utilities.optionhandling.parameters.StringParameter;

/**
 * Output a clustering result in a simple and compact ascii format:
 * whitespace separated cluster indexes
 * <p>
 * This format can be read using {@link ClusteringVectorParser} for meta
 * analysis, or read as clustering using
 * {@link elki.clustering.meta.ExternalClustering}.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ClusteringVectorDumper implements ResultHandler {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ClusteringVectorDumper.class);

  /**
   * Output file.
   */
  private File outputFile;

  /**
   * Optional label to force for this output.
   */
  private String forceLabel;

  /**
   * Always append to the output file.
   */
  private boolean append;

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   * @param append Append to output file (overwrite otherwise).
   * @param forceLabel Forced label to use for the output, may be {@code null}.
   */
  public ClusteringVectorDumper(File outputFile, boolean append, String forceLabel) {
    super();
    this.outputFile = outputFile;
    this.forceLabel = forceLabel;
    this.append = append;
  }

  /**
   * Constructor.
   * 
   * @param outputFile Output file
   * @param append Append to output file (overwrite otherwise).
   */
  public ClusteringVectorDumper(File outputFile, boolean append) {
    this(outputFile, append, null);
  }

  @Override
  public void processNewResult(Object newResult) {
    List<Clustering<?>> cs = Clustering.getClusteringResults(newResult);
    if(cs.isEmpty()) {
      return;
    }
    if(forceLabel != null && forceLabel.length() > 0 && cs.size() > 1) {
      LOG.warning("Found more than one clustering result, they will have the same (forced) label.");
    }

    // Open output stream - or use stdout.
    if(outputFile != null) {
      try (FileOutputStream os = new FileOutputStream(outputFile, append); //
          PrintStream writer = new PrintStream(os)) {
        // TODO: dump settings, too?
        for(Clustering<?> c : cs) {
          dumpClusteringOutput(writer, c);
        }
        append = true; // Append future results.
      }
      catch(IOException e) {
        LOG.exception("Error writing to output stream.", e);
      }
    }
    else {
      for(Clustering<?> c : cs) {
        dumpClusteringOutput(System.out, c);
      }
    }
  }

  /**
   * Dump a single clustering result.
   * 
   * @param writer Output writer
   * @param c Clustering result
   */
  protected void dumpClusteringOutput(PrintStream writer, Clustering<?> c) {
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
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      if(iter.getOffset() > 0) {
        writer.append(' ');
      }
      writer.append(Integer.toString(map.intValue(iter)));
    }
    if(forceLabel != null) {
      if(forceLabel.length() > 0) {
        writer.append(' ').append(forceLabel);
      }
    }
    else {
      writer.append(' ').append(Metadata.of(c).getLongName());
    }
    writer.append('\n');
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
    private File outputFile = null;

    /**
     * Optional label to force for this output.
     */
    private String forceLabel;

    /**
     * Always append to the output file.
     */
    private boolean append;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(OUT_ID, FileParameter.FileType.OUTPUT_FILE) //
          .setOptional(true) //
          .grab(config, x -> outputFile = x);
      new Flag(APPEND_ID).grab(config, x -> append = x);
      new StringParameter(FORCE_LABEL_ID) //
          .setOptional(true) //
          .grab(config, x -> forceLabel = x);
    }

    @Override
    public ClusteringVectorDumper make() {
      return new ClusteringVectorDumper(outputFile, append, forceLabel);
    }
  }
}
