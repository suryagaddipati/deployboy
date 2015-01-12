package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.buildtype.plugins.DotCiPluginAdapter;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.util.GroovyYamlTemplateProcessor;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHCommitState;
import org.kohsuke.github.GHDeployment;
import org.kohsuke.github.GHRepository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static hudson.model.Result.SUCCESS;
import static hudson.model.Result.UNSTABLE;

@Extension
public class DeployRequestDotCiPlugin extends DotCiPluginAdapter {
    public DeployRequestDotCiPlugin() {
        super("deploy_boy", "");
    }

    @Override
    public boolean perform(DynamicBuild build, Launcher launcher, BuildListener listener) {
        try {
            Map<String,Object> buildEnvironment = build.getEnvironmentWithChangeSet(listener);
            Map config = new GroovyYamlTemplateProcessor(getDeployBoyYml(build), buildEnvironment).getConfig();
            DeployBoyBuildConfiguration.GHDeploymentConfiguration deployBoyBuildConfiguration = new DeployBoyBuildConfiguration( config).getGHDeploymentConfiguration();
            setCommitStatus(build);
            GHDeployment deployment = build.getGithubRepository().createDeployment(deployBoyBuildConfiguration.getRef())
                    .autoMerge(deployBoyBuildConfiguration.isAutoMerge())
                    .payload(getPayload(build,deployBoyBuildConfiguration.getPayload()))
                    .description(deployBoyBuildConfiguration.getDescription())
                    .environment(deployBoyBuildConfiguration.getEnvironment())
                    .requiredContexts(deployBoyBuildConfiguration.getRequireContexts())
                    .create();
            listener.getLogger().println("DeployBoy: Created deployment at " + deployment.getUrl());
            return true;
        } catch (Exception e) {
           throw new RuntimeException(e);
        }finally {
           setCommitStatusBackToPending(build);
        }
    }

    protected String getPayload(DynamicBuild build, String payloadInput) throws IOException {
        String  payload = StringUtils.trimToNull(payloadInput) == null ? "{}": payloadInput;
        JSONObject payloadJson = JSONObject.fromObject(payload);
        Map<String,String> dotCiInfo =  new HashMap<String, String>();
        dotCiInfo.put("pusher",build.getCause().getPusher());
        dotCiInfo.put("pusher_avatar_url", getPusherAvatarUrl(build));
        dotCiInfo.put("url", build.getFullUrl());
        payloadJson.put("DotCi", dotCiInfo);
        return payloadJson.toString();
    }

    private String getPusherAvatarUrl(DynamicBuild build) throws IOException {
        return build.getGithubRepositoryService().getGithub().getUser(build.getCause().getPusher()).getAvatarUrl();
    }

    private void setCommitStatusBackToPending(DynamicBuild build) {
        GHRepository repository = build.getGithubRepository();

        try {
            String url = "";
            try {
                url = build.getFullUrl();
            } catch (Exception e) {
                // do nothing
                // TODO DO SOMETHING
            }
            repository.createCommitStatus(build.getSha(), GHCommitState.PENDING, url, "Build in progress", "DotCi");
        } catch (IOException e) {
            // Ignore if cannot createNotifier a pending status
        }
    }

    private void setCommitStatus(DynamicBuild build) {
        GHRepository repository = build.getGithubRepository();
        GHCommitState state;
        String msg;
        Result result = build.getResult();
        if (result.isBetterOrEqualTo(SUCCESS)) {
            state = GHCommitState.SUCCESS;
            msg = "Success";
        } else if (result.isBetterOrEqualTo(UNSTABLE)) {
            state = GHCommitState.FAILURE;
            msg = "Unstable";
        } else {
            state = GHCommitState.FAILURE;
            msg = "Failed";
        }
        try {
            repository.createCommitStatus(build.getSha(), state, build.getFullUrl(), msg,"DotCi");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getDeployBoyYml(DynamicBuild build) throws IOException {
        try {
            return build.getGithubRepositoryService().getGHFile("deploy_boy.yml", build.getSha()).getContent();
        } catch (FileNotFoundException _){
            throw new InvalidBuildConfigurationException("No deploy_boy.yml found.");
        }
    }
}
