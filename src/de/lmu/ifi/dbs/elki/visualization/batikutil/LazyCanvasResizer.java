package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Class to lazily process canvas resize events by applying a threshold.
 *  
 * @author Erich Schubert
 *
 */
public abstract class LazyCanvasResizer extends ComponentAdapter {
  /**
   * Default threshold for resizing.
   */
  public static final double DEFAULT_THRESHOLD = 0.05;

  /**
   * Active threshold
   */
  double threshold;
  
  /**
   * Last ratio of the Canvas applied
   */
  double activeRatio;
  
  /**
   * Component the ratio applies to.
   */
  Component component;

  /**
   * Full constructor.
   * 
   * @param component Component to track
   * @param threshold Threshold
   */
  public LazyCanvasResizer(Component component, double threshold) {
    super();
    this.threshold = threshold;
    this.component = component;
    this.activeRatio = getCurrentRatio();
  }

  /**
   * Simplified constructor using the default threshold {@link #DEFAULT_THRESHOLD}
   * 
   * @param component Component to track.
   */
  public LazyCanvasResizer(Component component) {
    this(component, DEFAULT_THRESHOLD);
  }

  /**
   * React to a component resize event.
   */
  @Override
  public void componentResized(ComponentEvent e) {
    if (e.getComponent() == component) {
      double newRatio = getCurrentRatio();
      if (Math.abs(newRatio - activeRatio) > threshold) {
        activeRatio = newRatio;
        executeResize(newRatio);
      }
    }
  }

  /**
   * Get the components current ratio.
   * 
   * @return Current ratio.
   */
  public final double getCurrentRatio() {
    return (double) component.getWidth() / (double) component.getHeight();
  }
  
  /**
   * Callback function that needs to be overridden with actual implementations.
   * 
   * @param newratio New ratio to apply.
   */
  public abstract void executeResize(double newratio);

  /**
   * Get the components last applied ratio.
   * 
   * @return Last applied ratio.
   */
  public double getActiveRatio() {
    return activeRatio;
  }
}
