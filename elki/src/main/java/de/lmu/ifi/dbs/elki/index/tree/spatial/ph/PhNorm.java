package de.lmu.ifi.dbs.elki.index.tree.spatial.ph;

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

import ch.ethz.globis.pht.PhDistance;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.Norm;

final class PhNorm<O extends NumberVector> implements PhDistance {

  private final Norm<NumberVector> norm;
  private final PhNumberVectorAdapter o1;
  private final PhNumberVectorAdapter o2;
  
  PhNorm(Norm<O> norm, int dimensions) {
    this.norm = (Norm<NumberVector>) norm;
    this.o1 = new PhNumberVectorAdapter(dimensions);
    this.o2 = new PhNumberVectorAdapter(dimensions);
  }
  
  @Override
  public double dist(long[] v1, long[] v2) {
    return norm.distance((NumberVector)o1.wrap(v1), (NumberVector)o2.wrap(v2));
  }
  
  @Override
  public double distEst(long[] v1, long[] v2) {
    return norm.distance((NumberVector)o1.wrap(v1), (NumberVector)o2.wrap(v2));
  }
  
}