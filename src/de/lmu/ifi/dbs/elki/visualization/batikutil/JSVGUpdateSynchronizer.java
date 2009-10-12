package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.lang.ref.WeakReference;

import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.bridge.UpdateManagerAdapter;
import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.swing.svg.JSVGComponent;

import de.lmu.ifi.dbs.elki.visualization.svg.UpdateRunner;
import de.lmu.ifi.dbs.elki.visualization.svg.UpdateSynchronizer;

/**
 * This class is used to synchronize SVG updates with an JSVG canvas.
 * 
 * @author Erich Schubert
 */
public class JSVGUpdateSynchronizer implements UpdateSynchronizer {
  /**
   * A weak reference to the component the plot is in.
   */
  private WeakReference<JSVGComponent> cref = null;

  /**
   * The UpdateRunner we are put into
   */
  private WeakReference<UpdateRunner> updaterunner = null;

  /**
   * Adapter to track component changes
   */
  private UMAdapter umadapter = new UMAdapter();

  /**
   * The current Runnable scheduled
   */
  private WeakReference<JSVGSynchronizedRunner> syncrunner = null;

  /**
   * Create an updateSynchronizer for the given component.
   * 
   * @param component Component to manage updates on.
   */
  public JSVGUpdateSynchronizer(JSVGComponent component) {
    assert(component != null);
    
    this.cref = new WeakReference<JSVGComponent>(component);
    // Hook into UpdateManager creation.
    component.addUpdateManagerListener(umadapter);
    // makeRunnerIfNeeded();
  }

  @Override
  public void activate() {
    makeRunnerIfNeeded();
  }
  
  /**
   * Join the runnable queue of a component.
   */
  protected synchronized void makeRunnerIfNeeded() {
    // Nothing to do if not connected to a plot
    UpdateRunner ur = getUpdateRunner();
    if(ur == null) {
      return;
    }
    // Nothing to do if update queue is empty
    synchronized(ur) {
      if(ur.isEmpty()) {
        return;
      }
    }
    // We need a component
    JSVGComponent component = getComponent();
    if(component == null) {
      return;
    }
    // We only need a new runner when we don't have one in the queue yet!
    JSVGSynchronizedRunner cur = getSynchronizedRunner();
    if(cur != null && cur.getAttachedTo() == component) {
      return;
    }
    // Really create a new runner.
    synchronized(component) {
      UpdateManager um = component.getUpdateManager();
      if(um != null) {
        synchronized(um) {
          if(um.isRunning()) {
            JSVGSynchronizedRunner newrunner = new JSVGSynchronizedRunner(component);
            setSynchronizedRunner(newrunner);
            um.getUpdateRunnableQueue().invokeLater(newrunner);
            return;
          }
        }
      }
    }
  }

  /**
   * Read the component link.
   * 
   * @return Component tracked
   */
  protected JSVGComponent getComponent() {
    if(this.cref == null) {
      return null;
    }
    return this.cref.get();
  }

  @Override
  public void setUpdateRunner(UpdateRunner updateRunner) {
    this.updaterunner = new WeakReference<UpdateRunner>(updateRunner);
  }

  /**
   * Return the current update runner we are synchronizing or null.
   * 
   * @return update runner
   */
  protected UpdateRunner getUpdateRunner() {
    return updaterunner.get();
  }

  /**
   * Set the current update runner
   * 
   * @param newrunner
   */
  protected void setSynchronizedRunner(JSVGSynchronizedRunner newrunner) {
    syncrunner = new WeakReference<JSVGSynchronizedRunner>(newrunner);
  }

  /**
   * Get the current update runner.
   * 
   * @return current update runner
   */
  protected JSVGSynchronizedRunner getSynchronizedRunner() {
    if(syncrunner == null) {
      return null;
    }
    return syncrunner.get();
  }
  
  /**
   * Update Runner that will be placed in the Components UpdateManagers queue.
   * 
   * @author Erich Schubert
   * 
   */
  private class JSVGSynchronizedRunner implements Runnable {
    /**
     * Component we were attached to.
     */
    private final JSVGComponent attachedTo;

    /**
     * Constructor
     */
    protected JSVGSynchronizedRunner(JSVGComponent attachedTo) {
      this.attachedTo = attachedTo;
    }

    @Override
    public void run() {
      // Assert that we're still "the one"
      if(this.attachedTo == getComponent() && this == getSynchronizedRunner()) {
        // Remove ourself. We're finished!
        setSynchronizedRunner(null);
        // Now invoke any pending updates in the actual update queue.
        UpdateRunner ur = getUpdateRunner();
        if(ur != null) {
          ur.runQueue();
        }
      }
    }

    /**
     * Find out which component we are waiting for.
     * 
     * @return component
     */
    public JSVGComponent getAttachedTo() {
      return attachedTo;
    }
  }

  /**
   * Adapter that will track the component for UpdateManager changes.
   * 
   * @author Erich Schubert
   */
  private class UMAdapter extends UpdateManagerAdapter {
    /**
     * Constructor. Protected to allow construction above.
     */
    protected UMAdapter() {
      // nothing to do.
    }

    /**
     * React to an update manager becoming available.
     */
    @Override
    public void managerStarted(@SuppressWarnings("unused") UpdateManagerEvent e) {
      JSVGComponent component = getComponent();
      if(component != null) {
        makeRunnerIfNeeded();
      }
    }

    @Override
    public void managerStopped(@SuppressWarnings("unused") UpdateManagerEvent e) {
      // this probably means our runner will not be run!
      setSynchronizedRunner(null);
    }
  }
}
