package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDBIDRangeDistanceFunction;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Provides a DistanceFunction that is based on float distances given by a
 * distance matrix of an external file.
 * 
 * @author Erich Schubert
 */
@Title("File based float distance for database objects.")
@Description("Loads float distance values from an external matrix.")
public class DiskCacheBasedFloatDistanceFunction extends AbstractDBIDRangeDistanceFunction {
  // TODO: constructor with file.

  /**
   * Parameter that specifies the name of the distance matrix file.
   * <p>
   * Key: {@code -distance.matrix}
   * </p>
   */
  public static final OptionID MATRIX_ID = new OptionID("distance.matrix", //
  "The name of the file containing the distance matrix.");

  /**
   * Magic to identify double cache matrices
   */
  public static final int FLOAT_CACHE_MAGIC = 23423411;

  /**
   * Storage required for a float value.
   */
  private static final int FLOAT_SIZE = 4;

  /**
   * The distance cache
   */
  private OnDiskUpperTriangleMatrix cache;

  /**
   * Constructor.
   * 
   * @param cache Distance matrix
   */
  public DiskCacheBasedFloatDistanceFunction(OnDiskUpperTriangleMatrix cache) {
    super();
    this.cache = cache;
  }

  @Override
  public double distance(int i1, int i2) {
    // the smaller id is the first key
    if(i1 > i2) {
      return distance(i2, i1);
    }

    try {
      return cache.getRecordBuffer(i1, i2).getFloat();
    }
    catch(IOException e) {
      throw new RuntimeException("Read error when loading distance " + i1 + "," + i2 + " from cache file.", e);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    DiskCacheBasedFloatDistanceFunction other = (DiskCacheBasedFloatDistanceFunction) obj;
    return this.cache.equals(other.cache);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    protected OnDiskUpperTriangleMatrix cache = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final FileParameter param = new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(param)) {
        File matrixfile = param.getValue();
        try {
          cache = new OnDiskUpperTriangleMatrix(matrixfile, FLOAT_CACHE_MAGIC, 0, FLOAT_SIZE, false);
        }
        catch(IOException e) {
          config.reportError(new WrongParameterValueException(param, matrixfile.toString(), e));
        }
      }
    }

    @Override
    protected DiskCacheBasedFloatDistanceFunction makeInstance() {
      return new DiskCacheBasedFloatDistanceFunction(cache);
    }
  }
}