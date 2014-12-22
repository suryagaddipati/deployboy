package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.buildtype.util.shell.ShellScriptRunner;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.execution.BuildExecutionContext;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import com.groupon.jenkins.util.GroovyYamlTemplateProcessor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;

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
        new ShellScriptRunner(buildExecutionContext,listener).runScript(deployBoyBuildConfiguration.getShellCommands(payload));
        return Result.ABORTED ;
    }

    private String getDeployBoyYml(DynamicBuild build) throws IOException {
        try {
            return build.getGithubRepositoryService().getGHFile("deploy_boy.yml", build.getSha()).getContent();
        } catch (FileNotFoundException _){
            throw new InvalidBuildConfigurationException("No deploy_boy.yml found.");
        }
    }
}
