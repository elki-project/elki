package de.lmu.ifi.dbs.elki.application.cache;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.external.DiskCacheBasedFloatDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ByteArrayUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.workflow.InputStep;

/**
 * Precompute an on-disk distance matrix, using float precision.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @apiviz.has OnDiskUpperTriangleMatrix
 * @apiviz.has DistanceFunction
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
  private InputStep input;

  /**
   * Distance function that is to be cached.
   */
  private DistanceFunction<O> distance;

  /**
   * Output file.
   */
  private File out;

  /**
   * Constructor.
   * 
   * @param input Data source
   * @param distance Distance function
   * @param out Matrix output file
   */
  public CacheFloatDistanceInOnDiskMatrix(InputStep input, DistanceFunction<O> distance, File out) {
    super();
    this.input = input;
    this.distance = distance;
    this.out = out;
  }

  @Override
  public void run() {
    Database database = input.getDatabase();
    Relation<O> relation = database.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> distanceQuery = database.getDistanceQuery(relation, distance);

    DBIDRange ids = DBIDUtil.assertRange(relation.getDBIDs());
    int matrixsize = ids.size();

    OnDiskUpperTriangleMatrix matrix;
    try {
      matrix = new OnDiskUpperTriangleMatrix(out, DiskCacheBasedFloatDistanceFunction.FLOAT_CACHE_MAGIC, 0, ByteArrayUtil.SIZE_FLOAT, matrixsize);
    }
    catch(IOException e) {
      throw new AbortException("Error creating output matrix.", e);
    }

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
    }
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
     * Data source to process.
     */
    private InputStep input = null;

    /**
     * Distance function that is to be cached.
     */
    private DistanceFunction<O> distance = null;

    /**
     * Output file.
     */
    private File out = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      input = config.tryInstantiate(InputStep.class);
      // Distance function parameter
      final ObjectParameter<DistanceFunction<O>> dpar = new ObjectParameter<>(CacheDoubleDistanceInOnDiskMatrix.Parameterizer.DISTANCE_ID, DistanceFunction.class);
      if(config.grab(dpar)) {
        distance = dpar.instantiateClass(config);
      }
      // Output file parameter
      final FileParameter cpar = new FileParameter(CacheDoubleDistanceInOnDiskMatrix.Parameterizer.CACHE_ID, FileParameter.FileType.OUTPUT_FILE);
      if(config.grab(cpar)) {
        out = cpar.getValue();
      }
    }

    @Override
    protected CacheFloatDistanceInOnDiskMatrix<O> makeInstance() {
      return new CacheFloatDistanceInOnDiskMatrix<>(input, distance, out);
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
