package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.execution.BuildExecutionContext;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;

import java.io.IOException;

/**
 * Created by surya on 12/28/2014.
 */
@Extension
public class DummyBuildType extends BuildType {
    @Override
    public String getDescription() {
        return "Dummy";
    }

    @Override
    public Result runBuild(DynamicBuild dynamicBuild, BuildExecutionContext buildExecutionContext, Launcher launcher, BuildListener buildListener) throws IOException, InterruptedException {
        dynamicBuild.setResult(Result.SUCCESS);
        new DeployRequestDotCiPlugin().perform(dynamicBuild,launcher,buildListener);
        return Result.SUCCESS;
    }
}
