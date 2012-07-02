package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDBIDDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.persistent.OnDiskUpperTriangleMatrix;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;

/**
 * Provides a DistanceFunction that is based on double distances given by a
 * distance matrix of an external file.
 * 
 * @author Erich Schubert
 */
@Title("File based double distance for database objects.")
@Description("Loads double distance values from an external matrix.")
public class DiskCacheBasedDoubleDistanceFunction extends AbstractDBIDDistanceFunction<DoubleDistance> {
  // TODO: constructor with file.

  /**
   * Parameter that specifies the name of the distance matrix file.
   * <p>
   * Key: {@code -distance.matrix}
   * </p>
   */
  public static final OptionID MATRIX_ID = OptionID.getOrCreateOptionID("distance.matrix", "The name of the file containing the distance matrix.");

  /**
   * Magic to identify double cache matrices
   */
  public static final int DOUBLE_CACHE_MAGIC = 50902811;

  /**
   * Storage required for a double value.
   */
  private static final int DOUBLE_SIZE = 8;

  /**
   * The distance matrix
   */
  private OnDiskUpperTriangleMatrix cache;

  /**
   * Constructor.
   * 
   * @param cache Distance matrix
   */
  public DiskCacheBasedDoubleDistanceFunction(OnDiskUpperTriangleMatrix cache) {
    super();
    this.cache = cache;
  }

  /**
   * Returns the distance between the two objects specified by their objects
   * ids. If a cache is used, the distance value is looked up in the cache. If
   * the distance does not yet exists in cache, it will be computed an put to
   * cache. If no cache is used, the distance is computed.
   * 
   * @param id1 first object id
   * @param id2 second object id
   * @return the distance between the two objects specified by their objects ids
   */
  @Override
  public DoubleDistance distance(DBIDRef id1, DBIDRef id2) {
    if(id1 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    if(id2 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    final int intid1 = DBIDUtil.asInteger(id1);
    final int intid2 = DBIDUtil.asInteger(id2);
    if(intid1 < 0 || intid2 < 0) {
      throw new AbortException("Negative DBIDs not supported in OnDiskCache");
    }
    // the smaller id is the first key
    if(intid1 > intid2) {
      return distance(id2, id1);
    }

    double distance;
    try {
      distance = cache.getRecordBuffer(intid1, intid2).getDouble();
    }
    catch(IOException e) {
      throw new RuntimeException("Read error when loading distance " + id1 + "," + id2 + " from cache file.", e);
    }
    return new DoubleDistance(distance);
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    DiskCacheBasedDoubleDistanceFunction other = (DiskCacheBasedDoubleDistanceFunction) obj;
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
          cache = new OnDiskUpperTriangleMatrix(matrixfile, DOUBLE_CACHE_MAGIC, 0, DOUBLE_SIZE, false);
        }
        catch(IOException e) {
          config.reportError(new WrongParameterValueException(param, matrixfile.toString(), e));
        }
      }
    }

    @Override
    protected DiskCacheBasedDoubleDistanceFunction makeInstance() {
      return new DiskCacheBasedDoubleDistanceFunction(cache);
    }
  }
}