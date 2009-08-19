/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Jorg Heymans, Stephen Connolly, Tom Huybrechts
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

import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrappers;
import hudson.tasks.Builder;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Publisher;
import hudson.tasks.Maven;
import hudson.tasks.Maven.ProjectWithMaven;
import hudson.tasks.Maven.MavenInstallation;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Buildable software project.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Project<P extends Project<P,B>,B extends Build<P,B>>
    extends AbstractProject<P,B> implements SCMedItem, Saveable, ProjectWithMaven {

    /**
     * List of active {@link Builder}s configured for this project.
     */
    private DescribableList<Builder,Descriptor<Builder>> builders =
            new DescribableList<Builder,Descriptor<Builder>>(this);

    /**
     * List of active {@link Publisher}s configured for this project.
     */
    private DescribableList<Publisher,Descriptor<Publisher>> publishers =
            new DescribableList<Publisher,Descriptor<Publisher>>(this);

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     */
    private DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappers =
            new DescribableList<BuildWrapper,Descriptor<BuildWrapper>>(this);

    /**
     * Creates a new project.
     */
    public Project(ItemGroup parent,String name) {
        super(parent,name);
    }

    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);

        if(buildWrappers==null)
            // it didn't exist in < 1.64
            buildWrappers = new DescribableList<BuildWrapper, Descriptor<BuildWrapper>>(this);
        builders.setOwner(this);
        publishers.setOwner(this);
        buildWrappers.setOwner(this);
    }

    public AbstractProject<?, ?> asProject() {
        return this;
    }

    public List<Builder> getBuilders() {
        return builders.toList();
    }
    
    public List<Builder> getTemplateBuilders() {
    	Project<?,?> templateProject = getTemplateProject();
    	if (templateProject == null) {
    		return Collections.emptyList();
    	} else {
    		return getTemplateProject().getAllBuilders();
    	}
    }
    
    public List<Builder> getAllBuilders() {
        return Util.join(getTemplateBuilders(), getBuilders());
    }
    
    public Map<Descriptor<Publisher>,Publisher> getTemplatePublishers() {
    	Project<?,?> templateProject = getTemplateProject();
    	if (templateProject == null) {
    		return Collections.emptyMap();
    	} else {
    		return getTemplateProject().getAllPublishers();
    	}
    }
    public Map<Descriptor<Publisher>,Publisher> getAllPublishers() {
        return Util.join(getTemplatePublishers(), getPublishers());
    }

    public Map<Descriptor<BuildWrapper>,BuildWrapper> getTemplateBuildWrappers() {
    	Project<?,?> templateProject = getTemplateProject();
    	if (templateProject == null) {
    		return Collections.emptyMap();
    	} else {
    		return getTemplateProject().getAllBuildWrappers();
    	}
    }
    public Map<Descriptor<BuildWrapper>,BuildWrapper> getAllBuildWrappers() {
        return Util.join(getTemplateBuildWrappers(), getBuildWrappers());
    }
    
    public Map<Descriptor<Publisher>,Publisher> getPublishers() {
        return publishers.toMap();
    }

    public DescribableList<Builder,Descriptor<Builder>> getBuildersList() {
        return builders;
    }
    
    public DescribableList<Builder,Descriptor<Builder>> getAllBuildersList() {
    	Project<?,?> templateProject = getTemplateProject();
    	if (templateProject == null) {
    		return new DescribableList<Builder, Descriptor<Builder>>(Saveable.NOOP);
    	} else {
    		return getTemplateProject().getAllBuildersList();
    	}
    }

    public DescribableList<Publisher,Descriptor<Publisher>> getAllPublishersList() {
    	Project<?,?> templateProject = getTemplateProject();
    	if (templateProject == null) {
    		return new DescribableList<Publisher, Descriptor<Publisher>>(Saveable.NOOP);
    	} else {
    		return getTemplateProject().getAllPublishersList();
    	}
    }
    public DescribableList<Publisher,Descriptor<Publisher>> getPublishersList() {
        return publishers;
    }

    public Map<Descriptor<BuildWrapper>,BuildWrapper> getBuildWrappers() {
        return buildWrappers.toMap();
    }

    @Override
    protected Set<ResourceActivity> getResourceActivities() {
        final Set<ResourceActivity> activities = new HashSet<ResourceActivity>();

        if (getTemplateProject() != null) activities.addAll(getTemplateProject().getResourceActivities());
        activities.addAll(super.getResourceActivities());
        activities.addAll(Util.filter(builders,ResourceActivity.class));
        activities.addAll(Util.filter(publishers,ResourceActivity.class));
        activities.addAll(Util.filter(buildWrappers,ResourceActivity.class));

        return activities;
    }

    /**
     * Adds a new {@link BuildStep} to this {@link Project} and saves the configuration.
     *
     * @deprecated as of 1.290
     *      Use {@code getPublishersList().add(x)}
     */
    public void addPublisher(Publisher buildStep) throws IOException {
        publishers.add(buildStep);
    }

    /**
     * Removes a publisher from this project, if it's active.
     *
     * @deprecated as of 1.290
     *      Use {@code getPublishersList().remove(x)}
     */
    public void removePublisher(Descriptor<Publisher> descriptor) throws IOException {
        publishers.remove(descriptor);
    }

    public Publisher getPublisher(Descriptor<Publisher> descriptor) {
        for (Publisher p : publishers) {
            if(p.getDescriptor()==descriptor)
                return p;
        }
        return null;
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        getAllPublishersList().buildDependencyGraph(this,graph);
        builders.buildDependencyGraph(this,graph);
        buildWrappers.buildDependencyGraph(this,graph);
    }

    @Override
    public boolean isFingerprintConfigured() {
        for (Publisher p : getAllPublishersList()) {
            if(p instanceof Fingerprinter)
                return true;
        }
        return false;
    }

    public MavenInstallation inferMavenInstallation() {
        for (Builder builder : getAllBuilders()) {
            if (builder instanceof Maven)
                return ((Maven) builder).getMaven();
        }
        return null;
    }

//
//
// actions
//
//
    @Override
    protected void submit( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException, FormException {
        super.submit(req,rsp);

        req.setCharacterEncoding("UTF-8");
        JSONObject json = req.getSubmittedForm();

        buildWrappers.rebuild(req,json, BuildWrappers.getFor(this));
        builders.rebuildHetero(req,json, Builder.all(), "builder");
        publishers.rebuild(req, json, BuildStepDescriptor.filter(Publisher.all(), this.getClass()));
        updateTransientActions(); // to pick up transient actions from builder, publisher, etc.
    }

    protected void updateTransientActions() {
        synchronized(transientActions) {
            super.updateTransientActions();

            for (BuildStep step : getBuildersList()) {
                Action a = step.getProjectAction(this);
                if(a!=null)
                    transientActions.add(a);
            }
            for (BuildStep step : getPublishersList()) {
                Action a = step.getProjectAction(this);
                if(a!=null)
                    transientActions.add(a);
            }
            for (BuildWrapper step : getBuildWrappers().values()) {
                Action a = step.getProjectAction(this);
                if(a!=null)
                    transientActions.add(a);
            }
            for (Trigger trigger : getTriggers().values()) {
                Action a = trigger.getProjectAction();
                if(a!=null)
                    transientActions.add(a);
            }
        }
    }

    /**
     * @deprecated
     *      left for legacy config file compatibility
     */
    @Deprecated
    private transient String slave;
}