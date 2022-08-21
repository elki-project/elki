/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.ids;

/**
 * Iterator over Double+DBID pairs results.
 * <p>
 * There is no getter for the DBID, as this implements
 * {@link elki.database.ids.DBIDRef}.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 */
public interface DoubleDBIDListIter extends DoubleDBIDIter, DBIDArrayIter {
  /**
   * Static empty iterator.
   */
  DoubleDBIDListIter EMPTY = new DoubleDBIDListIter() {
    @Override
    public int internalGetIndex() {
      return -1;
    }

    @Override
    public boolean valid() {
      return false;
    }

    @Override
    public int getOffset() {
      return -1;
    }

    @Override
    public double doubleValue() {
      return Double.NaN;
    }

    @Override
    public DoubleDBIDListIter advance() {
      return this;
    }

    @Override
    public DoubleDBIDListIter advance(int count) {
      return this;
    }

    @Override
    public DoubleDBIDListIter retract() {
      return this;
    }

    @Override
    public DoubleDBIDListIter seek(int off) {
      return this;
    }
  };

  @Override
  DoubleDBIDListIter advance();

  @Override
  DoubleDBIDListIter advance(int count);

  @Override
  DoubleDBIDListIter retract();

  @Override
  DoubleDBIDListIter seek(int off);
}
