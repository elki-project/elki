package experimentalcode.shared.index.xtree.util;
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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * History of all splits ever occurred in a Node.
 * 
 * @author Marisa Thoma
 */
public final class SplitHistory implements Serializable {

  private static final long serialVersionUID = -340123050472355300L;

  /**
   * BitSet with a bit for each dimension
   */
  private LargeProperties dimBits;

  /**
   * Initialize a new split history instance of dimension <code>dim</code>.
   * 
   * @param dim
   */
  public SplitHistory(int dim) {
    dimBits = new LargeProperties(dim);
  }

  public SplitHistory(LargeProperties lp) throws CloneNotSupportedException {
    this.dimBits = (LargeProperties) lp.clone();
  }

  public SplitHistory(LargeProperties lp, boolean clone) {
    if(clone)
      try {
        this.dimBits = (LargeProperties) lp.clone();
      }
      catch(CloneNotSupportedException e) {
        throw new AbortException("This cannot not have happened", e);
      }
    else
      this.dimBits = lp;
  }

  /**
   * Set dimension <code>dimension</code> to <code>true</code>
   * 
   * @param dimension
   */
  public void setDim(int dimension) {
    dimBits.setProperty(dimension);
  }

  /**
   * Get the common split dimensions from a list of split histories.
   * 
   * @param splitHistories
   * @return list of split dimensions
   */
  public static Collection<Integer> getCommonDimensions(Collection<SplitHistory> splitHistories) {
    Collection<Integer> common = new Stack<Integer>();
    Iterator<SplitHistory> it = splitHistories.iterator();
    LargeProperties checkSet = null;
    try {
      checkSet = (LargeProperties) (it.next().dimBits).clone();
    }
    catch(CloneNotSupportedException ex) {
      Logger.getLogger(SplitHistory.class.getName()).log(Level.SEVERE, null, ex);
    }
    while(it.hasNext()) {
      SplitHistory sh = it.next();
      checkSet.intersect(sh.dimBits);
    }
    int i = 0;
    for(Iterator<Boolean> bIt = checkSet.iterator(); bIt.hasNext(); i++) {
      if(bIt.next())
        common.add(i);
    }
    return common;
  }

  /**
   * See {@link LargeProperties#toString()} for reference on the returned
   * string's format
   */
  @Override
  public String toString() {
    return dimBits.toString();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new SplitHistory(this.dimBits);
  }

  /**
   * Writes the split history to the specified output stream.
   * 
   * @param out the stream to write the history to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    dimBits.writeExternal(out);
  }

  /**
   * Reads the split history from the specified input stream.
   * 
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being restored
   *         cannot be found.
   */
  public static SplitHistory readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    return new SplitHistory(LargeProperties.readExternal(in), false);
  }
  
  public boolean isEmpty() {
    return dimBits.isEmpty();
  }
}
