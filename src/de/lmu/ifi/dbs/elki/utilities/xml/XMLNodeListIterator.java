package de.lmu.ifi.dbs.elki.utilities.xml;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.lmu.ifi.dbs.elki.utilities.ExceptionMessages;

/**
 * Simple adapter class to iterate over a DOM tree nodes children.
 * 
 * @author Erich Schubert
 *
 */
public final class XMLNodeListIterator implements Iterable<Node>, Iterator<Node> {
  /**
   * Store the next node
   */
  private int index;
  
  /**
   * The {@link NodeList} to iterate over.
   */
  private NodeList nodelist;

  /**
   * Constructor with first element to iterate over.
   * 
   * @param nl NodeList to iterate over.
   */
  public XMLNodeListIterator(NodeList nl) {
    super();
    this.nodelist = nl;
    this.index = 0;
  }

  /**
   * Check if the next node is defined. 
   */
  @Override
  public boolean hasNext() {
    return (this.index < this.nodelist.getLength());
  }

  /**
   * Return next and advance iterator.
   */
  @Override
  public Node next() {
    Node cur = this.nodelist.item(this.index);
    this.index++;
    return cur;
  }

  /**
   * Removal: unsupported operation.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException(this.getClass().getSimpleName()+": "+ExceptionMessages.UNSUPPORTED_REMOVE);
  }

  /**
   * Iterable interface adapter - clone.
   */
  @Override
  public Iterator<Node> iterator() {
    return new XMLNodeListIterator(this.nodelist);
  }  
}
