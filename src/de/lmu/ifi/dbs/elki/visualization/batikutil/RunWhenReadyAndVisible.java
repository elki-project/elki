package de.lmu.ifi.dbs.elki.visualization.batikutil;

import org.apache.batik.bridge.UpdateManager;
import org.apache.batik.bridge.UpdateManagerAdapter;
import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.swing.svg.AbstractJSVGComponent;

/**
 * Wait for the object to get an updateManager, then run.
 * If the updateManager already exists, schedule a run via the runnable queue.
 * 
 * @author Erich Schubert
 * 
 */
public abstract class RunWhenReadyAndVisible extends UpdateManagerAdapter implements Runnable {
  /**
   * The component we are attached to, and that we might need to unregister with.
   */
  private AbstractJSVGComponent component = null;

  /**
   * Try to either hook into the UpdateRunnableQueue, or wait for an updateManager to become
   * available.
   */
  public RunWhenReadyAndVisible() {
    super();
  }

  /**
   * Hook to the given component, schedule update.
   * 
   * @param component The component to hook to.
   */
  public final void hook(AbstractJSVGComponent component) {
    this.component = component;
    synchronized(component) {
      UpdateManager um = component.getUpdateManager();
      if(um != null) {
        //System.err.println("Using runnable queue immediately.");
        synchronized(um) {
          if (um.isRunning()) {
            um.getUpdateRunnableQueue().invokeLater(this);
            return;
          }
        }
      }
      //System.err.println("No update manager - waiting for it to become available.");
      component.addUpdateManagerListener(this);
    }
  }

  /**
   * React to an update manager becoming available.
   */
  @Override
  public void managerStarted(@SuppressWarnings("unused") UpdateManagerEvent e) {
    //System.err.println("New update manager. Wake up!");
    component.getUpdateManager().getUpdateRunnableQueue().invokeLater(this);
    component.removeUpdateManagerListener(this);
  }

  /**
   * Run method invoked from the updateManager.
   */
  public abstract void run();
}
