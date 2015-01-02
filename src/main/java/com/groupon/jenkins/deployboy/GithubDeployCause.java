package com.groupon.jenkins.deployboy;

import com.google.common.collect.Lists;
import com.groupon.jenkins.dynamic.build.cause.BuildCause;
import com.groupon.jenkins.dynamic.build.cause.GithubLogEntry;
import com.groupon.jenkins.git.GitBranch;

public class GithubDeployCause extends BuildCause{

    public DeploymentEventPayload getPayload() {
        return payload;
    }

    private DeploymentEventPayload payload;

    public GithubDeployCause(DeploymentEventPayload payload){
        this.payload = payload;
    }
    @Override
    public String getBuildDescription() {
        return "By: "+ payload.getPusher() + "\n At: <a href=\" " + payload.getDotCiUrl() + "\"> DotCi Build </a>";
    }

    @Override
    public String getSha() {
        return payload.getRef();
    }

    @Override
    public String getShortDescription() {
        return "Deployment requested by: " + payload.getPusher();
    }

    @Override
    public String getPusher() {
        return payload.getPusher();
    }

    @Override
    public String getPullRequestNumber() {
        return null;
    }

    @Override
    public GitBranch getBranch() {
        return new GitBranch(payload.getRef());
    }

    @Override
    public Iterable<GithubLogEntry> getChangeLogEntries() {
        return Lists.newArrayList();
    }
}
