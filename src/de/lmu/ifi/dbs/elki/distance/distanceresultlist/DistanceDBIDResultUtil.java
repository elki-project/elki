package de.lmu.ifi.dbs.elki.distance.distanceresultlist;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Utility classes for distance DBID results.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses DistanceDBIDPair oneway - -
 */
public final class DistanceDBIDResultUtil {
  /**
   * Get a comparator to sort by distance, then DBID
   * 
   * @return comparator
   */
  @SuppressWarnings("unchecked")
  public static <D extends DistanceDBIDPair<?>> Comparator<D> distanceComparator() {
    return (Comparator<D>) BY_DISTANCE_THEN_DBID;
  }

  /**
   * Sort a Java list by distance.
   * 
   * @param list List to sort
   */
  @SuppressWarnings("unchecked")
  public static <D extends DistanceDBIDPair<?>> void sortByDistance(List<? extends D> list) {
    Collections.sort(list, (Comparator<D>) BY_DISTANCE_THEN_DBID);
  }

  /**
   * Static comparator.
   */
  private static final Comparator<DistanceDBIDPair<?>> BY_DISTANCE_THEN_DBID = new Comparator<DistanceDBIDPair<?>>() {
    @SuppressWarnings("unchecked")
    @Override
    public int compare(DistanceDBIDPair<?> o1, DistanceDBIDPair<?> o2) {
      final int d = ((DistanceDBIDPair<DoubleDistance>)o1).compareByDistance((DistanceDBIDPair<DoubleDistance>)o2);
      return (d == 0) ? DBIDUtil.compare(o1, o2) : d;
    }
  };

  public static String toString(DistanceDBIDResult<?> res) {
    StringBuffer buf = new StringBuffer();
    buf.append('[');
    DistanceDBIDResultIter<?> iter = res.iter();
    for(; iter.valid(); iter.advance()) {
      if(buf.length() > 1) {
        buf.append(", ");
      }
      buf.append(iter.getDistancePair().toString());
    }
    buf.append(']');
    return buf.toString();
  }
}