/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package com.groupon.jenkins.deployboy;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.groupon.jenkins.DotCiModule;
import com.groupon.jenkins.buildtype.install_packages.InstallPackagesBuild;
import com.groupon.jenkins.dynamic.build.repository.DynamicBuildRepository;
import com.groupon.jenkins.dynamic.build.repository.DynamicProjectRepository;
import com.groupon.jenkins.dynamic.buildtype.BuildType;
import com.groupon.jenkins.github.services.GithubAccessTokenRepository;
import com.groupon.jenkins.github.services.GithubDeployKeyRepository;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class DeployBoyConfiguration extends GlobalConfiguration {

    private String githubClientID;
    private String githubClientSecret;
    private String githubCallbackUrl;
    private AbstractModule guiceModule;
    private transient Injector injector;
    private String label;

    public static DeployBoyConfiguration get() {
        return GlobalConfiguration.all().get(DeployBoyConfiguration.class);
    }

    public DeployBoyConfiguration() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }


    public String getGithubCallbackUrl() {
        if (StringUtils.isEmpty(githubCallbackUrl)) {
            return Jenkins.getInstance().getRootUrl() + GithubDeployWebhook.URL+"/";
        }
        return this.githubCallbackUrl;
    }

    public void setGithubCallbackUrl(String githubCallbackUrl) {
        this.githubCallbackUrl = githubCallbackUrl;
    }

    public void setLabel(String label) {
        this.label = label;
    }



    public String getGithubClientID() {
        return githubClientID;
    }

    public void setGithubClientID(String githubClientID) {
        this.githubClientID = githubClientID;
    }

    public String getGithubClientSecret() {
        return githubClientSecret;
    }

    public void setGithubClientSecret(String githubClientSecret) {
        this.githubClientSecret = githubClientSecret;
    }

    public DynamicProjectRepository getDynamicProjectRepository() {
        return getInjector().getInstance(DynamicProjectRepository.class);
    }

    public GithubDeployAccessTokenRepository getDeployAccessTokenRepository() {
        return getInjector().getInstance(GithubDeployAccessTokenRepository.class);
    }

    private transient Object injectorLock = new Object();

    public Injector getInjector() {
        if(injector == null) {
            synchronized (injectorLock) {
                if(injector == null) { // make sure we got the lock in time
                    injector = Guice.createInjector(getGuiceModule());
                }
            }
        }

        return injector;
    }


    private AbstractModule getGuiceModule() {
        if(guiceModule == null) {
            return new DotCiModule();
        }
        return guiceModule;
    }

    public String getLabel() {
        return label;
    }
}
