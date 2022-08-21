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
package elki.distance.external;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import elki.database.ids.DBIDRange;
import elki.distance.AbstractDBIDRangeDistance;
import elki.logging.Logging;
import elki.persistent.OnDiskUpperTriangleMatrix;
import elki.utilities.io.ByteArrayUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.WrongParameterValueException;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Distance function that is based on double distances given by a distance
 * matrix of an external binary matrix file.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public class DiskCacheBasedDoubleDistance extends AbstractDBIDRangeDistance {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(DiskCacheBasedDoubleDistance.class);

  /**
   * Magic to identify double cache matrices
   */
  public static final int DOUBLE_CACHE_MAGIC = 50902811;

  /**
   * The distance matrix
   */
  private OnDiskUpperTriangleMatrix cache;

  /**
   * Constructor.
   * 
   * @param cache Distance matrix
   */
  public DiskCacheBasedDoubleDistance(OnDiskUpperTriangleMatrix cache) {
    super();
    this.cache = cache;
  }

  /**
   * Constructor.
   *
   * @param matrixfile File name
   * @throws IOException
   */
  public DiskCacheBasedDoubleDistance(Path matrixfile) throws IOException {
    super();
    this.cache = new OnDiskUpperTriangleMatrix(matrixfile, DOUBLE_CACHE_MAGIC, 0, ByteArrayUtil.SIZE_DOUBLE, false);
  }

  @Override
  public double distance(int i1, int i2) {
    try {
      return cache.getRecordBuffer(i1, i2).getDouble();
    }
    catch(IOException e) {
      throw new RuntimeException("Read error when loading distance " + i1 + "," + i2 + " from cache file.", e);
    }
  }

  @Override
  public void checkRange(DBIDRange range) {
    if(cache.getMatrixSize() < range.size()) {
      LOG.warning("Distance matrix has size " + cache.getMatrixSize() + " but range has size: " + range.size());
    }
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DiskCacheBasedDoubleDistance other = (DiskCacheBasedDoubleDistance) obj;
    return this.cache.equals(other.cache);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter that specifies the name of the distance matrix file.
     */
    public static final OptionID MATRIX_ID = new OptionID("distance.matrix", //
        "The name of the file containing the distance matrix.");

    /**
     * The distance matrix
     */
    protected OnDiskUpperTriangleMatrix cache = null;

    @Override
    public void configure(Parameterization config) {
      FileParameter param = new FileParameter(MATRIX_ID, FileParameter.FileType.INPUT_FILE);
      param.grab(config, matrixfile -> {
        // FIXME: use Path here
        try {
          cache = new OnDiskUpperTriangleMatrix(Paths.get(matrixfile), DOUBLE_CACHE_MAGIC, 0, ByteArrayUtil.SIZE_DOUBLE, false);
        }
        catch(IOException e) {
          config.reportError(new WrongParameterValueException(param, matrixfile.toString(), e.getMessage(), e));
        }
      });
    }

    @Override
    public DiskCacheBasedDoubleDistance make() {
      return new DiskCacheBasedDoubleDistance(cache);
    }
  }
}
