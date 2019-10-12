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
package elki.application.cache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import elki.application.AbstractApplication;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDListIter;
import elki.database.ids.ModifiableDoubleDBIDList;
import elki.database.query.QueryBuilder;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.FileParameter;
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
public class CacheDoubleDistanceRangeQueries<O> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CacheDoubleDistanceRangeQueries.class);

  /**
   * Data source to process.
   */
  private Database database;

  /**
   * Distance function that is to be cached.
   */
  private Distance<? super O> distance;

  /**
   * Query radius.
   */
  private double radius;

  /**
   * Output file.
   */
  private File out;

  /**
   * Magic number to identify files.
   *
   * Note, when cloning this class, and performing any incompatible change to
   * the file format, you should also change this magic ID!
   */
  public static final int RANGE_CACHE_MAGIC = 0xCAC43333;

  /**
   * Constructor.
   *
   * @param database Data source
   * @param distance Distance function
   * @param radius Query radius
   * @param out Matrix output file
   */
  public CacheDoubleDistanceRangeQueries(Database database, Distance<? super O> distance, double radius, File out) {
    super();
    this.database = database;
    this.distance = distance;
    this.radius = radius;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    RangeQuery<O> rangeQ = new QueryBuilder<>(relation, distance).rangeQuery();

    LOG.verbose("Performing range queries with radius " + radius);

    // open file.
    try (RandomAccessFile file = new RandomAccessFile(out, "rw");
        FileChannel channel = file.getChannel();
        // and acquire a file write lock
        FileLock lock = channel.lock()) {
      // write magic header
      file.writeInt(RANGE_CACHE_MAGIC);
      // write the query radius.
      file.writeDouble(radius);

      int bufsize = 100 * 12 * 2 + 10; // Initial size, enough for 100.
      ByteBuffer buffer = ByteBuffer.allocateDirect(bufsize);

      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Computing range queries", relation.size(), LOG) : null;

      ModifiableDoubleDBIDList nn = DBIDUtil.newDistanceDBIDList();
      DoubleDBIDListIter ni = nn.iter();
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        rangeQ.getRangeForDBID(it, radius, nn.clear()).sort();
        final int nnsize = nn.size();

        // Grow the buffer when needed:
        if(nnsize * 12 + 10 > bufsize) {
          while(nnsize * 12 + 10 > bufsize) {
            bufsize <<= 1;
          }
          LOG.verbose("Resizing buffer to " + bufsize + " to store " + nnsize + " results:");
          buffer = ByteBuffer.allocateDirect(bufsize);
        }

        buffer.clear();
        ByteArrayUtil.writeUnsignedVarint(buffer, it.internalGetIndex());
        ByteArrayUtil.writeUnsignedVarint(buffer, nnsize);
        int c = 0;
        for(ni.seek(0); ni.valid(); ni.advance(), c++) {
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
     * Parameter that specifies the query radius to precompute.
     */
    public static final OptionID RADIUS_ID = new OptionID("loader.radius", "Query radius for precomputation.");

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
    private double radius;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<Database>(DATABASE_ID, Database.class, StaticArrayDatabase.class) //
          .grab(config, x -> database = x);
      // Distance function parameter
      new ObjectParameter<Distance<? super O>>(DISTANCE_ID, Distance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(RADIUS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .grab(config, x -> radius = x);
      // Output file parameter
      new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> out = x);
    }

    @Override
    public CacheDoubleDistanceRangeQueries<O> make() {
      return new CacheDoubleDistanceRangeQueries<>(database, distance, radius, out);
    }
  }

  /**
   * Main method, delegate to super class.
   *
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(CacheDoubleDistanceRangeQueries.class, args);
  }
}
