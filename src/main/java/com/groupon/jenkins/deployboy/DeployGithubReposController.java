package com.groupon.jenkins.deployboy;

import com.google.common.collect.Iterables;
import com.groupon.jenkins.SetupConfig;
import com.groupon.jenkins.buildsetup.GithubReposController;
import com.groupon.jenkins.buildsetup.ProjectConfigInfo;
import com.groupon.jenkins.dynamic.build.DynamicProject;
import com.groupon.jenkins.dynamic.build.DynamicProjectBranchTabsProperty;
import com.groupon.jenkins.dynamic.build.GithubBranchParameterDefinition;
import com.groupon.jenkins.dynamic.buildtype.BuildTypeProperty;
import com.groupon.jenkins.dynamic.organizationcontainer.OrganizationContainer;
import com.groupon.jenkins.dynamic.organizationcontainer.OrganizationContainerRepository;
import com.groupon.jenkins.github.GithubRepoProperty;
import com.groupon.jenkins.github.services.GithubCurrentUserService;
import hudson.Extension;
import hudson.model.Label;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.*;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

@Extension
public class DeployGithubReposController extends GithubReposController{
    public static final String DEPLOYURL = "deployReposGithub";
    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return DEPLOYURL;
    }

    public Iterable<String> getOrgs() throws IOException {
        return getCurrentUser().getOrgs();
    }

    public Iterable<ProjectConfigInfo> getRepositories() throws IOException {
        List<ProjectConfigInfo> projectInfos = new LinkedList<ProjectConfigInfo>();
        Map<String, GHRepository> ghRepos = getCurrentUser().getRepositories(getCurrentOrg());
        for (Map.Entry<String, GHRepository> entry : ghRepos.entrySet()) {
            if (entry.getValue().hasAdminAccess()) {
                projectInfos.add(new ProjectConfigInfo(entry.getKey(), entry.getValue()));
            }
        }
        return projectInfos;
    }

    protected GithubCurrentUserService getCurrentUser() throws IOException {
        return new GithubCurrentUserService(getGitHub(Stapler.getCurrentRequest()));
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
        String org = req.getRestOfPath().replace("/", "");
        req.getSession().setAttribute("setupOrg" + this.getCurrentGithubLogin(), org);
        rsp.forwardToPreviousPage(req);
    }

    private String getCurrentGithubLogin() throws IOException {
        return getCurrentUser().getCurrentLogin();
    }

    public String getCurrentOrg() throws IOException {
        String currentOrg = (String) Stapler.getCurrentRequest().getSession().getAttribute("setupOrg" + getCurrentGithubLogin());
        return StringUtils.isEmpty(currentOrg) ? Iterables.get(getOrgs(), 0) : currentOrg;
    }

    public void doCreateProject(StaplerRequest request, StaplerResponse response) throws IOException {
        GHRepository githubRepo = getGithubRepository(request);
        githubRepo.createWebHook(new URL(getDeployBoyConfiguration().getGithubCallbackUrl()), Arrays.asList(GHEvent.DEPLOYMENT,GHEvent.DEPLOYMENT_STATUS));
        DynamicProject project = createProject(githubRepo);
        response.sendRedirect2(redirectAfterCreateItem(request, project));
    }

    private DeployBoyConfiguration getDeployBoyConfiguration() {
        return DeployBoyConfiguration.get();
    }

    private DynamicProject createProject(GHRepository githubRepository) throws IOException {
        OrganizationContainer folder = new OrganizationContainerRepository().getOrCreateContainer(githubRepository.getOwner().getLogin());
        String projectName = githubRepository.getName()+ "-Deploys";
        DynamicProject project = folder.createProject(DynamicProject.class, projectName);

        project.setDescription(format("<a href=\"%s\">%s</a>", githubRepository.getUrl(), githubRepository.getUrl()));
        project.setConcurrentBuild(true);
        if (StringUtils.isNotEmpty(SetupConfig.get().getLabel())) {
            project.setAssignedLabel(Jenkins.getInstance().getLabel(SetupConfig.get().getLabel()));
        }
        project.addProperty(new ParametersDefinitionProperty(new GithubBranchParameterDefinition("BRANCH", "master",githubRepository.getUrl())));
        project.addProperty(new GithubRepoProperty(githubRepository.getUrl()));
        project.setAssignedLabel(Label.get(getDeployBoyConfiguration().getLabel()));
        project.addProperty(new BuildTypeProperty(DeployBoyBuildType.NAME));
        project.addProperty(new DynamicProjectBranchTabsProperty("master"));
        project.save();
        folder.addItem(project);
        folder.save();
        return project;
    }


    private GHRepository getGithubRepository(StaplerRequest request) throws IOException {
        String repoName = request.getParameter("fullName");

        GitHub github = getGitHub(request);
        return github.getRepository(repoName);
    }

    private GitHub getGitHub(StaplerRequest request) throws IOException {
        return GitHub.connectUsingOAuth(getSetupConfig().getGithubApiUrl(), getAccessToken(request));
    }

    private SetupConfig getSetupConfig() {
        return SetupConfig.get();
    }

    private String getCurrentUserLogin(StaplerRequest request) throws IOException {
        GHUser self = GitHub.connectUsingOAuth(getSetupConfig().getGithubApiUrl(), getAccessToken(request)).getMyself();
        return self.getLogin();
    }

    private String getAccessToken(StaplerRequest request) {
        return (String) request.getSession().getAttribute("deploy_access_token");
    }


    protected String redirectAfterCreateItem(StaplerRequest req, TopLevelItem result) throws IOException {
        return Jenkins.getInstance().getRootUrl()  + result.getUrl();
    }


    @Override
    public Object getTarget() {
        StaplerRequest currentRequest = Stapler.getCurrentRequest();
        //if(getAccessToken(currentRequest) == null) return new GithubOauthLoginAction();
        return  this;
    }
}
