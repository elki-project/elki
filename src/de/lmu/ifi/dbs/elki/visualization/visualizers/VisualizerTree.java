package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;

/**
 * Class to map the result parts to visualizations.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type.
 */
public class VisualizerTree<O extends DatabaseObject> implements Iterable<Visualizer> {
  /**
   * Our visualizer context
   */
  VisualizerContext<? extends O> context;

  /**
   * The map from results to their visualizers
   */
  HashMap<AnyResult, java.util.Vector<Visualizer>> map = new HashMap<AnyResult, java.util.Vector<Visualizer>>();

  /**
   * Constructor
   * 
   * @param context Visualizer context to manage
   */
  public VisualizerTree(VisualizerContext<? extends O> context) {
    super();
    this.context = context;
  }

  /**
   * Add a visualizer for the given result.
   * 
   * @param r Result visualized
   * @param v Visualizer for this result
   */
  public void addVisualization(AnyResult r, Visualizer v) {
    if (r == null) {
      LoggingUtil.warning("Visualizer added to null result: "+v, new Throwable());
      return;
    }
    java.util.Vector<Visualizer> vis = map.get(r);
    if(vis == null) {
      vis = new java.util.Vector<Visualizer>(1);
      map.put(r, vis);
    }
    vis.add(v);
  }

  /**
   * Cheap test for emptyness.
   * 
   * @return true when empty
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public Iterator<Visualizer> iterator() {
    return new FlatIterator();
  }

  /**
   * Iterator doing a depth-first traversal of the tree.
   * 
   * @author Erich Schubert
   */
  private class FlatIterator implements Iterator<Visualizer> {
    /**
     * The results iterator.
     */
    private Iterator<? extends AnyResult> resultiter = null;

    /**
     * Current results visualizers
     */
    private Iterator<Visualizer> resultvisiter = null;

    /**
     * The next item to return.
     */
    private Visualizer nextItem = null;

    /**
     * Constructor.
     */
    public FlatIterator() {
      super();
      List<AnyResult> allresults = ResultUtil.filterResults(context.getResult(), AnyResult.class);
      this.resultiter = allresults.iterator();
      updateNext();
    }

    /**
     * Update the iterator to point to the next element.
     */
    private void updateNext() {
      nextItem = null;
      // try within the current result
      if(resultvisiter != null && resultvisiter.hasNext()) {
        nextItem = resultvisiter.next();
        return;
      }
      if(resultiter != null && resultiter.hasNext()) {
        // advance to next result, retry.
        final Collection<Visualizer> childvis = map.get(resultiter.next());
        if (childvis != null && childvis.size() > 0) {
          resultvisiter = childvis.iterator();
        } else {
          resultvisiter = null;
        }
        updateNext();
        return;
      }
      // This means we have failed!
    }

    @Override
    public boolean hasNext() {
      return (nextItem != null);
    }

    @Override
    public Visualizer next() {
      Visualizer ret = nextItem;
      updateNext();
      return ret;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Removals are not supported.");
    }
  }

  /**
   * Get the visualizers for a particular result.
   * 
   * @param r Result
   * @return Visualizers
   */
  public List<Visualizer> getVisualizers(AnyResult r) {
    return map.get(r);
  }
}