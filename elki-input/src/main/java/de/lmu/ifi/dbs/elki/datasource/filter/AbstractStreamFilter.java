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
package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;

/**
 * Abstract base class for streaming filters.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public abstract class AbstractStreamFilter implements StreamFilter {
  /**
   * Data source
   */
  protected BundleStreamSource source = null;

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    return init(objects.asStream()).asMultipleObjectsBundle();
  }

  @Override
  public BundleStreamSource init(BundleStreamSource source) {
    this.source = source;
    return this;
  }

  @Override
  public boolean hasDBIDs() {
    return source.hasDBIDs();
  }

  @Override
  public boolean assignDBID(DBIDVar var) {
    return source.assignDBID(var);
  }

  @Override
  public MultipleObjectsBundle asMultipleObjectsBundle() {
    return MultipleObjectsBundle.fromStream(this);
  }

  @Override
  public String toString() {
    return getClass().getName();
  }
}
