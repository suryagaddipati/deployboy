package com.groupon.jenkins.deployboy;

import hudson.model.Cause;

public class GithubDeployCause extends Cause {
    private String requester;

    public GithubDeployCause(String requester){
        this.requester = requester;
    }
    @Override
    public String getShortDescription() {
        return "Deployment requested by: " + requester;
    }
}
