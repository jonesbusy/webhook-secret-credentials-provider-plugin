package io.jenkins.plugins.webhookexternalstore.converters;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Extension;
import io.jenkins.plugins.webhookexternalstore.WebhookPayload;
import io.jenkins.plugins.webhookexternalstore.exceptions.CredentialsConvertionException;
import java.util.logging.Logger;

/**
 * Converter for username/password credentials using "usernamePassword" type and {@link UsernamePasswordCredentialsImpl}.
 */
@Extension
@SuppressWarnings("unused")
public class WebhookUsernamePasswordCredentialConverter extends WebhookToCredentialConverter {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(WebhookUsernamePasswordCredentialConverter.class.getName());

    @Override
    public boolean canConvert(String type) {
        return "usernamePassword".equals(type);
    }

    @Override
    public IdCredentials convert(WebhookPayload payload) throws CredentialsConvertionException {
        String id = payload.getId();
        String description = payload.getDescription();

        String username = payload.getSecretValue("username");
        if (username == null) {
            throw new CredentialsConvertionException(
                    "Missing required username in secret for usernamePassword credentials");
        }
        String password = payload.getSecretValue("password");
        if (password == null) {
            throw new CredentialsConvertionException(
                    "Missing required password in secret for usernamePassword credentials");
        }

        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, id, description, username, password);
    }
}
