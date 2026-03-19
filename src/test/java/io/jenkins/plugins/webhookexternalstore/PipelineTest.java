package io.jenkins.plugins.webhookexternalstore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.model.Result;
import java.util.Map;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PipelineTest {

    @Test
    void testUseCredentials(JenkinsRule jenkins) throws Exception {

        // Add credentials
        WebhookPayload payload = new WebhookPayload(
                "my-secret-text", "My secret text credentials", "secretText", Map.of("token", "superSecret"));
        WebhookCredentialsProvider webhookCredentialsProvider = jenkins.jenkins
                .getExtensionList(WebhookCredentialsProvider.class)
                .get(0);
        webhookCredentialsProvider.addOrUpdateCredential(payload);

        // language=jenkinsfile
        String pipeline = "pipeline {\n" + "    agent {\n"
                + "        label('built-in')\n"
                + "    }\n"
                + "    stages {\n"
                + "        stage('Use credentials') {\n"
                + "            steps {\n"
                + "                withCredentials([string(credentialsId: 'my-secret-text', variable: 'SECRET_TEXT')]) {\n"
                + "                    echo \"The secret text is: ${SECRET_TEXT}\"\n"
                + "                }\n"
                + "            }\n"
                + "        }\n"
                + "    }\n"
                + "}";
        WorkflowJob workflowJob = jenkins.createProject(WorkflowJob.class);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = workflowJob.scheduleBuild2(0).waitForStart();
        jenkins.waitForCompletion(run1);
        assertEquals(Result.SUCCESS, run1.getResult());
        assertEquals(
                "The secret text is: ****",
                JenkinsRule.getLog(run1)
                        .lines()
                        .filter(line -> line.contains("The secret text"))
                        .findFirst()
                        .orElseThrow());
    }
}
