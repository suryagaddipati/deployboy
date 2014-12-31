package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import com.groupon.jenkins.git.GitUrl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder.dockerCommand;
import static java.lang.String.format;

public class DeployBoyBuildConfiguration {
    private final GHDeploymentConfiguration ghDeploymentConfiguration;
    private Map config;

    public DeployBoyBuildConfiguration(Map config) {
        this.config = config;
        this.ghDeploymentConfiguration = new GHDeploymentConfiguration((Map<String, Object>) config.get("deployment"));
    }

    public ShellCommands getShellCommands(String cloneUrl, String ref) {
     ShellCommands    shellCommands =  new ShellCommands();
        DockerCommandBuilder dockerRunCommand = dockerCommand("run")
                .flag("rm")
                .flag("sig-proxy=true")
                .flag("v","/home/deploy_bot/.ssh/id_rsa:/id_rsa:ro")
                .flag("v","/home/deploy_bot/.ssh/id_rsa_github:/id_rsa_github:ro")
                .args(getImageName(), "sh -cx \"" + getRunCommand(cloneUrl,ref) + "\"");
        shellCommands.add(dockerRunCommand.get());
        return shellCommands;
    }
    public ShellCommands getCheckoutCommands(String cloneUrl, String ref){
        ShellCommands    shellCommands =  new ShellCommands();
        GitUrl gitRepoUrl = new GitUrl(cloneUrl);
        String checkoutLocation = format("/var/%s",gitRepoUrl.getFullRepoName());
        shellCommands.add(format("git clone   %s %s",  cloneUrl,checkoutLocation));
        shellCommands.add("cd " + checkoutLocation);
        shellCommands.add(format("git reset --hard  %s", ref));
        return  shellCommands;
    }

    private String getRunCommand(String cloneUrl, String ref) {
        Map<String,String> deploymentConfig = (Map<String, String>) config.get("deployment");
        List<String> deployCommands = Arrays.asList(deploymentConfig.get("task"));
        return getCheckoutCommands(cloneUrl,ref).add(new ShellCommands(deployCommands)).toSingleShellCommand();
    }

    private String getImageName() {
         return (String) config.get("environment_image");
    }

    public GHDeploymentConfiguration getGHDeploymentConfiguration() {
        return this.ghDeploymentConfiguration;
    }


    public static class GHDeploymentConfiguration{
        /*
        ref: master
task: bundle install ; bundle exec cap $deploy_env deploy
auto_merge: false
required_contexts: [DotCi]
payload: "[]"
environment: environment
description: "deploy boy"
         */

        private Map<String, Object> deploymentConfig;

        public GHDeploymentConfiguration(Map<String, Object> deploymentConfig) {
            this.deploymentConfig = deploymentConfig;
        }
        public String getRef(){
            return (String) this.deploymentConfig.get("ref");
        }
        public String getEnvironment(){
            return (String) this.deploymentConfig.get("environment");
        }
        public String getDescription(){
            return (String) this.deploymentConfig.get("description");
        }

        public List<String> getRequireContexts(){
            return (List<String>) this.deploymentConfig.get("required_contexts");
        }
        public String getPayload(){
            return (String) this.deploymentConfig.get("payload");
        }
        public Boolean isAutoMerge(){
           return (Boolean) this.deploymentConfig.get("auto_merge");
        }
    }
}
