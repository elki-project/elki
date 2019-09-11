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
package elki.distance.strings;

import elki.data.type.SimpleTypeInformation;
import elki.distance.PrimitiveDistance;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Levenshtein distance on strings, normalized by string length.
 * <p>
 * Note: this is no longer a metric, the triangle inequality is violated.
 * Example: d("ab","bc")=1, d("ab", "abc")+d("abc","bc")=0.4+0.4=0.8
 * <p>
 * Reference:
 * <p>
 * V. I. Levenshtein<br>
 * Binary codes capable of correcting deletions, insertions and reversals.<br>
 * Soviet physics doklady. Vol. 10. 1966.
 * <p>
 * TODO: add case insensitive flag.
 * 
 * @author Felix Stahlberg
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - String
 */
@Description("Levenshtein distance, normalized by average string length.")
@Reference(authors = "V. I. Levenshtein", //
    title = "Binary codes capable of correcting deletions, insertions and reversals", //
    booktitle = "Soviet physics doklady 10", //
    bibkey = "journals/misc/Levenshtein66")
public class NormalizedLevenshteinDistance implements PrimitiveDistance<String> {
  /**
   * Static instance, case sensitive.
   */
  public static final NormalizedLevenshteinDistance STATIC_SENSITIVE = new NormalizedLevenshteinDistance();

  /**
   * Constructor. Use static instance instead.
   */
  @Deprecated
  public NormalizedLevenshteinDistance() {
    super();
  }

  @Override
  public double distance(String o1, String o2) {
    int cost = LevenshteinDistance.levenshteinDistance(o1, o2);
    return cost * 2.0 / (o1.length() + o2.length());
  }

  @Override
  public SimpleTypeInformation<? super String> getInputTypeRestriction() {
    return LevenshteinDistance.TYPE;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public NormalizedLevenshteinDistance make() {
      return STATIC_SENSITIVE;
    }
  }
}
