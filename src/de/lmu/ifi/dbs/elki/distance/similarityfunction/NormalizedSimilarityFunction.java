package de.lmu.ifi.dbs.elki.distance.similarityfunction;
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

import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Marker interface to signal that the similarity function is normalized to
 * produce values in the range of [0:1].
 * 
 * @author Erich Schubert
 * @param <O> object type
 * @param <D> distance type
 * 
 */
public interface NormalizedSimilarityFunction<O, D extends Distance<?>> extends SimilarityFunction<O, D> {
  // Empty - marker interface.
}
