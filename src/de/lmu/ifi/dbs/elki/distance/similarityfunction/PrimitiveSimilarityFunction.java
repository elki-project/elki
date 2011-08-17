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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Interface SimilarityFunction describes the requirements of any similarity
 * function.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public interface PrimitiveSimilarityFunction<O, D extends Distance<D>> extends SimilarityFunction<O, D> {
  /**
   * Computes the similarity between two given DatabaseObjects according to this
   * similarity function.
   * 
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the similarity between two given DatabaseObjects according to this
   *         similarity function
   */
  D similarity(O o1, O o2);

  @Override
  abstract public SimpleTypeInformation<? super O> getInputTypeRestriction();
}