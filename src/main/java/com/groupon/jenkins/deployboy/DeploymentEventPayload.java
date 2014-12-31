package com.groupon.jenkins.deployboy;

import hudson.model.Cause;
import net.sf.json.JSONObject;

public class DeploymentEventPayload {
    private final String pusher;
    private final String projectUrl;
    private String cloneUrl;
    private String ref;
    private int id;

//    private final JSONObject payloadJson;

    public DeploymentEventPayload(String payloadData) {
        JSONObject payloadJson = JSONObject.fromObject(payloadData);
         pusher = payloadJson.getJSONObject("deployment").getJSONObject("creator").getString("login");
        projectUrl = payloadJson.getJSONObject("repository").getString("html_url");
        cloneUrl =payloadJson.getJSONObject("repository").getString("clone_url");
        ref =payloadJson.getJSONObject("deployment").getString("ref");
        id = payloadJson.getJSONObject("deployment").getInt("id");
    }

    public String getPusher() {
        return pusher;
    }

    public boolean needsBuild() {
        return true;
    }

    public String getProjectUrl() {
        return   projectUrl;
    }
    public String getCloneUrl() {
        return  cloneUrl;
    }

    public Cause getCause() {
        return new GithubDeployCause(this);
    }

    public String getRef() {
        return ref;
    }

    public int getId() {
        return this.id;
    }
}
