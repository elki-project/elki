package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import de.lmu.ifi.dbs.elki.utilities.datastructures.AbstractFilteredIterator;
import de.lmu.ifi.dbs.elki.utilities.datastructures.IterableIterator;

/**
 * A list to handle visualizations.
 * 
 * @author Erich Schubert
 */
public class VisualizerList extends AbstractCollection<Visualizer> {
  /**
   * The actual data storage.
   */
  private ArrayList<VisualizerTreeItem> visualizers = new ArrayList<VisualizerTreeItem>();

  /**
   * Constructor.
   */
  public VisualizerList() {
    super();
  }

  /**
   * Simple API to register new visualizers.
   * 
   * @param vis visualizer
   * @param groupname Group to register to
   */
  public void register(VisualizerTreeItem vis, String groupname) {
    Collection<VisualizerTreeItem> col = this.visualizers;
    if(groupname != null) {
      Iterator<VisualizerTreeItem> iter = treeIterator();
      while(iter.hasNext()) {
        VisualizerTreeItem item = iter.next();
        if(item instanceof VisualizerGroup && item.getName().equals(groupname)) {
          col = (VisualizerGroup) item;
          break;
        }
      }
    }
    col.add(vis);
  }

  @Override
  public boolean add(Visualizer e) {
    return visualizers.add(e);
  }

  @Override
  public boolean contains(Object o) {
    return visualizers.contains(o);
  }

  @Override
  public boolean remove(Object o) {
    return visualizers.remove(o);
  }

  @Override
  public Iterator<Visualizer> iterator() {
    return new VisualizerIterator();
  }

  /**
   * Get a depth-first tree iterator over all items (including groups)
   * 
   * @return Depth-first tree iterator.
   */
  public Iterator<VisualizerTreeItem> treeIterator() {
    return new TreeIterator(visualizers.iterator());
  }

  /**
   * Get a top-level iterator.
   * 
   * @return top level iterator.
   */
  public Iterator<VisualizerTreeItem> topIterator() {
    return visualizers.iterator();
  }

  @Override
  public int size() {
    return visualizers.size();
  }

  /**
   * Iterate over all tools.
   * 
   * @return Iterator and Iterable
   */
  public IterableIterator<Visualizer> getVisible() {
    return new VisibleVisualizers();
  }

  /**
   * Iterate over all tools.
   * 
   * @return Iterator and Iterable
   */
  public IterableIterator<Visualizer> getTools() {
    return new ToolVisualizers();
  }

  /**
   * Iterator doing a depth-first traversal of the tree.
   * 
   * @author Erich Schubert
   */
  private static class TreeIterator implements Iterator<VisualizerTreeItem> {
    /**
     * The actual iterators.
     */
    private LinkedList<Iterator<VisualizerTreeItem>> iters;

    /**
     * The next item to return.
     */
    private VisualizerTreeItem nextItem = null;

    /**
     * Constructor with an initial iterator.
     * 
     * @param iter Initial iterator
     */
    public TreeIterator(Iterator<VisualizerTreeItem> iter) {
      super();
      this.iters = new LinkedList<Iterator<VisualizerTreeItem>>();
      this.iters.add(iter);
      updateNext();
    }

    /**
     * Update the iterator to point to the next element.
     */
    private void updateNext() {
      nextItem = null;
      while(!iters.isEmpty()) {
        // Pop 'dead' iterators.
        if(!iters.peek().hasNext()) {
          iters.pop();
          continue;
        }
        nextItem = iters.peek().next();
        // Descend into visualizer groups
        if(nextItem instanceof VisualizerGroup) {
          iters.push(((VisualizerGroup) nextItem).iterator());
        }
        return;
      }
    }

    @Override
    public boolean hasNext() {
      return (nextItem != null);
    }

    @Override
    public VisualizerTreeItem next() {
      VisualizerTreeItem ret = nextItem;
      updateNext();
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removals are not supported.");
    }
  }

  /**
   * Class to filter for visualizers only.
   * 
   * @author Erich Schubert
   */
  class VisualizerIterator extends AbstractFilteredIterator<VisualizerTreeItem, Visualizer> implements IterableIterator<Visualizer> {
    @Override
    protected Iterator<VisualizerTreeItem> getParentIterator() {
      return VisualizerList.this.treeIterator();
    }

    @Override
    protected Visualizer testFilter(VisualizerTreeItem nextobj) {
      if(nextobj instanceof Visualizer) {
        return (Visualizer) nextobj;
      }
      else {
        return null;
      }
    }

    @Override
    public Iterator<Visualizer> iterator() {
      return this;
    }
  }

  /**
   * Class to filter for visible visualizers only.
   * 
   * @author Erich Schubert
   */
  class VisibleVisualizers extends AbstractFilteredIterator<VisualizerTreeItem, Visualizer> implements IterableIterator<Visualizer> {
    @Override
    protected Iterator<VisualizerTreeItem> getParentIterator() {
      return VisualizerList.this.treeIterator();
    }

    @Override
    protected Visualizer testFilter(VisualizerTreeItem nextobj) {
      if(nextobj instanceof Visualizer && VisualizerUtil.isVisible((Visualizer) nextobj)) {
        return (Visualizer) nextobj;
      }
      else {
        return null;
      }
    }

    @Override
    public Iterator<Visualizer> iterator() {
      return this;
    }
  }

  /**
   * Class to filter for visible visualizers only.
   * 
   * @author Erich Schubert
   */
  class ToolVisualizers extends AbstractFilteredIterator<VisualizerTreeItem, Visualizer> implements IterableIterator<Visualizer> {
    @Override
    protected Iterator<VisualizerTreeItem> getParentIterator() {
      return VisualizerList.this.treeIterator();
    }

    @Override
    protected Visualizer testFilter(VisualizerTreeItem nextobj) {
      if(nextobj instanceof Visualizer && VisualizerUtil.isTool((Visualizer) nextobj)) {
        return (Visualizer) nextobj;
      }
      else {
        return null;
      }
    }

    @Override
    public Iterator<Visualizer> iterator() {
      return this;
    }
  }
}