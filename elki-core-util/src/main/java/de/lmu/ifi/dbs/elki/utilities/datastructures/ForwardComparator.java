package de.lmu.ifi.dbs.elki.utilities.datastructures;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2016
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

import java.util.Comparator;

/**
 * Regular comparator. See {@link java.util.Collections#reverseOrder()} for a
 * reverse comparator.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.exclude
 */
public final class ForwardComparator implements Comparator<Comparable<Object>> {
  /**
   * Static instance.
   */
  private static final Comparator<?> FORWARD = new ForwardComparator();

  @Override
  public int compare(Comparable<Object> o1, Comparable<Object> o2) {
    return o1.compareTo(o2);
  }

  /**
   * Compare two objects, forward. See
   * {@link java.util.Collections#reverseOrder()} for a reverse comparator.
   * 
   * @param <T> Object type
   * @return Forward comparator
   */
  @SuppressWarnings("unchecked")
  public static <T extends Comparable<?>> Comparator<T> forwardOrder() {
    return (Comparator<T>) FORWARD;
  }
}
