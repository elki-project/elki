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
public class CanberraDistanceFunction extends AbstractVectorDoubleDistanceFunction {
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
  public double doubleDistance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    final int dim = o1.getDimensionality();
    double sum = 0.0;
    for(int i = 1; i <= dim; i++) {
      double v1 = o1.doubleValue(i);
      double v2 = o2.doubleValue(i);
      sum += Math.abs(v1 - v2) / (Math.abs(v1) + Math.abs(v2));
    }
    return sum;
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