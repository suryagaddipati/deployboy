package com.groupon.jenkins.deployboy;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.groupon.jenkins.dynamic.build.cause.BuildCause;
import com.groupon.jenkins.dynamic.build.cause.GithubLogEntry;
import com.groupon.jenkins.git.GitBranch;
import com.groupon.jenkins.github.services.GithubRepositoryService;
import org.kohsuke.github.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GithubDeployCause extends BuildCause{

    private List<GithubLogEntry> changeLog = Lists.newArrayList();
    private GitBranch branch;
    private String diffUrl;

    public DeploymentEventPayload getPayload() {
        return payload;
    }

    private DeploymentEventPayload payload;

    public GithubDeployCause(DeploymentEventPayload payload){
        this.payload = payload;
        GHRepository repository = getGithubRepository(payload);
        this.branch =new GitBranch(payload.getEnvironment());
        setChangeLog(payload, repository);
    }

    private void setChangeLog(DeploymentEventPayload payload, GHRepository repository) {
        PagedIterable<GHDeployment> deployments = repository.listDeployments(null, null, null, payload.getEnvironment());
        try{
            GHDeployment prevDeployment = Iterables.get(deployments, 1);
            try {
                GHCompare diff = repository.getCompare(prevDeployment.getSha(),payload.getSha());
                this.diffUrl = diff.getHtmlUrl().toString();
                changeLog = toChangeLog(diff);
            } catch (IOException e) {
                throw  new RuntimeException(e);
            }
        }catch(IndexOutOfBoundsException _){
//empty changelog first deployment

        }
    }

    private List<GithubLogEntry> toChangeLog(GHCompare diff) {
        ArrayList<GithubLogEntry> changeLogEntries = new ArrayList<GithubLogEntry>();
        for( GHCommit commit: diff.getCommits()){
            changeLogEntries.add(toGithubChangeLog(commit));
        }
        return changeLogEntries;
    }

    private GithubLogEntry toGithubChangeLog(GHCommit commit) {
        //    public GithubLogEntry(java.lang.String message, java.lang.String githubUrl, java.lang.String commitId, java.util.List<java.lang.String> affectedPaths) { /* compiled code */ }
        String url = String.format("%s/commit/%s",commit.getOwner().getUrl(), commit.getSHA1());
        GHCompare.InnerCommit innerCommit = ((GHCompare.Commit) commit).getCommit();
        Iterable<String> paths = Iterables.transform(commit.getFiles(), new Function<GHCommit.File, String>() {
            @Override
            public String apply(@Nullable GHCommit.File input) {
                return input.getFileName();
            }
        });
        return  new GithubLogEntry(innerCommit.getMessage(),url, commit.getSHA1(),Lists.newArrayList(paths));
    }

    @Override
    public String getBuildDescription() {
      if(diffUrl == null)  {
          return String.format("<b>%s</b>  <br> %s", payload.getEnvironment(), payload.getPusher());
      }
       return String.format("<b>%s</b> (<a href=\"%s\">%s...</a>) " +
               "<br> <img height=\"24\"  width=\"24\" src=\"%s\" tooltip=\"%s\">" +
               "<br>", payload.getEnvironment(),diffUrl, "Commits", payload.getPusherAvatarUrl(), payload.getPusher());
    }

    @Override
    public String getSha() {
        return payload.getRef();
    }

    @Override
    public String getShortDescription() {
        return String.format("Deployment requested by: <b>%s</b>  <br> From: %s", payload.getPusher(), payload.getDotCiUrl());
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
        return this.branch;
    }

    @Override
    public Iterable<GithubLogEntry> getChangeLogEntries() {
        return changeLog;
    }
    public GHRepository getGithubRepository(DeploymentEventPayload payload) {
        return new GithubRepositoryService(payload.getProjectUrl()).getGithubRepository();
    }
}
