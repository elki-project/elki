package de.lmu.ifi.dbs.elki.visualization.batikutil;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

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
 * 
 * @apiviz.uses UpdateRunner
 */
class JSVGUpdateSynchronizer implements UpdateSynchronizer {
  /**
   * A weak reference to the component the plot is in.
   */
  private final WeakReference<JSVGComponent> cref;

  /**
   * The UpdateRunner we are put into
   */
  private List<WeakReference<UpdateRunner>> updaterunner = new ArrayList<WeakReference<UpdateRunner>>();

  /**
   * Adapter to track component changes
   */
  private final UMAdapter umadapter = new UMAdapter();

  /**
   * The current Runnable scheduled
   */
  private WeakReference<JSVGSynchronizedRunner> syncrunner = null;

  /**
   * Create an updateSynchronizer for the given component.
   * 
   * @param component Component to manage updates on.
   */
  protected JSVGUpdateSynchronizer(JSVGComponent component) {
    assert (component != null);

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
  protected void makeRunnerIfNeeded() {
    // Nothing to do if not connected to a plot
    if(updaterunner.size() == 0) {
      return;
    }
    // we don't need to make a SVG runner when there are no pending updates.
    boolean stop = true;
    for(WeakReference<UpdateRunner> wur : updaterunner) {
      UpdateRunner ur = wur.get();
      if(ur != null && !ur.isEmpty()) {
        stop = false;
      }
    }
    if(stop) {
      return;
    }
    // We only need a new runner when we don't have one in the queue yet!
    if(getSynchronizedRunner() != null) {
      return;
    }
    // We need a component
    JSVGComponent component = this.cref.get();
    if(component == null) {
      return;
    }
    // Really create a new runner.
    synchronized(this) {
      synchronized(component) {
        UpdateManager um = component.getUpdateManager();
        if(um != null) {
          synchronized(um) {
            if(um.isRunning()) {
              JSVGSynchronizedRunner newrunner = new JSVGSynchronizedRunner();
              setSynchronizedRunner(newrunner);
              um.getUpdateRunnableQueue().invokeLater(newrunner);
              return;
            }
          }
        }
      }
    }
  }

  @Override
  public void addUpdateRunner(UpdateRunner updateRunner) {
    updaterunner.add(new WeakReference<UpdateRunner>(updateRunner));
  }

  /**
   * Set the current update runner
   * 
   * @param newrunner
   */
  // Not synchronized - private
  private void setSynchronizedRunner(JSVGSynchronizedRunner newrunner) {
    syncrunner = new WeakReference<JSVGSynchronizedRunner>(newrunner);
  }

  /**
   * Get the current update runner.
   * 
   * @return current update runner
   */
  // Not synchronized - private
  private JSVGSynchronizedRunner getSynchronizedRunner() {
    if(syncrunner == null) {
      return null;
    }
    return syncrunner.get();
  }

  /**
   * Invoke from the SVGs run queue.
   * 
   * @param caller For consistency checks
   */
  protected void invokeFromRunner(JSVGSynchronizedRunner caller) {
    synchronized(this) {
      // Assert that we're still "the one"
      if(caller != getSynchronizedRunner()) {
        return;
      }
      // Remove ourself. We've been run.
      setSynchronizedRunner(null);
    }
    // Wake up all runners
    for(WeakReference<UpdateRunner> wur : updaterunner) {
      UpdateRunner ur = wur.get();
      if(ur != null && !ur.isEmpty()) {
        ur.runQueue();
      }
    }
  }

  /**
   * Forget the current update runner. Called when the update manager is
   * stopped.
   */
  protected void forgetSynchronizedRunner() {
    setSynchronizedRunner(null);
  }

  /**
   * Update Runner that will be placed in the Components UpdateManagers queue.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class JSVGSynchronizedRunner implements Runnable {
    /**
     * Constructor
     */
    protected JSVGSynchronizedRunner() {
      // Nothing to do.
    }

    @Override
    public void run() {
      invokeFromRunner(this);
    }
  }

  /**
   * Adapter that will track the component for UpdateManager changes.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
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
    public void managerStarted(UpdateManagerEvent e) {
      makeRunnerIfNeeded();
    }

    @Override
    public void managerStopped(UpdateManagerEvent e) {
      forgetSynchronizedRunner();
    }
  }
}
