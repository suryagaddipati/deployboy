package com.groupon.jenkins.deployboy;

import hudson.model.Cause;
import net.sf.json.JSONObject;

public class DeploymentEventPayload {
    private final String pusher;
    private final String projectUrl;
    private final String dotCiUrl;
    private String cloneUrl;
    private String ref;
    private int id;


    public DeploymentEventPayload(String payloadData) {
        JSONObject payloadJson = JSONObject.fromObject(payloadData);
        JSONObject deployment = payloadJson.getJSONObject("deployment");
        JSONObject dotCiInfo = JSONObject.fromObject(deployment.getString("payload")).getJSONObject("DotCi");
        pusher = dotCiInfo.getString("pusher");
        dotCiUrl = dotCiInfo.getString("url");
        projectUrl = payloadJson.getJSONObject("repository").getString("html_url");
        cloneUrl =payloadJson.getJSONObject("repository").getString("clone_url");
        ref = deployment.getString("ref");
        id = deployment.getInt("id");
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

    public String getDotCiUrl() {
        return dotCiUrl;
    }
}
