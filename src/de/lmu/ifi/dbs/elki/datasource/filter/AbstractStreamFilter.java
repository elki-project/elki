package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.datasource.bundle.BundleStreamSource;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.StreamFromBundle;

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
 * Abstract base class for streaming filters.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractStreamFilter implements StreamFilter {
  /**
   * Data source
   */
  protected BundleStreamSource source = null;

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    init(new StreamFromBundle(objects));
    return MultipleObjectsBundle.fromStream(this);
  }

  @Override
  public void init(BundleStreamSource source) {
    this.source = source;
  }
}
