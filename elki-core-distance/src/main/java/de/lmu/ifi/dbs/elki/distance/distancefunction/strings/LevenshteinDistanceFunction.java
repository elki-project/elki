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
package de.lmu.ifi.dbs.elki.distance.distancefunction.strings;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Classic Levenshtein distance on strings.
 * <p>
 * Reference:
 * <p>
 * V. I. Levenshtein<br>
 * Binary codes capable of correcting deletions, insertions and reversals<br>
 * Soviet physics doklady 10
 * <p>
 * TODO: add case insensitive flag.
 * <p>
 * TODO: add an API that can stop early at a maximum distance
 * 
 * @author Felix Stahlberg
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - String
 */
@Description("Levenshtein distance.")
@Reference(authors = "V. I. Levenshtein", //
    title = "Binary codes capable of correcting deletions, insertions and reversals", //
    booktitle = "Soviet physics doklady 10", //
    bibkey = "journals/misc/Levenshtein66")
public class LevenshteinDistanceFunction implements PrimitiveDistanceFunction<String> {
  /**
   * Static instance, case sensitive.
   */
  public static final LevenshteinDistanceFunction STATIC_SENSITIVE = new LevenshteinDistanceFunction();

  /**
   * Input data type.
   */
  protected static final SimpleTypeInformation<? super String> TYPE = new SimpleTypeInformation<>(String.class);

  /**
   * Constructor. Use static instance instead.
   */
  @Deprecated
  public LevenshteinDistanceFunction() {
    super();
  }

  @Override
  public double distance(String o1, String o2) {
    return levenshteinDistance(o1, o2);
  }

  /**
   * Levenshtein distance for two strings.
   * 
   * @param o1 First string
   * @param o2 Second string
   * @return Levenshtein distance
   */
  public static int levenshteinDistance(String o1, String o2) {
    // Let o1 be the shorter one:
    if(o1.length() > o2.length()) {
      return levenshteinDistance(o2, o1);
    }
    final int l1 = o1.length(), l2 = o2.length();
    // Exploit that Java caches the hash code of strings:
    if(l1 == l2 && o1.hashCode() == o2.hashCode() && o1.equals(o2)) {
      return 0;
    }
    // Determine prefix and postfix lengths:
    final int prefix = prefixLen(o1, o2);
    if(prefix == l1 || prefix == l2) {
      return Math.abs(l1 - l2);
    }
    final int postfix = postfixLen(o1, o2, prefix);
    return // Prefix and postfix are a complete object:
    (prefix + postfix == l1 || prefix + postfix == l2) ? Math.abs(l1 - l2) : //
    // Exactly one char difference optimization:
        (l1 == l2 && prefix + postfix + 1 == l1) ? 1 : //
        // Default case, compute
            levenshteinDistance(o1, o2, prefix, postfix);
  }

  /**
   * Compute the length of the prefix.
   * 
   * @param o1 First string
   * @param o2 Second string
   * @return Prefix length
   */
  private static int prefixLen(String o1, String o2) {
    final int l1 = o1.length(), l2 = o2.length(), l = l1 < l2 ? l1 : l2;
    int prefix = 0;
    while(prefix < l && (o1.charAt(prefix) == o2.charAt(prefix))) {
      prefix++;
    }
    return prefix;
  }

  /**
   * Compute the postfix length.
   * 
   * @param o1 First object
   * @param o2 Second object
   * @param prefix Known prefix length
   * @return Postfix length
   */
  private static int postfixLen(String o1, String o2, int prefix) {
    int postfix = 0;
    int p1 = o1.length(), p2 = o2.length();
    while(p1 > prefix && p2 > prefix && (o1.charAt(--p1) == o2.charAt(--p2))) {
      ++postfix;
    }
    return postfix;
  }

  /**
   * Compute the Levenshtein distance, except for prefix and postfix.
   * 
   * @param o1 First object
   * @param o2 Second object
   * @param prefix Prefix length
   * @param postfix Postfix length
   * @return Levenshtein distance
   */
  public static int levenshteinDistance(String o1, String o2, int prefix, int postfix) {
    final int l1 = o1.length(), l2 = o2.length();
    // Buffer, interleaved. Even and odd values are our rows.
    int[] buf = new int[(l2 + 1 - (prefix + postfix)) << 1];
    // Initial "row", on even positions
    for(int j = 0; j < buf.length; j += 2) {
      buf[j] = j >> 1;
    }
    int inter = 1; // Interleaving offset
    for(int i = prefix, e1 = l1 - postfix; i < e1; i++, inter ^= 1) {
      final char chr = o1.charAt(i);
      buf[inter] = i + 1 - prefix; // First entry
      for(int c = 2 + inter, p = 3 - inter, j = prefix; c < buf.length; c += 2, p += 2) {
        buf[c] = min(buf[p] + 1, buf[c - 2] + 1, buf[p - 2] + ((chr == o2.charAt(j++)) ? 0 : 1));
      }
    }
    return buf[buf.length - 2 + (inter ^ 1)];
  }

  /**
   * Three-way integer minimum.
   * 
   * @param a First value
   * @param b Second value
   * @param c Third value.
   * @return Minimum
   */
  private static int min(int a, int b, int c) {
    return a <= b ? (a <= c ? a : c) : (b <= c ? b : c);
  }

  @Override
  public SimpleTypeInformation<? super String> getInputTypeRestriction() {
    return TYPE;
  }

  @Override
  public boolean isMetric() {
    return true;
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
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LevenshteinDistanceFunction makeInstance() {
      return STATIC_SENSITIVE;
    }
  }
}
