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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Direct channel connecting two mappers.
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type
 */
public class DirectConnectChannel<T> implements InputChannel<T>, OutputChannel<T> {
  @Override
  public Instance<T> instantiate(MapExecutor mapper) {
    return new Instance<T>();
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   *
   * @param <T> Data type
   */
  public static class Instance<T> implements OutputChannel.Instance<T>, InputChannel.Instance<T> {
    /**
     * Cache for last data consumed/produced
     */
    protected T data = null;

    @Override
    public T get(DBIDRef id) {
      assert(data != null) : "Data not ready.";
      final T tmp = data;
      data = null;
      return tmp;
    }

    @Override
    public void put(DBIDRef id, T data) {
      assert(data != null) : "Data was not consumed.";
      this.data = data;
    }
  }
}
