package de.lmu.ifi.dbs.elki.distance.distancefunction.strings;

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

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Levenshtein distance on strings, normalized by string length.
 * 
 * Reference:
 * <p>
 * V. I. Levenshtein<br>
 * Binary codes capable of correcting deletions, insertions and reversals.<br>
 * Soviet physics doklady. Vol. 10. 1966.
 * </p>
 * 
 * TODO: add case insensitive flag.
 * 
 * @author Felix Stahlberg
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @apiviz.uses String
 */
@Description("Levenshtein distance, normalized by average string length.")
@Reference(authors = "V. I. Levenshtein", title = "Binary codes capable of correcting deletions, insertions and reversals.", booktitle = "Soviet physics doklady. Vol. 10. 1966.")
public class NormalizedLevenshteinDistanceFunction extends AbstractPrimitiveDistanceFunction<String> {
  /**
   * Static instance, case sensitive.
   */
  public static final NormalizedLevenshteinDistanceFunction STATIC_SENSITIVE = new NormalizedLevenshteinDistanceFunction();

  /**
   * Constructor. Use static instance instead.
   */
  @Deprecated
  public NormalizedLevenshteinDistanceFunction() {
    super();
  }

  @Override
  public double distance(String o1, String o2) {
    int cost = LevenshteinDistanceFunction.levenshteinDistance(o1, o2);
    return cost * 2.0 / (o1.length() + o2.length());
  }

  @Override
  public SimpleTypeInformation<? super String> getInputTypeRestriction() {
    return TypeUtil.STRING;
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
    protected NormalizedLevenshteinDistanceFunction makeInstance() {
      return STATIC_SENSITIVE;
    }
  }
}
