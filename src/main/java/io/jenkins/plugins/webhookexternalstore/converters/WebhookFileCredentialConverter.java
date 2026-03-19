package io.jenkins.plugins.webhookexternalstore.converters;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import io.jenkins.plugins.webhookexternalstore.WebhookPayload;
import io.jenkins.plugins.webhookexternalstore.exceptions.CredentialsConvertionException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.variant.OptionalExtension;

/**
 * Converter for string/token credentials using "secretText" type and {@link FileCredentialsImpl}.
 */
@OptionalExtension(requirePlugins = {"plain-credentials"})
@SuppressWarnings("unused")
public class WebhookFileCredentialConverter extends WebhookToCredentialConverter {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(WebhookFileCredentialConverter.class.getName());

    @Override
    public boolean canConvert(String type) {
        return "secretFile".equals(type);
    }

    @Override
    public IdCredentials convert(WebhookPayload payload) throws CredentialsConvertionException {
        String id = payload.getId();
        String description = payload.getDescription();

        String filename = payload.getSecretValue("filename");
        if (filename == null) {
            throw new CredentialsConvertionException("Missing required filename in secret for secretFile credentials");
        }

        String dataBase64 = payload.getSecretValue("data");
        if (dataBase64 == null) {
            throw new CredentialsConvertionException("Missing required data in secret for secretFile credentials");
        }
        String data = new String(Base64.getDecoder().decode(dataBase64), StandardCharsets.UTF_8);
        SecretBytes sb = SecretBytes.fromBytes(data.getBytes(StandardCharsets.UTF_8));
        return new FileCredentialsImpl(CredentialsScope.GLOBAL, id, description, filename, sb);
    }
}
