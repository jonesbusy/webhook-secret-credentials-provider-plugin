package io.jenkins.plugins.webhookexternalstore;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import io.jenkins.plugins.webhookexternalstore.converters.WebhookToCredentialConverter;
import io.jenkins.plugins.webhookexternalstore.exceptions.CredentialsConvertionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import jenkins.model.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

/**
 * CredentialsProvider that provides credentials received via webhooks.
 * <p>
 * This provider manages credentials that are created/updated through webhook calls
 * and makes them available throughout Jenkins.
 */
@Extension
public class WebhookCredentialsProvider extends CredentialsProvider {

    /**
     * Logger instance for this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WebhookCredentialsProvider.class.getName());

    /**
     * Memory store for webhook credentials, keyed by credential ID.
     */
    private final ConcurrentHashMap<String, IdCredentials> credentials = new ConcurrentHashMap<>();

    /**
     * Cache of CredentialsStore instances per ModelObject context.
     */
    private WebhookCredentialsStore store;

    @Override
    public synchronized CredentialsStore getStore(ModelObject object) {
        if (store == null) {
            store = new WebhookCredentialsStore(this, Jenkins.get());
        }
        return store;
    }

    private CredentialsStore getStore() {
        return getStore(Jenkins.get());
    }

    @Override
    public <C extends Credentials> List<C> getCredentialsInItemGroup(
            Class<C> type,
            ItemGroup itemGroup,
            Authentication authentication,
            List<DomainRequirement> domainRequirements) {
        ArrayList<C> list = new ArrayList<>();
        if (ACL.SYSTEM2.equals(authentication)) {
            for (IdCredentials cred : credentials.values()) {
                if (type.isInstance(cred)) {
                    list.add(type.cast(cred));
                }
            }
        }
        return list;
    }

    /**
     * Add or update a credential from a webhook payload.
     *
     * @param payload the webhook payload
     * @throws CredentialsConvertionException if the payload cannot be converted to a credential
     */
    public void addOrUpdateCredential(WebhookPayload payload) throws CredentialsConvertionException {
        LOG.debug("Processing webhook payload for credential ID: {}", payload.getId());
        IdCredentials credential = WebhookToCredentialConverter.convertFromPayload(payload);
        String credentialId = payload.getId();
        credentials.put(credentialId, credential);
        LOG.trace("Added/Updated credential with ID: {}", credentialId);
        saveStore(getStore());
        LOG.trace("Successfully added/updated credential with ID: {}", credentialId);
    }

    void setCredentials(List<IdCredentials> creds) {
        credentials.clear();
        creds.forEach(cred -> credentials.put(cred.getId(), cred));
    }

    /**
     * Get all webhook credentials.
     *
     * @return a list of all webhook sourced credentials
     */
    public List<IdCredentials> getAllWebhookCredentials() {
        return new ArrayList<>(credentials.values());
    }

    /**
     * Get the singleton instance of this provider.
     *
     * @return the webhook credentials provider instance
     */
    public static WebhookCredentialsProvider getInstance() {
        return Jenkins.get().getExtensionList(CredentialsProvider.class).stream()
                .filter(WebhookCredentialsProvider.class::isInstance)
                .map(WebhookCredentialsProvider.class::cast)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public String getIconClassName() {
        return "symbol-webhook plugin-webhook-secret-credentials-provider";
    }

    /**
     * Save the credentials store to persist changes.
     * @param store the credentials store to save
     */
    private void saveStore(CredentialsStore store) {
        try {
            store.save();
            LOG.debug("Saved credentials for store  {}", store.getDisplayName());
        } catch (IOException e) {
            LOG.error("Failed to save credentials store for {}: {}", store.getDisplayName(), e.getMessage(), e);
            throw new RuntimeException("Failed to save credentials store: " + e.getMessage(), e);
        }
    }
}
