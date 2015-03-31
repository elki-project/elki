package de.lmu.ifi.dbs.elki.index.lsh.hashfunctions;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.math.linearalgebra.randomprojections.RandomProjectionFamily.Projection;

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

public class CosineLocalitySensitiveHashFunction implements LocalitySensitiveHashFunction<NumberVector>{
  
  private RandomProjectionFamily.Projection projection;
  public CosineLocalitySensitiveHashFunction(Projection projection) {
    this.projection=projection;
  }
  @Override
  public int hashObject(NumberVector obj) {
    double[] projectionResult = projection.project(obj);
    int hashValue=0;
    for(int i = 0; i < projectionResult.length; i++) {
      if(projectionResult[i]>0)
      {
        hashValue=hashValue+(1<<i);
      }
    }
    return hashValue;
  }

}
