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
package elki.application.cache;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import elki.application.AbstractApplication;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.KNNList;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Precompute the k nearest neighbors in a disk cache.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @has - - - Distance
 * 
 * @param <O> Object type
 */
public class CacheDoubleDistanceKNNLists<O> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CacheDoubleDistanceKNNLists.class);

  /**
   * Data source to process.
   */
  private Database database;

  /**
   * Distance function that is to be cached.
   */
  private Distance<? super O> distance;

  /**
   * Number of neighbors to precompute.
   */
  private int k;

  /**
   * Output file.
   */
  private Path out;

  /**
   * Magic number to identify files.
   * <p>
   * Note, when cloning this class, and performing any incompatible change to
   * the file format, you should also change this magic ID!
   */
  public static final int KNN_CACHE_MAGIC = 0xCAC43D1C;

  /**
   * Constructor.
   * 
   * @param database Data source
   * @param distance Distance function
   * @param k Number of nearest neighbors
   * @param out Matrix output file
   */
  public CacheDoubleDistanceKNNLists(Database database, Distance<? super O> distance, int k, Path out) {
    super();
    this.database = database;
    this.distance = distance;
    this.k = k;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    KNNSearcher<DBIDRef> knnQ = new QueryBuilder<>(relation, distance).noCache().kNNByDBID(k);

    // open file.
    try (FileChannel channel = FileChannel.open(out, //
        StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
        // and acquire a file write lock
        FileLock lock = channel.lock()) {

      int bufsize = k * 12 * 2 + 10; // Initial size, enough for 2 kNN.
      ByteBuffer buffer = ByteBuffer.allocateDirect(bufsize);

      // write magic header
      buffer.putInt(KNN_CACHE_MAGIC).flip();
      channel.write(buffer);

      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing kNN", relation.size(), LOG) : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final KNNList nn = knnQ.getKNN(it, k);
        final int nnsize = nn.size();

        // Grow the buffer when needed:
        if(nnsize * 12 + 10 > bufsize) {
          while(nnsize * 12 + 10 > bufsize) {
            bufsize <<= 1;
          }
          buffer = ByteBuffer.allocateDirect(bufsize);
        }

        buffer.clear();
        ByteArrayUtil.writeUnsignedVarint(buffer, it.internalGetIndex());
        ByteArrayUtil.writeUnsignedVarint(buffer, nnsize);
        int c = 0;
        for(DoubleDBIDListIter ni = nn.iter(); ni.valid(); ni.advance(), c++) {
          ByteArrayUtil.writeUnsignedVarint(buffer, ni.internalGetIndex());
          buffer.putDouble(ni.doubleValue());
        }
        if(c != nn.size()) {
          throw new AbortException("Sizes did not agree. Cache is invalid.");
        }

        buffer.flip();
        channel.write(buffer);
        LOG.incrementProcessed(prog);
      }
      LOG.ensureCompleted(prog);
      lock.release();
    }
    catch(IOException e) {
      LOG.exception(e);
    }
    // FIXME: close!
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractApplication.Par {
    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID CACHE_ID = new OptionID("loader.diskcache", "File name of the disk cache to create.");

    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID DISTANCE_ID = new OptionID("loader.distance", "Distance function to cache.");

    /**
     * Parameter that specifies the number of neighbors to precompute.
     */
    public static final OptionID K_ID = new OptionID("loader.k", "Number of nearest neighbors to precompute.");

    /**
     * Data source to process.
     */
    private Database database = null;

    /**
     * Distance function that is to be cached.
     */
    private Distance<? super O> distance = null;

    /**
     * Number of neighbors to precompute.
     */
    private int k;

    /**
     * Output file.
     */
    private Path out = null;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<Database>(DATABASE_ID, Database.class, StaticArrayDatabase.class) //
          .grab(config, x -> database = x);
      // Distance function parameter
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      // Output file parameter
      new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> out = Paths.get(x));
    }

    @Override
    public CacheDoubleDistanceKNNLists<O> make() {
      return new CacheDoubleDistanceKNNLists<>(database, distance, k, out);
    }
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(CacheDoubleDistanceKNNLists.class, args);
  }
}
