package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.buildtype.plugins.DotCiPluginAdapter;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.util.GroovyYamlTemplateProcessor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Run;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

@Extension
public class DeployRequestDotCiPlugin extends DotCiPluginAdapter {
    protected DeployRequestDotCiPlugin() {
        super("deploy_request", "");
    }

    @Override
    public boolean perform(DynamicBuild build, Launcher launcher, BuildListener listener) {
        try {
            Map<String,Object> buildEnvironment = build.getEnvironmentWithChangeSet(listener);
            Map config = new GroovyYamlTemplateProcessor(getDeployBoyYml(build), buildEnvironment).getConfig();
            DeployBoyBuildConfiguration deployBoyBuildConfiguration = new DeployBoyBuildConfiguration( config);
            /**
            GHDeployment deployment = repository.createDeployment("master")
                    .payload("{\"user\":\"atmos\",\"room_id\":123456}")
                    .description("question")
                    .create(); **/
            return true;
        } catch (Exception e) {
           throw new RuntimeException(e);
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
