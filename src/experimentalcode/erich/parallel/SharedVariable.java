package experimentalcode.erich.parallel;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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


/**
 * Shared variables storing a particular type.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Payload type
 */
public interface SharedVariable<T> {
  /**
   * Instantiate for an execution thread.
   * 
   * @param mapper Mapper to instantiate for
   * @return Instance
   */
  public Instance<T> instantiate(MapExecutor mapper);

  /**
   * Instance for a single execution thread.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Payload type
   */
  public interface Instance<T> {
    /**
     * Get the current value
     * 
     * @return Value
     */
    public T get();

    /**
     * Set a new value
     * 
     * @param data Setter
     */
    public void set(T data);
  }
}