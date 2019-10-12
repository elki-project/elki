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
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.external.DiskCacheBasedFloatDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.persistent.OnDiskUpperTriangleMatrix;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Precompute an on-disk distance matrix, using float precision.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @has - - - OnDiskUpperTriangleMatrix
 * @has - - - Distance
 * 
 * @param <O> Object type
 */
public class CacheFloatDistanceInOnDiskMatrix<O> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CacheFloatDistanceInOnDiskMatrix.class);

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
  public CacheFloatDistanceInOnDiskMatrix(Database database, Distance<? super O> distance, File out) {
    super();
    this.database = database;
    this.distance = distance;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> distanceQuery = new QueryBuilder<>(relation, distance).distanceQuery();

    DBIDRange ids = DBIDUtil.assertRange(relation.getDBIDs());
    int size = ids.size();

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Precomputing distances", (int) (((size + 1) * (long) size) >>> 1), LOG) : null;
    try (OnDiskUpperTriangleMatrix matrix = //
        new OnDiskUpperTriangleMatrix(out, DiskCacheBasedFloatDistance.FLOAT_CACHE_MAGIC, 0, ByteArrayUtil.SIZE_FLOAT, size)) {
      DBIDArrayIter id1 = ids.iter(), id2 = ids.iter();
      for(; id1.valid(); id1.advance()) {
        for(id2.seek(id1.getOffset()); id2.valid(); id2.advance()) {
          float d = (float) distanceQuery.distance(id1, id2);
          if(debugExtraCheckSymmetry) {
            float d2 = (float) distanceQuery.distance(id2, id1);
            if(Math.abs(d - d2) > 0.0000001) {
              LOG.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          try {
            matrix.getRecordBuffer(id1.getOffset(), id2.getOffset()).putFloat(d);
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
  public static class Par<O> extends AbstractApplication.Par {
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
    public void configure(Parameterization config) {
      super.configure(config);
      new ObjectParameter<Database>(DATABASE_ID, Database.class, StaticArrayDatabase.class) //
          .grab(config, x -> database = x);
      // Distance function parameter
      new ObjectParameter<Distance<? super O>>(CacheDoubleDistanceInOnDiskMatrix.Par.DISTANCE_ID, Distance.class) //
          .grab(config, x -> distance = x);
      // Output file parameter
      new FileParameter(CacheDoubleDistanceInOnDiskMatrix.Par.CACHE_ID, FileParameter.FileType.OUTPUT_FILE) //
          .grab(config, x -> out = x);
    }

    @Override
    public CacheFloatDistanceInOnDiskMatrix<O> make() {
      return new CacheFloatDistanceInOnDiskMatrix<>(database, distance, out);
    }
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    runCLIApplication(CacheFloatDistanceInOnDiskMatrix.class, args);
  }
}
