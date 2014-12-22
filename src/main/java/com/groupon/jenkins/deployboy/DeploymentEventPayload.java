package com.groupon.jenkins.deployboy;

import hudson.model.Cause;
import net.sf.json.JSONObject;

public class DeploymentEventPayload {

    private final JSONObject payloadJson;

    public DeploymentEventPayload(String payloadData) {
        this.payloadJson = JSONObject.fromObject(payloadData);
    }

    public String getPusher() {
        return payloadJson.getJSONObject("deployment").getJSONObject("creator").getString("login");
    }

    public boolean needsBuild() {
        return true;
    }

    public String getProjectUrl() {
        return  payloadJson.getJSONObject("repository").getString("html_url");
    }
    public String getCloneUrl() {
        return  payloadJson.getJSONObject("repository").getString("clone_url");
    }

    public Cause getCause() {
        return new GithubDeployCause(this);
    }

    public String getRef() {
        return payloadJson.getString("ref");
    }
}
