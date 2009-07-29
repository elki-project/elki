package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.bridge.UpdateManagerAdapter;
import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.swing.svg.JSVGComponent;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;

public class UpdateRunner {
  /**
   * The queue of pending updates
   */
  final private Queue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

  /**
   * A reference to the component the plot is in.
   */
  protected WeakReference<JSVGComponent> cref = null;

  /**
   * Adapter to track component changes
   */
  private UMAdapter umadapter = new UMAdapter();

  /**
   * The current Runnable scheduled
   */
  private WeakReference<UMUpdateRunner> umrunner = null;

  /**
   * Construct a new update handler
   */
  public UpdateRunner() {
    // nothing to do here.
  }

  /**
   * Attach to a new component. Use {@code null} to detach.
   * 
   * @param newcomponent New component to attach to. May be {@code null} to
   *        detach.
   */
  public void attachComponent(JSVGComponent newcomponent) {
    if(this.getComponent() != null) {
      this.getComponent().removeUpdateManagerListener(umadapter);
    }
    this.setComponent(newcomponent);
    if(this.getComponent() != null) {
      this.getComponent().addUpdateManagerListener(umadapter);
      makeRunnerIfNeeded();
    }
    else {
      // if we are not attached, we can run all the pending updates!
      runQueue();
    }
  }

  /**
   * Detach from any component we are attached to.
   */
  public void detachComponent() {
    attachComponent(null);
  }

  /**
   * Add a new update to run at any appropriate time.
   * 
   * @param r New runnable to perform the update
   */
  public synchronized void invokeLater(Runnable r) {
    if (getComponent() != null) {
      queue.add(r);
      makeRunnerIfNeeded();
    } else {
      // TODO: when we introduce exception handlers to the API, catch here!
      r.run();
    }
  }

  /**
   * Run the processing queue now.
   */
  protected void runQueue() {
    while(!queue.isEmpty()) {
      Runnable r = queue.poll();
      try {
        r.run();
      }
      catch(Exception e) {
        // Alternatively, we could allow the specification of exception handlers
        // for each runnable in the API. For now we'll just log.
        // TODO: handle exceptions here better!
        LoggingUtil.exception(e);
      }
    }
  }

  /**
   * Clear queue. For shutdown!
   */
  public void clear() {
    queue.clear();
  }

  /**
   * Join the runnable queue of a component.
   */
  protected synchronized void makeRunnerIfNeeded() {
    if (queue.isEmpty()) {
      return;
    }
    // We need a component
    JSVGComponent component = getComponent();
    if(component == null) {
      return;
    }
    // We only need a new runner when we don't have one in the queue yet!
    UMUpdateRunner cur = getRunner();
    if (cur != null && cur.getAttachedTo() == component) {
      return;
    }
    // Really create a new runner.
    synchronized(component) {
      UpdateManager um = component.getUpdateManager();
      if(um != null) {
        synchronized(um) {
          if(um.isRunning()) {
            UMUpdateRunner newrunner = new UMUpdateRunner(component);
            setRunner(newrunner);
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

  /**
   * Set the component link.
   * 
   * @param newcomponent Component to track
   */
  private void setComponent(JSVGComponent newcomponent) {
    if(newcomponent == null) {
      this.cref = null;
    }
    this.cref = new WeakReference<JSVGComponent>(newcomponent);
  }

  /**
   * Set the current update runner
   * 
   * @param newrunner
   */
  protected void setRunner(UMUpdateRunner newrunner) {
    umrunner = new WeakReference<UMUpdateRunner>(newrunner);
  }

  /**
   * Get the current update runner.
   * 
   * @return current update runner
   */
  protected UMUpdateRunner getRunner() {
    if(umrunner == null) {
      return null;
    }
    return umrunner.get();
  }

  /**
   * Update Runner that will be placed in the Components UpdateManagers queue.
   * 
   * @author Erich Schubert
   * 
   */
  private class UMUpdateRunner implements Runnable {
    /**
     * Component we were attached to.
     */
    private final JSVGComponent attachedTo;

    /**
     * Constructor
     */
    protected UMUpdateRunner(JSVGComponent attachedTo) {
      this.attachedTo = attachedTo;
    }

    @Override
    public void run() {
      if(this.attachedTo == getComponent() && this == getRunner()) {
        // we're done.
        setRunner(null);
        runQueue(); 
      }
    }

    /**
     * Find out which component we are waiting for.
     * 
     * @return
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
      setRunner(null);
    }    
  }
}
