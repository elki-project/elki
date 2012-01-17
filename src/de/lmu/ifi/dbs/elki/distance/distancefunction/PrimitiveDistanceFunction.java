package de.lmu.ifi.dbs.elki.distance.distancefunction;

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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Primitive distance function that is defined on some kind of object.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * 
 * @param <O> input object type
 * @param <D> distance result type
 */
public interface PrimitiveDistanceFunction<O, D extends Distance<?>> extends DistanceFunction<O, D> {
  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function.
   * 
   * @param o1 first DatabaseObject
   * @param o2 second DatabaseObject
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  D distance(O o1, O o2);
  
  @Override
  SimpleTypeInformation<? super O> getInputTypeRestriction();  
}