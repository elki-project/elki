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
 * Shared variables storing a particular type.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - SharedVariable.Instance
 * 
 * @param <I> Instance type
 */
public interface SharedVariable<I extends SharedVariable.Instance<?>> {
  /**
   * Instantiate for an execution thread.
   * 
   * @return new Instance
   */
  I instantiate();

  /**
   * Instance for a single execution thread.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Payload type
   */
  interface Instance<T> {
    /**
     * Get the current value
     * 
     * @return Value
     */
    T get();

    /**
     * Set a new value
     * 
     * @param data Setter
     */
    void set(T data);
  }
}