/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.data.spatial;

import de.lmu.ifi.dbs.elki.data.ModifiableHyperBoundingBox;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;

/**
 * Utility class with spatial functions.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @navassoc - compares - SpatialComparable
 * @navhas - creates - ModifiableHyperBoundingBox
 */
// IMPORTANT NOTE: when editing this class, bear in mind that the dimension
// numbers start at 1 or 0 depending on the context!
public final class SpatialUtil {
  /**
   * Fake constructor: do not instantiate.
   */
  private SpatialUtil() {
    // Do not instantiate.
  }

  /**
   * Check that two spatial objects have the same dimensionality.
   * 
   * @param box1 First object
   * @param box2 Second object
   * @return Dimensionality
   * @throws IllegalArgumentException when the dimensionalities do not agree
   */
  public static int assertSameDimensionality(SpatialComparable box1, SpatialComparable box2) {
    final int dim = box1.getDimensionality();
    if(dim != box2.getDimensionality()) {
      throw new IllegalArgumentException("The spatial objects do not have the same dimensionality!");
    }
    return dim;
  }

  /**
   * Returns a clone of the minimum hyper point.
   * 
   * @param box spatial object
   * @return the minimum hyper point
   */
  public static double[] getMin(SpatialComparable box) {
    final int dim = box.getDimensionality();
    double[] min = new double[dim];
    for(int i = 0; i < dim; i++) {
      min[i] = box.getMin(i);
    }
    return min;
  }

  /**
   * Returns a clone of the maximum hyper point.
   * 
   * @param box spatial object
   * @return the maximum hyper point
   */
  public static double[] getMax(SpatialComparable box) {
    final int dim = box.getDimensionality();
    double[] max = new double[dim];
    for(int i = 0; i < dim; i++) {
      max[i] = box.getMax(i);
    }
    return max;
  }

