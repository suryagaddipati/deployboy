package com.groupon.jenkins.deployboy;

import com.google.common.base.Joiner;
import com.groupon.jenkins.SetupConfig;
import com.groupon.jenkins.util.HttpPoster;
import hudson.Extension;
import hudson.model.RootAction;
import jenkins.model.Jenkins;
import org.jfree.util.Log;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
@Extension
public class DeployGithubOAuthLoginAction implements RootAction {
    private HttpPoster httpPoster;

    public DeployGithubOAuthLoginAction(){
        this(new HttpPoster());
    }

    public DeployGithubOAuthLoginAction(HttpPoster httpPoster ){
        this.httpPoster = httpPoster;
    }

    public void doIndex(StaplerRequest request, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
        rsp.sendRedirect2( getSetupConfig().getGithubWebUrl()+ "/login/oauth/authorize?client_id="
                + "0df32f9ea10061947eb1" + "&scope="+getScopes());
    }

    protected String getScopes() {
        return Joiner.on(",").join( Arrays.asList("repo_deployment", "repo:status", "user:email", "read:org", "write:repo_hook"));
    }


    protected SetupConfig getSetupConfig() {
        return SetupConfig.get();
    }

    public HttpResponse doFinishLogin(StaplerRequest request,StaplerResponse rsp) throws IOException {

        String code = request.getParameter("code");

        if (code == null || code.trim().length() == 0) {
            Log.info("doFinishLogin: missing code.");
            return HttpResponses.redirectToContextRoot();
        }

        String content = postForAccessToken(code);

        String accessToken = extractToken(content);
        request.getSession().setAttribute("deploy_access_token",accessToken);

        String newProjectSetupUrl = getJenkinsRootUrl() + "/" + DeployGithubReposController.DEPLOYURL;
        return HttpResponses.redirectTo(newProjectSetupUrl);
    }


    String getJenkinsRootUrl() {
        return Jenkins.getInstance().getRootUrl();
    }

    String postForAccessToken(String code) throws IOException {
        SetupConfig setupConfig = getSetupConfig();
        return httpPoster.post(setupConfig.getGithubWebUrl()+"/login/oauth/access_token?" + "client_id="+"0df32f9ea10061947eb1" + "&"
                + "client_secret=" + "91d8e7dad6e6b83f8138592909ec0b586866b99a"+ "&" + "code=" + code,new HashMap());
    }


    private String extractToken(String content) {
        String parts[] = content.split("&");
        for (String part : parts) {
            if (content.contains("access_token")) {
                String tokenParts[] = part.split("=");
                return tokenParts[1];
            }
        }
        return null;
    }


    @Override
    public String getIconFileName() {
        return "new-package.png";
    }

    @Override
    public String getDisplayName() {
        return "Deploy Boy";
    }

    @Override
    public String getUrlName() {
        return "deployBoy";
    }

}
