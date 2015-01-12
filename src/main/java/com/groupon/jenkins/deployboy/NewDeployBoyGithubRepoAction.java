package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.SetupConfig;
import com.groupon.jenkins.buildsetup.GithubRepoAction;
import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.groupon.jenkins.dynamic.build.DynamicProjectBranchTabsProperty;
import com.groupon.jenkins.dynamic.buildtype.BuildTypeProperty;
import com.groupon.jenkins.dynamic.organizationcontainer.OrganizationContainer;
import com.groupon.jenkins.dynamic.organizationcontainer.OrganizationContainerRepository;
import com.groupon.jenkins.github.GithubRepoProperty;
import hudson.Extension;
import hudson.model.Label;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHEvent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import static java.lang.String.format;

@Extension
public class NewDeployBoyGithubRepoAction extends GithubRepoAction{

    public void doCreateProject(StaplerRequest request, StaplerResponse response) throws IOException {
        GHRepository githubRepo = getGithubRepository(request);
        githubRepo.createWebHook(new URL(getDeployBoyConfiguration().getGithubCallbackUrl()), Arrays.asList(GHEvent.DEPLOYMENT,GHEvent.DEPLOYMENT_STATUS));
        DynamicProject project = createProject(githubRepo);
        response.sendRedirect2(redirectAfterCreateItem(request, project));
    }
    protected String redirectAfterCreateItem(StaplerRequest req, TopLevelItem result) throws IOException {
        return Jenkins.getInstance().getRootUrl()  + result.getUrl();
    }
    private DeployBoyConfiguration getDeployBoyConfiguration() {
        return DeployBoyConfiguration.get();
    }

    private DynamicProject createProject(GHRepository githubRepository) throws IOException {
        OrganizationContainer folder = new OrganizationContainerRepository().getOrCreateContainer(githubRepository.getOwner().getLogin());
        String projectName = githubRepository.getName()+ "-Deploys";
        DeployBoyJob project = folder.createProject(DeployBoyJob.class, projectName);

        project.setDescription(format("<a href=\"%s\">%s</a>", githubRepository.getUrl(), githubRepository.getUrl()));
        project.setConcurrentBuild(true);
        if (StringUtils.isNotEmpty(SetupConfig.get().getLabel())) {
            project.setAssignedLabel(Jenkins.getInstance().getLabel(SetupConfig.get().getLabel()));
        }
        project.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition("REF","master")));
        project.setAssignedLabel(Label.get(StringUtils.trimToNull(getDeployBoyConfiguration().getLabel())));
        project.addProperty(new BuildTypeProperty(DeployBoyBuildType.class.getName()));
        project.addProperty(new GithubRepoProperty(githubRepository.getUrl()));
        project.addProperty(new DynamicProjectBranchTabsProperty("master"));
        project.save();
        folder.addItem(project);
        folder.save();
        return project;
    }



}
