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
package de.lmu.ifi.dbs.elki.parallel.variables;

/**
 * Direct channel connecting two processors.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - SharedInteger.Instance
 */
public class SharedInteger implements SharedVariable<SharedInteger.Instance> {
  @Override
  public Instance instantiate() {
    return new Instance();
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements SharedVariable.Instance<Integer> {
    /**
     * Cache for last data consumed/produced
     */
    private int data = 0xDEADBEEF;

    /**
     * @deprecated use {@link #intValue}!
     */
    @Deprecated
    @Override
    public Integer get() {
      return data;
    }

    /**
     * @deprecated use {@link #set(int)}!
     */
    @Deprecated
    @Override
    public void set(Integer data) {
      this.data = data;
    }

    /**
     * Get the variables value.
     * 
     * @return Integer value
     */
    public int intValue() {
      return data;
    }

    /**
     * Set the variables value.
     * 
     * @param data New value
     */
    public void set(int data) {
      this.data = data;
    }
  }
}
