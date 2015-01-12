package com.groupon.jenkins.deployboy.notifications;

import com.groupon.jenkins.dynamic.build.DynamicBuild;
import hudson.Extension;
import hudson.model.BuildListener;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHDeploymentStatus;

@Extension
public class HipchatNotifier extends DeployNotifier {
    @Override
    public String getName() {
        return "hipchat";
    }

    @Override
    public void notify(GHDeploymentStatus deploymentStatus, DynamicBuild build, BuildListener listener) {
        if(deploymentStatus.getState().equals(GHCommitState.PENDING)){
            listener.getLogger().println("Hipchat: Deployment Pending at:"+ deploymentStatus.getDeploymentUrl() );
        }
        if(deploymentStatus.getState().equals(GHCommitState.FAILURE)){
            listener.getLogger().println("Hipchat: Deployment Failed at:"+ deploymentStatus.getDeploymentUrl() );
        }
        if(deploymentStatus.getState().equals(GHCommitState.SUCCESS)){
            listener.getLogger().println("Hipchat: Deployment Success at:"+ deploymentStatus.getDeploymentUrl() );
        }
    }
}
