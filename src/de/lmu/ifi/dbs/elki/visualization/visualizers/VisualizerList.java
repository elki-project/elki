package de.lmu.ifi.dbs.elki.visualization.visualizers;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;

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
  protected ArrayList<Visualizer> visualizers;
  
  /**
   * Constructor.
   */
  public VisualizerList() {
    super();
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
    return new VisibleVisualizers();
  }

  /**
   * Class to filter for visible visualizers only.
   * 
   * @author Erich Schubert
   */
  class VisibleVisualizers extends AbstractFilteredIterator<Visualizer> implements IterableIterator<Visualizer> {
    @Override
    protected Iterator<Visualizer> getParentIterator() {
      return visualizers.iterator();
    }

    @Override
    protected boolean testFilter(Visualizer nextobj) {
      return VisualizerUtil.isVisible(nextobj);
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
  class ToolVisualizers extends AbstractFilteredIterator<Visualizer> implements IterableIterator<Visualizer> {
    @Override
    protected Iterator<Visualizer> getParentIterator() {
      return visualizers.iterator();
    }

    @Override
    protected boolean testFilter(Visualizer nextobj) {
      return VisualizerUtil.isTool(nextobj);
    }

    @Override
    public Iterator<Visualizer> iterator() {
      return this;
    }
  }
}