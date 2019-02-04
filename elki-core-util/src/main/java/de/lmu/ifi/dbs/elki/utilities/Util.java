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
package de.lmu.ifi.dbs.elki.utilities;

/**
 * This class collects various static helper methods.
 * 
 * For helper methods related to special application fields see other utilities
 * classes.
 * 
 * @author Erich Schubert
 * @since 0.1
 */
public final class Util {
  /**
   * Prime number used in hash code computation.
   */
  private static final long HASHPRIME = 2654435761L;

  /**
   * Fake constructor: do not instantiate.
   */
  private Util() {
    // Do not instantiate.
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash1 First hashcode to mix
   * @param hash2 Second hashcode to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int hash1, int hash2) {
    return (int) (hash1 * HASHPRIME + hash2);
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash1 First hashcode to mix
   * @param hash2 Second hashcode to mix
   * @param hash3 Third hashcode to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int hash1, int hash2, int hash3) {
    long result = hash1 * HASHPRIME + hash2;
    return (int) (result * HASHPRIME + hash3);
  }

  /**
   * Mix multiple hashcodes into one.
   * 
   * @param hash Hashcodes to mix
   * @return Mixed hash code
   */
  public static int mixHashCodes(int... hash) {
    if(hash.length == 0) {
      return 0;
    }
    long result = hash[0];
    for(int i = 1; i < hash.length; i++) {
      result = result * HASHPRIME + hash[i];
    }
    return (int) result;
  }
}
