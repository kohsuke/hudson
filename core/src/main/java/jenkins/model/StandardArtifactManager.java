/*
 * The MIT License
 *
 * Copyright 2013 Jesse Glick.
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

package jenkins.model;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.Job;
import hudson.model.Run;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.kohsuke.stapler.HttpResponse;

/**
 * Default artifact manager which transfers files over the remoting channel and stores them inside the build directory.
 * @since XXX
 */
@Extension(ordinal=0)
public class StandardArtifactManager extends ArtifactManager {

    /**
     * The singleton instance, for delegation from managers which only customize transfer and not storage.
     * @return a {@link StandardArtifactManager}
     */
    public static ArtifactManager instance() {
        return Jenkins.getInstance().getExtensionList(ArtifactManager.class).get(StandardArtifactManager.class);
    }

    @Override public boolean appliesTo(Run<?,?> build) {
        return build instanceof AbstractBuild;
    }

    @Override public int archive(Run<?,?> build, Launcher launcher, BuildListener listener, String artifacts, String excludes) throws IOException, InterruptedException {
        File dir = getArtifactsDir(build);
        dir.mkdirs();
        FilePath workspace = ((AbstractBuild) build).getWorkspace();
        if (workspace == null) {
            return 0; // ArtifactArchiver has already checked this case
        }
        return workspace.copyRecursiveTo(artifacts, excludes, new FilePath(dir));
    }

    @Override public boolean deleteArtifacts(Run<?,?> build) throws IOException, InterruptedException {
        File ad = getArtifactsDir(build);
        if (!ad.exists()) {
            return false;
        }
        Util.deleteRecursive(ad);
        return true;
    }

    @Override public HttpResponse browseArtifacts(Run<?,?> build) {
        return new DirectoryBrowserSupport(build, new FilePath(getArtifactsDir(build)), build.getParent().getDisplayName() + ' ' + build.getDisplayName(), "package.png", true);
    }

    @Override public <JobT extends Job<JobT,RunT>, RunT extends Run<JobT,RunT>> Run<JobT,RunT>.ArtifactList getArtifactsUpTo(Run<JobT,RunT> build, int n) {
        Run<JobT,RunT>.ArtifactList r = build.new ArtifactList();
        addArtifacts(build, getArtifactsDir(build), "", "", r, null, n, new AtomicInteger());
        r.computeDisplayName();
        return r;
    }

    private static <JobT extends Job<JobT,RunT>, RunT extends Run<JobT,RunT>> int addArtifacts(Run<JobT,RunT> build, File dir, String path, String pathHref, Run<JobT,RunT>.ArtifactList r, Run<JobT,RunT>.Artifact parent, int upTo, AtomicInteger idSeq) {
        String[] children = dir.list();
        if(children==null)  return 0;
        Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);

        int n = 0;
        for (String child : children) {
            String childPath = path + child;
            String childHref = pathHref + Util.rawEncode(child);
            File sub = new File(dir, child);
            String length = sub.isFile() ? String.valueOf(sub.length()) : "";
            boolean collapsed = (children.length==1 && parent!=null);
            Run<JobT,RunT>.Artifact a;
            if (collapsed) {
                // Collapse single items into parent node where possible:
                a = build.new Artifact(parent.getFileName() + '/' + child, childPath,
                                 sub.isDirectory() ? null : childHref, length,
                                 parent.getTreeNodeId());
                r.getTree().put(a, r.getTree().remove(parent));
            } else {
                // Use null href for a directory:
                a = build.new Artifact(child, childPath,
                                 sub.isDirectory() ? null : childHref, length,
                                 "n" + idSeq.incrementAndGet());
                r.getTree().put(a, parent!=null ? parent.getTreeNodeId() : null);
            }
            if (sub.isDirectory()) {
                n += addArtifacts(build, sub, childPath + '/', childHref + '/', r, a, upTo-n, idSeq);
                if (n>=upTo) break;
            } else {
                // Don't store collapsed path in ArrayList (for correct data in external API)
                r.add(collapsed ? build.new Artifact(child, a.relativePath, a.getHref(), length, a.getTreeNodeId()) : a);
                if (++n>=upTo) break;
            }
        }
        return n;
    }

    @SuppressWarnings("deprecation")
    private static File getArtifactsDir(Run<?,?> build) {
        return build.getArtifactsDir();
    }

}
