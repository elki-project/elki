package de.lmu.ifi.dbs.elki.index.lsh.hashfunctions;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.RandomProjectionFamily.Projection;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Random projection family to use with sparse vectors.
 *
 * Reference:
 * <p>
 * M.S. Charikar<br />
 * Similarity estimation techniques from rounding algorithms<br />
 * Proc. 34th ACM Symposium on Theory of computing, STOC'02
 * </p>
 *
 * @author Evgeniy Faerman
 * @since 0.3
 */
@Reference(authors = "M.S. Charikar", //
title = "Similarity estimation techniques from rounding algorithms", //
booktitle = "Proc. 34th ACM Symposium on Theory of computing, STOC'02", //
url = "https://dx.doi.org/10.1145/509907.509965")
public class CosineLocalitySensitiveHashFunction implements LocalitySensitiveHashFunction<NumberVector> {
  /**
   * Projection function.
   */
  private RandomProjectionFamily.Projection projection;

  /**
   * Constructor.
   *
   * @param projection Projection
   */
  public CosineLocalitySensitiveHashFunction(Projection projection) {
    this.projection = projection;
  }

  @Override
  public int hashObject(NumberVector obj) {
    double[] buf = projection.project(obj);
    int hashValue = 0;
    for(int i = 0, j = 1; i < buf.length; i++, j <<= 1) {
      if(buf[i] > 0) {
        hashValue = hashValue | j;
      }
    }
    return hashValue;
  }

  @Override
  public int hashObject(NumberVector obj, double[] buf) {
    projection.project(obj, buf);
    int hashValue = 0;
    for(int i = 0, j = 1; i < buf.length; i++, j <<= 1) {
      if(buf[i] > 0) {
        hashValue = hashValue | j;
      }
    }
    return hashValue;
  }

  @Override
  public int getNumberOfProjections() {
    return projection.getOutputDimensionality();
  }
}
