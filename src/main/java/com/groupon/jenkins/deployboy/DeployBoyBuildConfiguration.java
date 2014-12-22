package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.docker.CheckoutCommands;
import com.groupon.jenkins.buildtype.util.shell.ShellCommands;
import com.groupon.jenkins.github.GitUrl;

import java.util.Map;

import static java.lang.String.format;

public class DeployBoyBuildConfiguration {
    private Map config;

    public DeployBoyBuildConfiguration(Map config) {
        this.config = config;
    }

    public ShellCommands getShellCommands(DeploymentEventPayload payload) {
     ShellCommands    shellCommands =  new ShellCommands();
       //shellCommands.add(getChe)

        GitUrl gitRepoUrl = new GitUrl(payload.getProjectUrl());
        String gitUrl = payload.getCloneUrl();
        String checkoutLocation = format("/var/%s",gitRepoUrl.getFullRepoName());
        shellCommands.add(format("git clone   %s %s",  gitUrl,checkoutLocation));
        shellCommands.add("cd " + checkoutLocation);
        shellCommands.add(format("git reset --hard  %s", payload.getRef()));
        return shellCommands;
    }
}
