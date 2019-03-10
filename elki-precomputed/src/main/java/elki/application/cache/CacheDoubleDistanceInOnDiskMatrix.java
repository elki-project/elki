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

import elki.application.AbstractApplication;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.external.DiskCacheBasedDoubleDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.persistent.OnDiskUpperTriangleMatrix;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Precompute an on-disk distance matrix, using double precision.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - OnDiskUpperTriangleMatrix
 * @has - - - Distance
 * 
 * @param <O> Object type
 */
public class CacheDoubleDistanceInOnDiskMatrix<O> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CacheDoubleDistanceInOnDiskMatrix.class);

  /**
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckSymmetry = false;

  /**
   * Data source to process.
   */
  private Database database;

  /**
   * Distance function that is to be cached.
   */
  private Distance<? super O> distance;

  /**
   * Output file.
   */
  private File out;

  /**
   * Constructor.
   * 
   * @param database Data source
   * @param distance Distance function
   * @param out Matrix output file
   */
  public CacheDoubleDistanceInOnDiskMatrix(Database database, Distance<? super O> distance, File out) {
    super();
    this.database = database;
    this.distance = distance;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, distance);

    DBIDRange ids = DBIDUtil.assertRange(relation.getDBIDs());
    int size = ids.size();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing distances", (int) (((size + 1) * (long) size) >>> 1), LOG) : null;
    try (OnDiskUpperTriangleMatrix matrix = //
        new OnDiskUpperTriangleMatrix(out, DiskCacheBasedDoubleDistance.DOUBLE_CACHE_MAGIC, 0, ByteArrayUtil.SIZE_DOUBLE, size)) {

      DBIDArrayIter id1 = ids.iter(), id2 = ids.iter();
      for(; id1.valid(); id1.advance()) {
        for(id2.seek(id1.getOffset()); id2.valid(); id2.advance()) {
          double d = distanceQuery.distance(id1, id2);
          if(debugExtraCheckSymmetry) {
            double d2 = distanceQuery.distance(id2, id1);
            if(Math.abs(d - d2) > 0.0000001) {
              LOG.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          try {
            matrix.getRecordBuffer(id1.getOffset(), id2.getOffset()).putDouble(d);
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + DBIDUtil.toString(id1) + "," + DBIDUtil.toString(id2) + " to matrix.", e);
          }
        }
        if(prog != null) {
          prog.setProcessed(prog.getProcessed() + (size - id1.getOffset()), LOG);
        }
      }
    }
    catch(IOException e) {
      throw new AbortException("Error precomputing distance matrix.", e);
    }
    LOG.ensureCompleted(prog);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<O> extends AbstractApplication.Parameterizer {
    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID CACHE_ID = new OptionID("loader.diskcache", "File name of the disk cache to create.");

    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID DISTANCE_ID = new OptionID("loader.distance", "Distance function to cache.");

    /**
     * Data source to process.
     */
    private Database database = null;

    /**
     * Distance function that is to be cached.
     */
    private Distance<? super O> distance = null;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final ObjectParameter<Database> dbP = new ObjectParameter<>(DATABASE_ID, Database.class, StaticArrayDatabase.class);
      if(config.grab(dbP)) {
        database = dbP.instantiateClass(config);
      }
      // Distance function parameter
      final ObjectParameter<Distance<? super O>> dpar = new ObjectParameter<>(DISTANCE_ID, Distance.class);
      if(config.grab(dpar)) {
        distance = dpar.instantiateClass(config);
      }
      // Output file parameter
      final FileParameter cpar = new FileParameter(CACHE_ID, FileParameter.FileType.OUTPUT_FILE);
      if(config.grab(cpar)) {
        out = cpar.getValue();
      }
    }

    @Override
    protected CacheDoubleDistanceInOnDiskMatrix<O> makeInstance() {
      return new CacheDoubleDistanceInOnDiskMatrix<>(database, distance, out);
    }
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(CacheDoubleDistanceInOnDiskMatrix.class, args);
  }
}
