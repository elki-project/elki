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
 * Direct channel connecting two mappers.
 * 
 * @author Erich Schubert
 */
public class SharedDouble implements SharedVariable<Double> {
  @Override
  public Instance instantiate(MapExecutor mapper) {
    Instance instance = mapper.getShared(this, Instance.class);
    if(instance == null) {
      instance = new Instance();
      mapper.addShared(this, instance);
    }
    return instance;
  }

  /**
   * Instance for a sub-channel.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements SharedVariable.Instance<Double> {
    /**
     * Cache for last data consumed/produced
     */
    protected double data = Double.NaN;

    @Deprecated
    @Override
    public Double get() {
      return data;
    }

    @Deprecated
    @Override
    public void set(Double data) {
      this.data = data;
    }

    public double doubleValue() {
      return data;
    }

    public void set(double data) {
      this.data = data;
    }
  }
}