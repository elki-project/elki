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
package de.lmu.ifi.dbs.elki.utilities.datastructures.range;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.IntegerArray;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;

/**
 * Parse integer range syntaxes.
 *
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @has - - - IntGenerator
 */
public class ParseIntRanges {
  /**
   * Parse integer ranges, in different syntaxes.
   *
   * <code>
   * 1,2,3,...,10
   * 1,3,,10
   * 1,3,..,10
   * 1,3,...,10
   * 1,+=2,10
   * 1,*=2,16
   * 1,2,3,4,..,10,100
   * 100,1,3,..,10
   * 1,2,..,10,20,..,100,200,..,1000
   * </code>
   *
   * @param str Ranges to parse
   * @return Ranges
   */
  public static IntGenerator parseIntRanges(String str) {
    ArrayList<IntGenerator> generators = new ArrayList<>();
    IntegerArray ints = new IntegerArray();
    int last = Integer.MAX_VALUE; // Last seen integer
    for(int pos = 0, next = -1; pos < str.length(); pos = next + 1) {
      // Find next comma:
      next = nextSep(str, pos);
      // Syntaxes involving ",," or ",..," or ",...," equivalently:
      if(next == pos || (str.charAt(pos) == '.' && //
          ((next - pos == 2 && str.charAt(pos + 1) == '.') //
              || (next - pos == 3 && str.charAt(pos + 1) == '.' && str.charAt(pos + 2) == '.')))) {
        if(ints.size() == 0 || next == str.length()) {
          throw new NumberFormatException("Not a valid integer range.");
        }
        // Shorter (but less tolerant) syntax: 1,..,10
        if(ints.size() == 1) {
          int start = ints.get(0);
          // Hack for: 1,..,10,20,..,100: reuse last value!
          int step = last == Integer.MAX_VALUE ? 1 : start - last;
          // Find next comma:
          next = nextSep(str, pos = next + 1);
          last = ParseUtil.parseIntBase10(str, pos, next); // Update last!
          generators.add(new LinearIntGenerator(start, step, last));
          ints.clear();
          continue;
        }
        // Longer syntax: 10,15,..,35 or 10,15,...,35 or 10,15,,35
        assert (ints.size() > 1);
        int start = ints.get(ints.size() - 2); // F
        int step = ints.get(ints.size() - 1) - start;
        ints.remove(ints.size() - 2, 2);
        // Remove additional static entries if step size is consistent:
        while(!ints.isEmpty() && ints.get(ints.size() - 1) == start - step) {
          start -= step;
          ints.remove(ints.size() - 1, 1);
        }
        // Leading elements not part of this sequence:
        if(!ints.isEmpty()) {
          generators.add(new StaticIntGenerator(ints.toArray()));
        }
        // Find next comma:
        next = nextSep(str, pos = next + 1);
        last = ParseUtil.parseIntBase10(str, pos, next); // Update last!
        generators.add(new LinearIntGenerator(start, step, last));
        ints.clear();
      }
      // Explicit syntax: 0,+=4,16
      else if(next - pos > 2 && str.charAt(pos) == '+' && str.charAt(pos + 1) == '=') {
        if(ints.size() == 0 || next == str.length()) {
          throw new NumberFormatException("Not a valid integer range.");
        }
        int start = ints.get(ints.size() - 1); //
        int step = ParseUtil.parseIntBase10(str, pos + 2, next);
        ints.remove(ints.size() - 1, 1);
        // Remove additional static entries if step size is consistent:
        while(!ints.isEmpty() && ints.get(ints.size() - 1) == start - step) {
          start -= step;
          ints.remove(ints.size() - 1, 1);
        }
        // Leading elements not part of this sequence:
        if(!ints.isEmpty()) {
          generators.add(new StaticIntGenerator(ints.toArray()));
        }
        // Find next comma:
        next = nextSep(str, pos = next + 1);
        last = ParseUtil.parseIntBase10(str, pos, next); // Update last!
        generators.add(new LinearIntGenerator(start, step, last));
        ints.clear();
      }
      // Explicit syntax: 0,+=4,16
      else if(next - pos > 2 && str.charAt(pos) == '*' && str.charAt(pos + 1) == '=') {
        if(ints.size() == 0 || next == str.length()) {
          throw new NumberFormatException("Not a valid integer range.");
        }
        int start = ints.get(ints.size() - 1); //
        int factor = ParseUtil.parseIntBase10(str, pos + 2, next);
        ints.remove(ints.size() - 1, 1);
        // Remove additional static entries if step size is consistent:
        while(ints.size() > 2 && ints.get(ints.size() - 1) * factor == start) {
          start = ints.get(ints.size() - 1);
          ints.remove(ints.size() - 1, 1);
        }
        // Leading elements not part of this sequence:
        if(!ints.isEmpty()) {
          generators.add(new StaticIntGenerator(ints.toArray()));
        }
        // Find next comma:
        next = nextSep(str, pos = next + 1);
        last = ParseUtil.parseIntBase10(str, pos, next); // Update last!
        generators.add(new ExponentialIntGenerator(start, factor, last));
        ints.clear();
      }
      else {
        // Default case: an integer
        ints.add(ParseUtil.parseIntBase10(str, pos, next));
      }
    }
    // Trailing static values.
    if(!ints.isEmpty()) {
      generators.add(new StaticIntGenerator(ints.toArray()));
    }
    return generators.size() == 1 ? generators.get(0) : new CombinedIntGenerator(generators);
  }

  /**
   * Find the next separator.
   *
   * TODO: allow other separators, too?
   *
   * @param str String
   * @param start Starting position
   * @return Next separator position, or end of string.
   */
  private static int nextSep(String str, int start) {
    int next = str.indexOf(',', start);
    return next == -1 ? str.length() : next;
  }

}
