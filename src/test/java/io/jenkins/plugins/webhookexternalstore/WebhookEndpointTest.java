package io.jenkins.plugins.webhookexternalstore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.Secret;
import java.util.Collections;
import java.util.List;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class WebhookEndpointTest {

    private static final String UPDATE_PATH = "webhook-credentials/update";

    @Test
    void shouldRaise401WithoutToken(JenkinsRule jenkins) throws Exception {
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            assertEquals(401, client.postJSON(UPDATE_PATH, new JSONObject()).getStatusCode());
        }
    }

    @Test
    void shouldRaise401WithInvalidToken(JenkinsRule jenkins) throws Exception {
        // Setup config
        WebhookConfiguration config = WebhookConfiguration.getInstance();
        config.setToken(Secret.fromString("test-bearer-token-123"));
        config.save();
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.addRequestHeader("Authorization", "Bearer Invalid");
            assertEquals(401, client.postJSON(UPDATE_PATH, new JSONObject()).getStatusCode());
        }
    }

    @Test
    void shouldCreateTokenCredentials(JenkinsRule jenkins) throws Exception {

        // Setup config
        WebhookConfiguration config = WebhookConfiguration.getInstance();
        config.setToken(Secret.fromString("test-bearer-token-123"));
        config.save();

        // Username password credentials
        // language=JSON
        String payload = "{\n" + "    \"description\": \"An username password credentials\",\n"
                + "    \"id\": \"username-password-credentials\",\n"
                + "    \"secret\": {\n"
                + "        \"token\": \"theSecretTokenValue\"\n"
                + "    },\n"
                + "    \"type\": \"secretText\"\n"
                + "}";

        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.addRequestHeader("Authorization", "Bearer test-bearer-token-123");
            JenkinsRule.JSONWebResponse response = client.postJSON(UPDATE_PATH, JSONObject.fromObject(payload));

            // Assert successful response
            assertEquals(200, response.getStatusCode());

            // Verify the UsernamePasswordCredentials was created and stored
            List<StringCredentials> stringCredentials = CredentialsProvider.lookupCredentialsInItemGroup(
                    StringCredentials.class, jenkins.getInstance(), null, Collections.emptyList());

            assertEquals(1, stringCredentials.size());
            StringCredentials createdCredentials = stringCredentials.get(0);
            assertEquals("username-password-credentials", createdCredentials.getId());
            assertEquals("An username password credentials", createdCredentials.getDescription());
            assertEquals("theSecretTokenValue", createdCredentials.getSecret().getPlainText());
        }
    }

    @Test
    void shouldCreateUserNamePasswordCredentials(JenkinsRule jenkins) throws Exception {

        // Setup config
        WebhookConfiguration config = WebhookConfiguration.getInstance();
        config.setToken(Secret.fromString("test-bearer-token-123"));
        config.save();

        // Username password credentials
        // language=JSON
        String payload = "{\n" + "    \"description\": \"An username password credentials\",\n"
                + "    \"id\": \"username-password-credentials\",\n"
                + "    \"secret\": {\n"
                + "        \"password\": \"password123\",\n"
                + "        \"username\": \"userName\"\n"
                + "    },\n"
                + "    \"type\": \"usernamePassword\"\n"
                + "}";
        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.addRequestHeader("Authorization", "Bearer test-bearer-token-123");
            JenkinsRule.JSONWebResponse response = client.postJSON(UPDATE_PATH, JSONObject.fromObject(payload));

            // Assert successful response
            assertEquals(200, response.getStatusCode());

            // Verify the UsernamePasswordCredentials was created and stored
            List<UsernamePasswordCredentialsImpl> stringCredentials = CredentialsProvider.lookupCredentialsInItemGroup(
                    UsernamePasswordCredentialsImpl.class, jenkins.getInstance(), null, Collections.emptyList());

            assertEquals(1, stringCredentials.size());
            UsernamePasswordCredentialsImpl createdCredentials = stringCredentials.get(0);
            assertEquals("username-password-credentials", createdCredentials.getId());
            assertEquals("An username password credentials", createdCredentials.getDescription());
            assertEquals("userName", createdCredentials.getUsername());
            assertEquals("password123", createdCredentials.getPassword().getPlainText());
        }
    }

    @Test
    void shouldCreateBasicSSHPrivateKeyCredentials(JenkinsRule jenkins) throws Exception {
        // Setup config
        WebhookConfiguration config = WebhookConfiguration.getInstance();
        config.setToken(Secret.fromString("test-bearer-token-123"));
        config.save();

        // Basic SSH User Private Key credentials
        // language=JSON
        String payload = "{\n" + "    \"description\": \"An SSH private key credentials\",\n"
                + "    \"id\": \"ssh-private-key-credentials\",\n"
                + "    \"secret\": {\n"
                + "        \"username\": \"sshUser\",\n"
                + "        \"privateKey\": \"-----BEGIN OPENSSH PRIVATE KEY-----\\n"
                + "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\\n"
                + "QyNTUxOQAAACD6v4+ISS8l9XSyVuod+3GTdbO/VYFTuUB3MdbHvPS/TwAAAJC2EVUKthFV\\n"
                + "CgAAAAtzc2gtZWQyNTUxOQAAACD6v4+ISS8l9XSyVuod+3GTdbO/VYFTuUB3MdbHvPS/Tw\\n"
                + "AAAEDDaBwB5sI/2gDPpGtYeuKwmVzxmKAZvibatpcopOU+zPq/j4hJLyX1dLJW6h37cZN1\\n"
                + "s79VgVO5QHcx1se89L9PAAAADHZhbGRATkIyNzc2NAE=\\n"
                + "-----END OPENSSH PRIVATE KEY-----\",\n"
                + "        \"passphrase\": \"sshPassphrase\"\n"
                + "    },\n"
                + "    \"type\": \"basicSSHUserPrivateKey\"\n"
                + "}";

        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.addRequestHeader("Authorization", "Bearer test-bearer-token-123");
            JenkinsRule.JSONWebResponse response = client.postJSON(UPDATE_PATH, JSONObject.fromObject(payload));

            // Assert successful response
            assertEquals(200, response.getStatusCode());

            // Verify the BasicSSHUserPrivateKey was created and stored
            List<BasicSSHUserPrivateKey> sshCredentials = CredentialsProvider.lookupCredentialsInItemGroup(
                    BasicSSHUserPrivateKey.class, jenkins.getInstance(), null, Collections.emptyList());
            assertEquals(1, sshCredentials.size());
            BasicSSHUserPrivateKey createdCredentials = sshCredentials.get(0);
            assertEquals("ssh-private-key-credentials", createdCredentials.getId());
            assertEquals("An SSH private key credentials", createdCredentials.getDescription());
            assertEquals("sshUser", createdCredentials.getUsername());
            assertEquals(
                    "-----BEGIN OPENSSH PRIVATE KEY-----\n"
                            + "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n"
                            + "QyNTUxOQAAACD6v4+ISS8l9XSyVuod+3GTdbO/VYFTuUB3MdbHvPS/TwAAAJC2EVUKthFV\n"
                            + "CgAAAAtzc2gtZWQyNTUxOQAAACD6v4+ISS8l9XSyVuod+3GTdbO/VYFTuUB3MdbHvPS/Tw\n"
                            + "AAAEDDaBwB5sI/2gDPpGtYeuKwmVzxmKAZvibatpcopOU+zPq/j4hJLyX1dLJW6h37cZN1\n"
                            + "s79VgVO5QHcx1se89L9PAAAADHZhbGRATkIyNzc2NAE=\n"
                            + "-----END OPENSSH PRIVATE KEY-----\n",
                    createdCredentials.getPrivateKeySource().getPrivateKeys().get(0));
            assertEquals("sshPassphrase", createdCredentials.getPassphrase().getPlainText());
        }
    }

    @Test
    void shouldCreateSecretFileCredentials(JenkinsRule jenkins) throws Exception {
        // Setup config
        WebhookConfiguration config = WebhookConfiguration.getInstance();
        config.setToken(Secret.fromString("test-bearer-token-123"));
        config.save();

        // Secret File credentials
        // language=JSON
        String payload = "{\n" + "    \"description\": \"A secret file credentials\",\n"
                + "    \"id\": \"secret-file-credentials\",\n"
                + "    \"secret\": {\n"
                + "        \"filename\": \"foo.txt\",\n"
                + "        \"data\": \"Zm9vLWJhci10ZXN0\"\n"
                + "    },\n"
                + "    \"type\": \"secretFile\"\n"
                + "}";

        try (JenkinsRule.WebClient client = jenkins.createWebClient()) {
            client.addRequestHeader("Authorization", "Bearer test-bearer-token-123");
            JenkinsRule.JSONWebResponse response = client.postJSON(UPDATE_PATH, JSONObject.fromObject(payload));

            // Assert successful response
            assertEquals(200, response.getStatusCode());

            // Verify the FileCredentialsImpl was created and stored
            List<FileCredentialsImpl> fileCredentials = CredentialsProvider.lookupCredentialsInItemGroup(
                    FileCredentialsImpl.class, jenkins.getInstance(), null, Collections.emptyList());
            assertEquals(1, fileCredentials.size());
            FileCredentialsImpl createdCredentials = fileCredentials.get(0);
            assertEquals("secret-file-credentials", createdCredentials.getId());
            assertEquals("A secret file credentials", createdCredentials.getDescription());
            assertEquals("foo.txt", createdCredentials.getFileName());
            assertEquals(
                    "foo-bar-test",
                    new String(createdCredentials.getSecretBytes().getPlainData()));
        }
    }
}
