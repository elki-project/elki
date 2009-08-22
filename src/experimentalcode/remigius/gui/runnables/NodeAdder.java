package experimentalcode.remigius.gui.runnables;

import org.w3c.dom.Element;

public class NodeAdder implements Runnable {

  private Element parent;
  private Element child;
  
  public NodeAdder(Element parent, Element child){
    this.parent = parent;
    this.child = child;
  }
  
  @Override
  public void run() {
    parent.appendChild(child);
  }
}
