package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.buildtype.util.shell.ShellScriptRunner;
import com.groupon.jenkins.deployboy.notifications.DeployNotifier;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.execution.BuildExecutionContext;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import com.groupon.jenkins.util.GroovyYamlTemplateProcessor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import org.kohsuke.github.GHDeploymentState;
import org.kohsuke.github.GHDeploymentStatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

@Extension
public class DeployBoyBuildType extends BuildType{

    public static String NAME = "Deploy Boy";

    @Override
    public String getDescription() {
        return NAME;
    }

    @Override
    public Result runBuild(DynamicBuild build, BuildExecutionContext buildExecutionContext, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        //Set Deployment Status to pending
        GithubDeployCause cause = (GithubDeployCause) build.getCause();
        DeploymentEventPayload payload = cause.getPayload();
        Map<String,Object> buildEnvironment = build.getEnvironmentWithChangeSet(listener);
        Map config = new GroovyYamlTemplateProcessor(getDeployBoyYml(build), buildEnvironment).getConfig();
        DeployBoyBuildConfiguration deployBoyBuildConfiguration = new DeployBoyBuildConfiguration( config);

        GHDeploymentStatus deploymentStatus = build.getGithubRepository().createDeployStatus(payload.getId(), GHDeploymentState.PENDING).create();
        runNotifiers(deploymentStatus,build,listener,deployBoyBuildConfiguration,deployBoyBuildConfiguration.getPendingNotifications());

        Result result = new ShellScriptRunner(buildExecutionContext, listener).runScript(deployBoyBuildConfiguration.getShellCommands(payload.getCloneUrl(), payload.getRef()));
        if(Result.SUCCESS.equals(result)){
            deploymentStatus = build.getGithubRepository().createDeployStatus(payload.getId(), GHDeploymentState.SUCCESS).create();
            runNotifiers(deploymentStatus,build,listener,deployBoyBuildConfiguration,deployBoyBuildConfiguration.getSuccessNotifications());
        }else{
            deploymentStatus = build.getGithubRepository().createDeployStatus(payload.getId(), GHDeploymentState.FAILURE).create();
            runNotifiers(deploymentStatus,build,listener,deployBoyBuildConfiguration,deployBoyBuildConfiguration.getFailureNotifications());
        }
        return result;
    }

    private void runNotifiers(GHDeploymentStatus deploymentStatus, DynamicBuild build, BuildListener listener, DeployBoyBuildConfiguration deployBoyBuildConfiguration, Iterable<DeployNotifier> deployNotifiers) {
         for(DeployNotifier deployNotifier : deployNotifiers){
            deployNotifier.notify(deploymentStatus,build,listener) ;
         }

    }

    private String getDeployBoyYml(DynamicBuild build) throws IOException {
        try {
            return build.getGithubRepositoryService().getGHFile("deploy_boy.yml", build.getSha()).getContent();
        } catch (FileNotFoundException _){
            throw new InvalidBuildConfigurationException("No deploy_boy.yml found.");
        }
    }
}
