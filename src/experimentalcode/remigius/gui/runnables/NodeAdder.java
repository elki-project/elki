package experimentalcode.remigius.gui.runnables;

import org.w3c.dom.Element;

/**
 * Runnable wrapper for appending XML-Elements to existing Elements.
 * 
 * @author Remigius Wojdanowski.
 *
 */
public class NodeAdder implements Runnable {
  
  /**
   * Will become the parent of the appended Element. 
   */
  private Element parent;
  
  /**
   * The Element to be appended.
   */
  private Element child;
  
  /**
   * Trivial constructor.
   *  
   * @param parent will become the parent of the appended Element. 
   * @param child the Element to be appended.
   */
  public NodeAdder(Element parent, Element child){
    this.parent = parent;
    this.child = child;
  }
  
  @Override
  public void run() {
    parent.appendChild(child);
  }
}
