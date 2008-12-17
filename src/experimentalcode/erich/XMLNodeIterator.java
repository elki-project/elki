package experimentalcode.erich;

import java.util.Iterator;

import org.w3c.dom.Node;

/**
 * Simple adapter class to iterate over a DOM tree nodes children.
 * 
 * @author Erich Schubert
 *
 */
public final class XMLNodeIterator implements Iterable<Node>, Iterator<Node> {
  /**
   * Store the next node
   */
  private Node next;

  /**
   * Constructor with first element to iterate over.
   * 
   * @param first first child of parent node.
   */
  public XMLNodeIterator(Node first) {
    super();
    this.next = first;
  }

  /**
   * Check if the next node is defined. 
   */
  @Override
  public boolean hasNext() {
    return (next != null);
  }

  /**
   * Return next and advance iterator.
   */
  @Override
  public Node next() {
    Node cur = next;
    next = next.getNextSibling();
    return cur;
  }

  /**
   * Removal: unsupported operation.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException("XMLNodeIterator doesn't support remove");
  }

  /**
   * Iterable interface adapter - clone.
   */
  @Override
  public Iterator<Node> iterator() {
    return new XMLNodeIterator(this.next);
  }  
}
