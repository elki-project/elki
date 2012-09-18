package de.lmu.ifi.dbs.elki.application.cache;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.application.AbstractApplication;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Wrapper to convert a traditional text-serialized result into a on-disk matrix
 * for random access.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has OnDiskUpperTriangleMatrix
 * @apiviz.has DistanceFunction
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class CacheDoubleDistanceInOnDiskMatrix<O, D extends NumberDistance<D, ?>> extends AbstractApplication {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(CacheDoubleDistanceInOnDiskMatrix.class);

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.diskcache}
   * </p>
   */
  public static final OptionID CACHE_ID = OptionID.getOrCreateOptionID("loader.diskcache", "File name of the disk cache to create.");

  /**
   * Parameter that specifies the name of the directory to be re-parsed.
   * <p>
   * Key: {@code -loader.distance}
   * </p>
   */
  public static final OptionID DISTANCE_ID = OptionID.getOrCreateOptionID("loader.distance", "Distance function to cache.");

  /**
   * Debug flag, to double-check all write operations.
   */
  private static final boolean debugExtraCheckSymmetry = false;

  /**
   * Holds the database connection to have the algorithm run with.
   */
  private Database database;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O, D> distance;

  /**
   * Output file.
   */
  private File out;

  /**
   * Constructor.
   * 
   * @param verbose Verbose flag
   * @param database Database
   * @param distance Distance function
   * @param out Matrix output file
   */
  public CacheDoubleDistanceInOnDiskMatrix(boolean verbose, Database database, DistanceFunction<O, D> distance, File out) {
    super(verbose);
    this.database = database;
    this.distance = distance;
    this.out = out;
  }

  @Override
  public void run() {
    database.initialize();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O, D> distanceQuery = database.getDistanceQuery(relation, distance);

    int matrixsize = 0;
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      int intid = DBIDUtil.asInteger(iditer);
      matrixsize = Math.max(matrixsize, intid + 1);
      if(intid < 0) {
        throw new AbortException("OnDiskMatrixCache does not allow negative DBIDs.");
      }
    }

    OnDiskUpperTriangleMatrix matrix;
    try {
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedDoubleDistanceFunction.DOUBLE_CACHE_MAGIC, 0, 8, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix.", e);
    }

    for(DBIDIter id1 = relation.iterDBIDs(); id1.valid(); id1.advance()) {
      for(DBIDIter id2 = relation.iterDBIDs(); id2.valid(); id2.advance()) {
        if(DBIDUtil.asInteger(id2) >= DBIDUtil.asInteger(id1)) {
          double d = distanceQuery.distance(id1, id2).doubleValue();
          if(debugExtraCheckSymmetry) {
            double d2 = distanceQuery.distance(id2, id1).doubleValue();
            if(Math.abs(d - d2) > 0.0000001) {
              LOG.warning("Distance function doesn't appear to be symmetric!");
            }
          }
          try {
            matrix.getRecordBuffer(DBIDUtil.asInteger(id1), DBIDUtil.asInteger(id2)).putDouble(d);
          }
          catch(IOException e) {
            throw new AbortException("Error writing distance record " + DBIDFactory.FACTORY.toString(id1) + "," + DBIDFactory.FACTORY.toString(id2) + " to matrix.", e);
          }
        }
      }
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends NumberDistance<D, ?>> extends AbstractApplication.Parameterizer {
    /**
     * Holds the database connection to have the algorithm run with.
     */
    private Database database = null;

    /**
     * Distance function that is to be cached.
     */
    private DistanceFunction<O, D> distance = null;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Database connection parameter
      final ObjectParameter<Database> dbpar = new ObjectParameter<Database>(OptionID.DATABASE_CONNECTION, Database.class, StaticArrayDatabase.class);
      if(config.grab(dbpar)) {
        database = dbpar.instantiateClass(config);
      }
      // Distance function parameter
      final ObjectParameter<DistanceFunction<O, D>> dpar = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_ID, DistanceFunction.class);
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
    protected CacheDoubleDistanceInOnDiskMatrix<O, D> makeInstance() {
      return new CacheDoubleDistanceInOnDiskMatrix<O, D>(verbose, database, distance, out);
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