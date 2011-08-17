package de.lmu.ifi.dbs.elki.algorithm.clustering;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;

/**
 * Interface for OPTICS type algorithms, that can be analysed by OPTICS Xi etc.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ClusterOrderResult
 * 
 * @param <D> Distance type
 */
public interface OPTICSTypeAlgorithm<D extends Distance<D>> extends Algorithm {
  @Override
  ClusterOrderResult<D> run(Database database) throws IllegalStateException;
  
  /**
   * Get the minpts value used. Needed for OPTICS Xi etc.
   * 
   * @return minpts value
   */
  public int getMinPts();

  /**
   * Get the distance factory. Needed for type checking (i.e. is number distance)
   * 
   * @return distance factory
   */
  public D getDistanceFactory();
}