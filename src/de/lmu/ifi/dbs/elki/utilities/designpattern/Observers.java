package de.lmu.ifi.dbs.elki.utilities.designpattern;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
 * Class to manage the observers of an instance.
 * 
 * Design note: to avoid reference cycles, this object does not keep track of its owner.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype delegate
 * @apiviz.has Observer
 */
public class Observers<T> extends java.util.Vector<Observer<? super T>> {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;
  
  /**
   * Constructor.
   */
  public Observers() {
    super();
  }
  
  /**
   * Add an observer to the object.
   * 
   * @param o Observer to add
   */
  public void addObserver(Observer<? super T> o) {
    super.add(o);
  }

  /**
   * Remove an observer from the object.
   * 
   * @param o Observer to remove
   */
  public void removeObserver(Observer<? super T> o) {
    super.remove(o);
  }
  
  /**
   * Notify the observers of the changed object.
   * 
   * @param owner Owner of the Observers list - changed instance
   */
  public void notifyObservers(T owner) {
    for (Observer<? super T> observer : this) {
      observer.update(owner);
    }
  }
}