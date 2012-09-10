package de.lmu.ifi.dbs.elki.distance.distancefunction;

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
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialPrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Canberra distance function, a variation of Manhattan distance.
 * 
 * <p>
 * Reference:<br />
 * G. N. Lance, W. T. Williams<br />
 * Computer programs for hierarchical polythetic classification ("similarity
 * analysis")<br />
 * In: Computer Journal, Volume 9, Issue 1
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "G. N. Lance, W. T. Williams", title = "Computer programs for hierarchical polythetic classification (similarity analysis).", booktitle = "Computer Journal, Volume 9, Issue 1", url = "http://comjnl.oxfordjournals.org/content/9/1/60.short")
public class CanberraDistanceFunction extends AbstractVectorDoubleDistanceFunction implements SpatialPrimitiveDoubleDistanceFunction<NumberVector<?>> {
  /**
   * Static instance. Use this!
   */
  public static final CanberraDistanceFunction STATIC = new CanberraDistanceFunction();

  /**
   * Constructor.
   */
  protected CanberraDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?> o1, NumberVector<?> o2) {
    final int dim = o1.getDimensionality();
    double sum = 0.0;
    for(int i = 1; i <= dim; i++) {
      double v1 = o1.doubleValue(i);
      double v2 = o2.doubleValue(i);
      final double div = Math.abs(v1) + Math.abs(v2);
      if (div > 0) {
        sum += Math.abs(v1 - v2) / div;
      }
    }
    return sum;
  }

  @Override
  public double doubleMinDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    final int dim = mbr1.getDimensionality();
    double sum = 0.0;
    for(int d = 1; d <= dim; d++) {
      final double m1, m2;
      if(mbr1.getMax(d) < mbr2.getMin(d)) {
        m1 = mbr2.getMin(d);
        m2 = mbr1.getMax(d);
      }
      else if(mbr1.getMin(d) > mbr2.getMax(d)) {
        m1 = mbr1.getMin(d);
        m2 = mbr2.getMax(d);
      }
      else { // The mbrs intersect!
        continue;
      }
      final double manhattanI = m1 - m2;
      final double a1 = Math.max(-mbr1.getMin(d), mbr1.getMax(d));
      final double a2 = Math.max(-mbr2.getMin(d), mbr2.getMax(d));
      final double div = a1 + a2;
      if (div > 0) {
        sum += manhattanI / div;
      }
    }
    return sum;
  }

  @Override
  public DoubleDistance minDist(SpatialComparable mbr1, SpatialComparable mbr2) {
    return new DoubleDistance(doubleMinDist(mbr1, mbr2));
  }

  @Override
  public <T extends NumberVector<?>> SpatialDistanceQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new SpatialPrimitiveDistanceQuery<T, DoubleDistance>(relation, this);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CanberraDistanceFunction makeInstance() {
      return CanberraDistanceFunction.STATIC;
    }
  }
}