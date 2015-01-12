package com.groupon.jenkins.deployboy.notifications;

import com.groupon.jenkins.dynamic.build.DynamicBuild;
import hudson.ExtensionPoint;
import hudson.model.BuildListener;
import org.kohsuke.github.GHDeploymentStatus;

public abstract class DeployNotifier implements ExtensionPoint {

    private Object options;

    public abstract String getName() ;

    public void setOptions(Object options) {
        this.options = options;
    }

    protected Object getOptions() {
        return options;
    }

    public abstract void notify(GHDeploymentStatus deploymentStatus, DynamicBuild build, BuildListener listener);
}
