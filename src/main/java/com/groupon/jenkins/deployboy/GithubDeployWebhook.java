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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.groupon.jenkins.git.GitUrl;
import com.groupon.jenkins.github.NoDuplicatesParameterAction;
import hudson.Extension;
import hudson.model.*;
import hudson.security.ACL;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContextHolder;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class GithubDeployWebhook implements UnprotectedRootAction {
    private static final Logger LOGGER = Logger.getLogger(com.groupon.jenkins.github.GithubWebhook.class.getName());
    public static final String URL = "githubDeployHook";
    private final SequentialExecutionQueue queue = new SequentialExecutionQueue(Executors.newSingleThreadExecutor());

    @Override
    public String getUrlName() {
        return URL;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void doIndex(StaplerRequest req, StaplerResponse response) throws IOException {
       String payload = getRequestPayload(req);
       if(req.getHeader("X-GitHub-Event").equals("deployment")){
           startDeployment(payload);
       }

        if(req.getHeader("X-GitHub-Event").equals("deployment_status")){
            handleDeployStatusChange(payload);
        }
    }

    private void handleDeployStatusChange(String payload) {

    }

    protected String getRequestPayload(StaplerRequest req) throws IOException {
        return req.getParameter("payload"); // CharStreams.toString(req.getReader());
    }

    public void startDeployment(String payloadData) {
        SecurityContextHolder.getContext().setAuthentication(ACL.SYSTEM);
        final DeploymentEventPayload payload = makePayload(payloadData);
        LOGGER.info("Received POST by " + payload.getPusher());
        if (payload.needsBuild()) {
            LOGGER.info("Received kicking off build for " + payload.getProjectUrl());
            for (final AbstractProject<?, ?> job : getJobsFor(payload.getProjectUrl())) {

                    LOGGER.info("starting job " + job.getName());
                    queue.execute(new Runnable() {
                        @Override
                        public void run() {
                            try{

                                job.scheduleBuild(0, payload.getCause(), new NoDuplicatesParameterAction(getParametersValues(job, payload.getRef())));
                            }catch (Exception e){
                                 LOGGER.log(Level.ALL,"",e);
                            }
                        }
                    });
            }
        }
    }
    public Iterable<DynamicProject> getJobsFor(final String url) {
        return Iterables.filter(Jenkins.getInstance().getAllItems(DynamicProject.class), new Predicate<DynamicProject>() {
            @Override
            public boolean apply( DynamicProject input) {
                GitUrl gitUrl = new GitUrl(url);
                String[] orgRepo = gitUrl.getFullRepoName().split("/");
                return  input.getParent().getName().equals(orgRepo[0]) && input.getName().equals(orgRepo[1] + "-Deploys");
            }
        });
    }
    private List<ParameterValue> getParametersValues(Job job, String ref) {
        ParametersDefinitionProperty paramDefProp = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);
        ArrayList<ParameterValue> defValues = new ArrayList<ParameterValue>();

        for(ParameterDefinition paramDefinition : paramDefProp.getParameterDefinitions())
        {
            if("REF".equals(paramDefinition.getName())){
                StringParameterValue branchParam = new StringParameterValue("REF", ref);
               defValues.add(branchParam);
            }else{
                ParameterValue defaultValue  = paramDefinition.getDefaultParameterValue();
                if(defaultValue != null)
                    defValues.add(defaultValue);
            }
        }

        return defValues;
    }

    protected DeploymentEventPayload makePayload(String payloadData) {
        return new DeploymentEventPayload(payloadData);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

}
