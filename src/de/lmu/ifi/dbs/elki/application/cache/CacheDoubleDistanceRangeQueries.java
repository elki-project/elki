package de.lmu.ifi.dbs.elki.application.cache;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.persistent.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Precompute the k nearest neighbors in a disk cache.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DistanceFunction
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
  private InputStep input;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O, DoubleDistance> distance;

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
   * @param input Data source
   * @param distance Distance function
   * @param radius Query radius
   * @param out Matrix output file
   */
  public CacheDoubleDistanceRangeQueries(InputStep input, DistanceFunction<O, DoubleDistance> distance, double radius, File out) {
    super();
    this.input = input;
    this.distance = distance;
    this.radius = radius;
    this.out = out;
  }

  @Override
  public void run() {
    Database database = input.getDatabase();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O, DoubleDistance> distanceQuery = database.getDistanceQuery(relation, distance);
    DoubleDistance rad = new DoubleDistance(radius);
    RangeQuery<O, DoubleDistance> rangeQ = database.getRangeQuery(distanceQuery, rad, DatabaseQuery.HINT_HEAVY_USE);

    LOG.verbose("Performing range queries with radius " + rad);

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

      for (DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        final DistanceDBIDList<DoubleDistance> nn = rangeQ.getRangeForDBID(it, rad);
        final int nnsize = nn.size();

        // Grow the buffer when needed:
        if (nnsize * 12 + 10 > bufsize) {
          while (nnsize * 12 + 10 > bufsize) {
            bufsize <<= 1;
          }
          LOG.verbose("Resizing buffer to "+bufsize+" to store "+nnsize+" results:");
          buffer = ByteBuffer.allocateDirect(bufsize);
        }

        buffer.clear();
        ByteArrayUtil.writeUnsignedVarint(buffer, it.internalGetIndex());
        ByteArrayUtil.writeUnsignedVarint(buffer, nnsize);
        int c = 0;
        if (nn instanceof DoubleDistanceDBIDList) {
          for (DoubleDistanceDBIDListIter ni = ((DoubleDistanceDBIDList) nn).iter(); ni.valid(); ni.advance(), c++) {
            ByteArrayUtil.writeUnsignedVarint(buffer, ni.internalGetIndex());
            buffer.putDouble(ni.doubleDistance());
          }
        } else {
          for (DistanceDBIDListIter<DoubleDistance> ni = nn.iter(); ni.valid(); ni.advance(), c++) {
            ByteArrayUtil.writeUnsignedVarint(buffer, ni.internalGetIndex());
            buffer.putDouble(ni.getDistance().doubleValue());
          }
        }
        if (c != nn.size()) {
          throw new AbortException("Sizes did not agree. Cache is invalid.");
        }

        buffer.flip();
        channel.write(buffer);
        if (prog != null) {
          prog.incrementProcessed(LOG);
        }
      }
      if (prog != null) {
        prog.ensureCompleted(LOG);
      }
      lock.release();
    } catch (IOException e) {
      LOG.exception(e);
    }
    // FIXME: close!
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractApplication.Parameterizer {
    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     * <p>
     * Key: {@code -loader.diskcache}
     * </p>
     */
    public static final OptionID CACHE_ID = new OptionID("loader.diskcache", "File name of the disk cache to create.");

    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     * <p>
     * Key: {@code -loader.distance}
     * </p>
     */
    public static final OptionID DISTANCE_ID = new OptionID("loader.distance", "Distance function to cache.");

    /**
     * Parameter that specifies the query radius to precompute.
     * <p>
     * Key: {@code -loader.radius}
     * </p>
     */
    public static final OptionID RADIUS_ID = new OptionID("loader.radius", "Query radius for precomputation.");

    /**
     * Data source to process.
     */
    private InputStep input = null;

    /**
     * Distance function that is to be cached.
     */
    private DistanceFunction<O, DoubleDistance> distance = null;

    /**
     * Number of neighbors to precompute.
     */
    private double radius;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      input = config.tryInstantiate(InputStep.class);
      // Distance function parameter
      final ObjectParameter<DistanceFunction<O, DoubleDistance>> dpar = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class);
      if (config.grab(dpar)) {
        distance = dpar.instantiateClass(config);
      }
      final DoubleParameter kpar = new DoubleParameter(RADIUS_ID);
      kpar.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if (config.grab(kpar)) {
        radius = kpar.doubleValue();
      }
      // Output file parameter
      final FileParameter cpar = new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE);
      if (config.grab(cpar)) {
        out = cpar.getValue();
      }
    }

    @Override
    protected CacheDoubleDistanceRangeQueries<O> makeInstance() {
      return new CacheDoubleDistanceRangeQueries<>(input, distance, radius, out);
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
