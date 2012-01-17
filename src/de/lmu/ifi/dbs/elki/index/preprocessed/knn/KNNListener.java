package de.lmu.ifi.dbs.elki.index.preprocessed.knn;

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

import java.util.EventListener;

/**
 * Listener interface invoked when the k nearest neighbors (kNNs) of some
 * objects have been changed due to insertion or removals of objects.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses KNNChangeEvent oneway - -
 */
public interface KNNListener extends EventListener {
  /**
   * Invoked after kNNs have been updated, inserted or removed
   * in some way.
   * 
   * @param e the change event
   */
  public void kNNsChanged(KNNChangeEvent e);

  /**
   * Existing objects have been removed and as a result existing kNNs have been
   * removed and some kNNs have been changed.
   * 
   * @param source the object responsible for the invocation
   * @param removals the ids of the removed kNNs
   * @param updates the ids of kNNs which have been changed due to the removals
   */
  // public void kNNsRemoved(Object source, DBIDs removals, DBIDs updates);
}
