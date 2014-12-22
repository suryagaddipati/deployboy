package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import com.groupon.jenkins.github.GitUrl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.groupon.jenkins.buildtype.dockerimage.DockerCommandBuilder.dockerCommand;
import static java.lang.String.format;

public class DeployBoyBuildConfiguration {
    private Map config;

    public DeployBoyBuildConfiguration(Map config) {
        this.config = config;
    }

    public ShellCommands getShellCommands(DeploymentEventPayload payload) {
     ShellCommands    shellCommands =  new ShellCommands();
        DockerCommandBuilder dockerRunCommand = dockerCommand("run")
                .flag("rm")
                .flag("sig-proxy=true")
                .flag("v","/home/deploy_bot/.ssh/id_rsa:/id_rsa:ro")
                .flag("v","/home/deploy_bot/.ssh/id_rsa_github:/id_rsa_github:ro")
                .args(getImageName(), "sh -cx \"" + getRunCommand(payload) + "\"");
        shellCommands.add(dockerRunCommand.get());
        return shellCommands;
    }
    public ShellCommands getCheckoutCommands(DeploymentEventPayload payload){
        ShellCommands    shellCommands =  new ShellCommands();
        GitUrl gitRepoUrl = new GitUrl(payload.getProjectUrl());
        String gitUrl = payload.getCloneUrl();
        String checkoutLocation = format("/var/%s",gitRepoUrl.getFullRepoName());
        shellCommands.add(format("git clone   %s %s",  gitUrl,checkoutLocation));
        shellCommands.add("cd " + checkoutLocation);
        shellCommands.add(format("git reset --hard  %s", payload.getRef()));
        return  shellCommands;
    }

    private String getRunCommand(DeploymentEventPayload payload) {
        Map<String,String> deploymentConfig = (Map<String, String>) config.get("deployment");
        List<String> deployCommands = Arrays.asList(deploymentConfig.get("task"));
        return getCheckoutCommands(payload).add(new ShellCommands(deployCommands)).toSingleShellCommand();
    }

    private String getImageName() {
         return (String) config.get("environment_image");
    }
}