  /**
   * Returns true if the two SpatialComparables intersect, false otherwise.
   * 
   * @param box1 the first SpatialComparable
   * @param box2 the first SpatialComparable
   * @return true if the SpatialComparables intersect, false otherwise
   */
  public static boolean intersects(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);
    for(int i = 0; i < dim; i++) {
      if(box2.getMax(i) < box1.getMin(i) || box1.getMax(i) < box2.getMin(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the first SpatialComparable contains the second
   * SpatialComparable, false otherwise.
   * 
   * @param box1 the outer SpatialComparable
   * @param box2 the inner SpatialComparable
   * @return true if the first SpatialComparable contains the second
   *         SpatialComparable, false otherwise
   */
  public static boolean contains(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);
    for(int i = 0; i < dim; i++) {
      if(box2.getMin(i) < box1.getMin(i) || box1.getMax(i) < box2.getMax(i)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if this SpatialComparable contains the given point, false
   * otherwise.
   * 
   * @param box spatial object
   * @param point the point to be tested for containment
   * @return true if this SpatialComparable contains the given point, false
   *         otherwise
   */
  public static boolean contains(SpatialComparable box, double[] point) {
    final int dim = box.getDimensionality();
    if(dim != point.length) {
      throw new IllegalArgumentException("This HyperBoundingBox and the given point need same dimensionality");
    }

    for(int i = 0; i < dim; i++) {
      if(box.getMin(i) > point[i] || box.getMax(i) < point[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Computes the volume of this SpatialComparable.
   * 
   * @param box Spatial object
   * @return the volume of this SpatialComparable
   */
  public static double volume(SpatialComparable box) {
    final int dim = box.getDimensionality();
    double vol = 1.;
    for(int i = 0; i < dim; i++) {
      double delta = box.getMax(i) - box.getMin(i);
      if(delta == 0.) {
        return 0.;
      }
      vol *= delta;
    }
    return vol;
  }

  /**
   * Compute the volume (area) of the union of two MBRs.
   * 
   * @param box1 First object
   * @param box2 Second object
   * @return Volume of union
   */
  public static double volumeUnion(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);
    double volume = 1.;
    for(int i = 0; i < dim; i++) {
      final double min = Math.min(box1.getMin(i), box2.getMin(i));
      final double max = Math.max(box1.getMax(i), box2.getMax(i));
      volume *= (max - min);
    }
    return volume;
  }

  /**
   * Computes the volume of this SpatialComparable.
   * 
   * @param box Box
   * @param scale Scaling factor
   * @return the volume of this SpatialComparable
   */
  public static double volumeScaled(SpatialComparable box, double scale) {
    final int dim = box.getDimensionality();
    double vol = 1.;
    for(int i = 0; i < dim; i++) {
      double delta = box.getMax(i) - box.getMin(i);
      if(delta == 0.) {
        return 0.;
      }
      vol *= delta * scale;
    }
    return vol;
  }

  /**
   * Compute the volume (area) of the union of two MBRs.
   * 
   * @param box1 First object
   * @param box2 Second object
   * @param scale Scaling factor
   * @return Volume of union
   */
  public static double volumeUnionScaled(SpatialComparable box1, SpatialComparable box2, double scale) {
    final int dim = assertSameDimensionality(box1, box2);
    double volume = 1.;
    for(int i = 0; i < dim; i++) {
      final double min = Math.min(box1.getMin(i), box2.getMin(i));
      final double max = Math.max(box1.getMax(i), box2.getMax(i));
      volume *= (max - min) * scale;
    }
    return volume;
  }

  /**
   * Compute the enlargement obtained by adding an object to an existing object.
   * 
   * @param exist Existing rectangle
   * @param addit Additional rectangle
   * @return Enlargement factor
   */
  public static double enlargement(SpatialComparable exist, SpatialComparable addit) {
    final int dim = assertSameDimensionality(exist, addit);
    double v1 = 1.;
    double v2 = 1.;
    for(int i = 0; i < dim; i++) {
      final double emin = exist.getMin(i);
      final double emax = exist.getMax(i);
      final double amin = addit.getMin(i);
      final double amax = addit.getMax(i);

      final double min = Math.min(emin, amin);
      final double max = Math.max(emax, amax);
      v1 *= (max - min);
      v2 *= (emax - emin);
    }
    return v2 - v1;
  }

  /**
   * Compute the enlargement obtained by adding an object to an existing object.
   * 
   * @param exist Existing rectangle
   * @param addit Additional rectangle
   * @param scale Scaling helper
   * @return Enlargement factor
   */
  public static double enlargementScaled(SpatialComparable exist, SpatialComparable addit, double scale) {
    final int dim = assertSameDimensionality(exist, addit);
    double v1 = 1.;
    double v2 = 1.;
    for(int i = 0; i < dim; i++) {
      final double emin = exist.getMin(i);
      final double emax = exist.getMax(i);
      final double amin = addit.getMin(i);
      final double amax = addit.getMax(i);

      final double min = Math.min(emin, amin);
      final double max = Math.max(emax, amax);
      v1 *= (max - min) * scale;
      v2 *= (emax - emin) * scale;
    }
    return v2 - v1;
  }

  /**
   * Computes the perimeter of this SpatialComparable.
   * 
   * @param box spatial object
   * @return the perimeter of this SpatialComparable
   */
  public static double perimeter(SpatialComparable box) {
    final int dim = box.getDimensionality();
    double perimeter = 0.;
    for(int i = 0; i < dim; i++) {
      perimeter += box.getMax(i) - box.getMin(i);
    }
    return perimeter;
  }

  /**
   * Computes the volume of the overlapping box between two SpatialComparables.
   * 
   * @param box1 the first SpatialComparable
   * @param box2 the second SpatialComparable
   * @return the overlap volume.
   */
  public static double overlap(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);

    // the maximal and minimal value of the overlap box.
    double omax, omin;

    // the overlap volume
    double overlap = 1.;

    for(int i = 0; i < dim; i++) {
      // The maximal value of that overlap box in the current
      // dimension is the minimum of the max values.
      omax = Math.min(box1.getMax(i), box2.getMax(i));
      // The minimal value is the maximum of the min values.
      omin = Math.max(box1.getMin(i), box2.getMin(i));

      // if omax <= omin in any dimension, the overlap box has a volume of zero
      if(omax <= omin) {
        return 0.;
      }

      overlap *= omax - omin;
    }

    return overlap;
  }

  /**
   * Computes the volume of the overlapping box between two SpatialComparables
   * and return the relation between the volume of the overlapping box and the
   * volume of both SpatialComparable.
   * 
   * @param box1 the first SpatialComparable
   * @param box2 the second SpatialComparable
   * @return the overlap volume in relation to the singular volumes.
   */
  public static double relativeOverlap(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);

    // the overlap volume
    double overlap = 1.;
    double vol1 = 1.;
    double vol2 = 1.;

    for(int i = 0; i < dim; i++) {
      final double box1min = box1.getMin(i);
      final double box1max = box1.getMax(i);
      final double box2min = box2.getMin(i);
      final double box2max = box2.getMax(i);

      final double omax = Math.min(box1max, box2max);
      final double omin = Math.max(box1min, box2min);

      // if omax <= omin in any dimension, the overlap box has a volume of zero
      if(omax <= omin) {
        return 0.;
      }

      overlap *= omax - omin;
      vol1 *= box1max - box1min;
      vol2 *= box2max - box2min;
    }

    return overlap / (vol1 + vol2);
  }

  /**
   * Computes the union HyperBoundingBox of two SpatialComparables.
   * 
   * @param box1 the first SpatialComparable
   * @param box2 the second SpatialComparable
   * @return the union HyperBoundingBox of this HyperBoundingBox and the given
   *         HyperBoundingBox
   */
  public static ModifiableHyperBoundingBox union(SpatialComparable box1, SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);

    double[] min = new double[dim];
    double[] max = new double[dim];

    for(int i = 0; i < dim; i++) {
      min[i] = Math.min(box1.getMin(i), box2.getMin(i));
      max[i] = Math.max(box1.getMax(i), box2.getMax(i));
    }
    return new ModifiableHyperBoundingBox(min, max);
  }

  /**
   * Returns the union of the two specified MBRs. Tolerant of "null" values.
   * 
   * @param mbr1 the first MBR
   * @param mbr2 the second MBR
   * @return the union of the two specified MBRs
   */
  public static ModifiableHyperBoundingBox unionTolerant(SpatialComparable mbr1, SpatialComparable mbr2) {
    if(mbr1 == null && mbr2 == null) {
      return null;
    }
    if(mbr1 == null) {
      // Clone - intentionally
      return new ModifiableHyperBoundingBox(mbr2);
    }
    if(mbr2 == null) {
      // Clone - intentionally
      return new ModifiableHyperBoundingBox(mbr1);
    }
    return union(mbr1, mbr2);
  }

  /**
   * Compute the union of a number of objects as a flat MBR (low-level, for
   * index structures).
   * 
   * @param data Object
   * @param getter Array adapter
   * @param <E> object type
   * @param <A> data value type
   * @return Flat MBR
   */
  public static <E extends SpatialComparable, A> double[] unionFlatMBR(A data, ArrayAdapter<E, ? super A> getter) {
    final int num = getter.size(data);
    assert (num > 0) : "Cannot compute MBR of empty set.";
    final int dim;
    final double[] mbr;
    { // First entry
      final E first = getter.get(data, 0);
      dim = first.getDimensionality();
      mbr = new double[2 * dim];
      for(int d = 0; d < dim; d++) {
        mbr[d] = first.getMin(d);
        mbr[dim + d] = first.getMax(d);
      }
    } // Remaining entries
    for(int i = 1; i < num; i++) {
      E next = getter.get(data, i);
      for(int d = 0; d < dim; d++) {
        mbr[d] = Math.min(mbr[d], next.getMin(d));
        mbr[dim + d] = Math.max(mbr[dim + d], next.getMax(d));
      }
    }
    return mbr;
  }

  /**
   * Calculate the intersection of the two MBRs or <code>null</code> if they do
   * not intersect. <em>Note</em>: if the given MBRs intersect in only one point
   * of any dimension, this method still returns a result!
   * 
   * @param box1 the first MBR
   * @param box2 the second MBR
   * @return the union of the two specified MBRs
   */
  public static ModifiableHyperBoundingBox intersection(final SpatialComparable box1, final SpatialComparable box2) {
    final int dim = assertSameDimensionality(box1, box2);

    double[] min = new double[dim];
    double[] max = new double[dim];

    for(int i = 0; i < dim; i++) {
      min[i] = Math.max(box1.getMin(i), box2.getMin(i));
      max[i] = Math.min(box1.getMax(i), box2.getMax(i));
      if(min[i] > max[i]) {
        return null;
      }
    }
    return new ModifiableHyperBoundingBox(min, max);
  }

  /**
   * Returns the centroid of this SpatialComparable.
   * 
   * @param obj Spatial object to process
   * @return the centroid of this SpatialComparable
   */
  public static double[] centroid(SpatialComparable obj) {
    final int dim = obj.getDimensionality();
    double[] centroid = new double[dim];
    for(int d = 0; d < dim; d++) {
      centroid[d] = (obj.getMax(d) + obj.getMin(d)) * .5;
    }
    return centroid;
  }

  /**
   * Test two SpatialComparables for equality.
   * 
   * @param box1 First bounding box
   * @param box2 Second bounding box
   * @return true when the boxes are equal
   */
  public static boolean equals(SpatialComparable box1, SpatialComparable box2) {
    if(box1.getDimensionality() != box2.getDimensionality()) {
      return false;
    }
    for(int i = 0; i < box1.getDimensionality(); i++) {
      if(box1.getMin(i) != box2.getMin(i)) {
        return false;
      }
      if(box1.getMax(i) != box2.getMax(i)) {
        return false;
      }
    }
    return true;
  }
}
