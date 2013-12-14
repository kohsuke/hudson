/*
 * The MIT License
 *
 * Copyright (c) 2004-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi,
 * Erik Ramfelt, Koichi Fujikawa, Red Hat, Inc., Seiji Sogabe,
 * Stephen Connolly, Tom Huybrechts, Yahoo! Inc., Alan Harder, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.model;


import hudson.security.AccessControlled;
import hudson.slaves.ComputerListener;
import hudson.slaves.RetentionStrategy;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerFallback;
import org.kohsuke.stapler.StaplerProxy;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;

import jenkins.model.Configuration;

public abstract class AbstractCIBase extends Node implements ItemGroup<TopLevelItem>, StaplerProxy, StaplerFallback, ViewGroup, AccessControlled, DescriptorByNameOwner {
    
    public static boolean LOG_STARTUP_PERFORMANCE = Configuration.getBooleanConfigParameter("logStartupPerformance", false);
    
    private static final Logger LOGGER = Logger.getLogger(AbstractCIBase.class.getName());

    private final transient Object updateComputerLock = new Object();

    /**
     * If you are calling this on Hudson something is wrong.
     *
     * @deprecated
     */
    @Deprecated @Override
    public String getNodeName() {
        return "";
    }

   /**
     * @deprecated
     *      Why are you calling a method that always returns ""?
     */
    public String getUrl() {
        return "";
    }

    /* =================================================================================================================
     * Support functions that can only be accessed through package-protected
     * ============================================================================================================== */
    protected void resetLabel(Label l) {
        l.reset();
    }

    protected void setViewOwner(View v) {
        v.owner = this;
    }
    protected void interruptReloadThread() {
        ViewJob.reloadThread.interrupt();
    }

    protected void killComputer(Computer c) {
        c.kill();
    }

    /* =================================================================================================================
    * Package-protected, but accessed API
    * ============================================================================================================== */

    /*package*/ final CopyOnWriteArraySet<String> disabledAdministrativeMonitors = new CopyOnWriteArraySet<String>();

    /* =================================================================================================================
     * Implementation provided
     * ============================================================================================================== */

     /**
     * Returns all {@link Node}s in the system, excluding {@link jenkins.model.Jenkins} instance itself which
     * represents the master.
     */
    public abstract List<Node> getNodes();

    public abstract Queue getQueue();

    protected abstract Map<Node,Computer> getComputerMap();

    /* =================================================================================================================
     * Computer API uses package protection heavily
     * ============================================================================================================== */

    private void updateComputer(Node n, Map<String,Computer> byNameMap, Set<Computer> used, boolean automaticSlaveLaunch) {
        Map<Node,Computer> computers = getComputerMap();
        Computer c;
        c = byNameMap.get(n.getNodeName());
        if (c!=null) {
            c.setNode(n); // reuse
        } else {
            // we always need Computer for the master as a fallback in case there's no other Computer.
            if(n.getNumExecutors()>0 || n==Jenkins.getInstance()) {
                computers.put(n, c = n.createComputer());
                if (!n.isHoldOffLaunchUntilSave() && automaticSlaveLaunch) {
                    RetentionStrategy retentionStrategy = c.getRetentionStrategy();
                    if (retentionStrategy != null) {
                        // if there is a retention strategy, it is responsible for deciding to start the computer
                        retentionStrategy.start(c);
                    } else {
                        // we should never get here, but just in case, we'll fall back to the legacy behaviour
                        c.connect(true);
                    }
                }
            }
        }
        used.add(c);
    }

    /*package*/ void removeComputer(Computer computer) {
        Map<Node,Computer> computers = getComputerMap();
        for (Map.Entry<Node, Computer> e : computers.entrySet()) {
            if (e.getValue() == computer) {
                computers.remove(e.getKey());
                computer.onRemoved();
                return;
            }
        }
        throw new IllegalStateException("Trying to remove unknown computer");
    }

    /*package*/ @CheckForNull Computer getComputer(Node n) {
        Map<Node,Computer> computers = getComputerMap();
        return computers.get(n);
    }

    /**
     * Updates Computers.
     *
     * <p>
     * This method tries to reuse existing {@link Computer} objects
     * so that we won't upset {@link Executor}s running in it.
     */
    protected void updateComputerList(boolean automaticSlaveLaunch) throws IOException {
        Map<Node,Computer> computers = getComputerMap();
        synchronized(updateComputerLock) {// just so that we don't have two code updating computer list at the same time
            Map<String,Computer> byName = new HashMap<String,Computer>();
            for (Computer c : computers.values()) {
                if(c.getNode()==null)
                    continue;   // this computer is gone
                byName.put(c.getNode().getNodeName(),c);
            }

            Set<Computer> old = new HashSet<Computer>(computers.values());
            Set<Computer> used = new HashSet<Computer>();

            updateComputer(this, byName, used, automaticSlaveLaunch);
            for (Node s : getNodes()) {
                long start = System.currentTimeMillis();
                updateComputer(s, byName, used, automaticSlaveLaunch);
                if(LOG_STARTUP_PERFORMANCE)
                    LOGGER.info(String.format("Took %dms to update node %s",
                            System.currentTimeMillis()-start, s.getNodeName()));
            }

            // find out what computers are removed, and kill off all executors.
            // when all executors exit, it will be removed from the computers map.
            // so don't remove too quickly
            old.removeAll(used);
            for (Computer c : old) {
                killComputer(c);
            }
        }
        getQueue().scheduleMaintenance();
        for (ComputerListener cl : ComputerListener.all())
            cl.onConfigurationChange();
    }

}
