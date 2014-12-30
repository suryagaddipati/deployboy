deployboy
=========
#### Deploy LifeCycle
 1. Create Deploy Boy Job
 2. Add the following lines to your .ci.yml
  
   ```yaml
     plugins:
       - deployment_request
   ```
   This creates a [Deployment](https://developer.github.com/v3/repos/deployments/#create-a-deployment) on the github repo, and triggers a deployment Event.
 3. Add `deploy_bot.yml` to your repo( Version in the `ref` being deployed will be used like `.ci.yml`)
  
   Example:
   
   ```yaml
environment_image: docker-registry:80/deploy/deal_estate:latest
deployment: 
    ref: master
    task: bundle install ; bundle exec cap $deploy_env deploy
    auto_merge: false
    required_contexts: [DotCi]
    payload: "[]"
    environment: environment
    description: "deploy boy"

notifications: 
  pending: 
    - hipchat: Rapt
    - jira
  failure: 
    - hipchat: Rapt
    - Jira
  success: 
    - hipchat: Rapt
    - Jira
   ```
   
