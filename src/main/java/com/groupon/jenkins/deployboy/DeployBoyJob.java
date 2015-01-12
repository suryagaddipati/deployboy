package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.dynamic.build.DynamicProject;
import hudson.model.ItemGroup;

public class DeployBoyJob extends DynamicProject{
    protected DeployBoyJob(ItemGroup parent, String name) {
        super(parent, name);
    }
}
