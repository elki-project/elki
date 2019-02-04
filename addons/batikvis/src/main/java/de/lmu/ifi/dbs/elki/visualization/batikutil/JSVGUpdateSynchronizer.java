/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.batikutil;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

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
 * @since 0.3
 *
 * @assoc - - - UpdateRunner
 */
class JSVGUpdateSynchronizer implements UpdateSynchronizer {
  /**
   * A weak reference to the component the plot is in.
   */
  private final WeakReference<JSVGComponent> cref;

  /**
   * The UpdateRunner we are put into
   */
  private Set<WeakReference<UpdateRunner>> updaterunner = new CopyOnWriteArraySet<>();

  /**
   * Adapter to track component changes
   */
  private final UMAdapter umadapter = new UMAdapter();

  /**
   * The current Runnable scheduled, prevents repeated invocations.
   */
  private final AtomicReference<Runnable> pending = new AtomicReference<>();

  /**
   * Create an updateSynchronizer for the given component.
   *
   * @param component Component to manage updates on.
   */
  protected JSVGUpdateSynchronizer(JSVGComponent component) {
    assert(component != null);

    this.cref = new WeakReference<>(component);
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
    // We don't need to make a SVG runner when there are no pending updates.
    boolean stop = true;
    for(WeakReference<UpdateRunner> wur : updaterunner) {
      UpdateRunner ur = wur.get();
      if(ur == null) {
        updaterunner.remove(wur);
      }
      else if(!ur.isEmpty()) {
        stop = false;
      }
    }
    if(stop) {
      return;
    }
    // We only need a new runner when we don't have one in the queue yet!
    if(pending.get() != null) {
      return;
    }
    // We need a component
    JSVGComponent component = this.cref.get();
    if(component == null) {
      return;
    }
    // Synchronize with all layers:
    synchronized(this) {
      synchronized(component) {
        UpdateManager um = component.getUpdateManager();
        if(um != null) {
          synchronized(um) {
            if(um.isRunning()) {
              // Create and insert a runner.
              Runnable newrunner = new Runnable() {
                @Override
                public void run() {
                  if(pending.compareAndSet(this, null)) {
                    // Wake up all runners
                    for(WeakReference<UpdateRunner> wur : updaterunner) {
                      UpdateRunner ur = wur.get();
                      if(ur == null || ur.isEmpty()) {
                        continue;
                      }
                      ur.runQueue();
                    }
                  }
                }
              };
              pending.set(newrunner);
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
    for(WeakReference<UpdateRunner> wur : updaterunner) {
      if(wur.get() == null) {
        updaterunner.remove(wur);
      }
    }
    updaterunner.add(new WeakReference<>(updateRunner));
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
    public void managerStarted(UpdateManagerEvent e) {
      makeRunnerIfNeeded();
    }

    @Override
    public void managerStopped(UpdateManagerEvent e) {
      pending.set(null);
    }
  }
}
