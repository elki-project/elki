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
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;

/**
 * Variable to share between different mappers (within one thread only!)
 * 
 * @author Erich Schubert
 * 
 * @param <T> Data type
 */
public class SharedObject<T> implements SharedVariable<T> {
  @Override
  public Instance<T> instantiate(MapExecutor mapper) {
    final Class<Instance<T>> cls = ClassGenericsUtil.uglyCastIntoSubclass(Instance.class);
    Instance<T> instance = mapper.getShared(this, cls);
    if(instance == null) {
      instance = new Instance<T>();
      mapper.addShared(this, instance);
    }
    return instance;
  }

  /**
   * Instance for a particular thread.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Data type
   */
  public static class Instance<T> implements SharedVariable.Instance<T> {
    /**
     * Cache for last data consumed/produced
     */
    protected T data = null;

    @Override
    public T get() {
      return data;
    }

    @Override
    public void set(T data) {
      this.data = data;
    }
  }
}