package io.jenkins.plugins.webhookexternalstore;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration for webhook credentials provider, including Bearer token authentication.
 */
@Extension
@Symbol("webhookSecretStore")
public class WebhookConfiguration extends GlobalConfiguration {

    /**
     * The token for webhook authentication.
     */
    private Secret token;

    public WebhookConfiguration() {
        load();
    }

    /**
     * Get the singleton instance of this configuration.
     *
     * @return the WebhookConfiguration instance
     */
    public static WebhookConfiguration getInstance() {
        return GlobalConfiguration.all().get(WebhookConfiguration.class);
    }

    /**
     * Get the token.
     *
     * @return the token as a Secret, or null if not configured
     */
    @SuppressWarnings("unused")
    public Secret getToken() {
        return token;
    }

    /**
     * Set the Bearer token.
     *
     * @param token the Bearer token to set
     */
    @DataBoundSetter
    @SuppressWarnings("unused")
    public void setToken(Secret token) {
        this.token = token;
        save();
    }

    /**
     * Check if a Bearer token is configured.
     *
     * @return true if a Bearer token is configured, false otherwise
     */
    public boolean isTokenConfigured() {
        return token != null && !Secret.toString(token).isBlank();
    }

    /**
     * Validate the provided Bearer token against the configured one.
     *
     * @param providedToken the token provided in the Authorization header
     * @return true if the token is valid, false otherwise
     */
    public boolean isValidBearerToken(String providedToken) {
        if (!isTokenConfigured() || providedToken == null || providedToken.isBlank()) {
            return false;
        }

        String configuredToken = Secret.toString(token);
        return configuredToken.equals(providedToken);
    }

    @Override
    public GlobalConfigurationCategory getCategory() {
        return GlobalConfigurationCategory.get(GlobalConfigurationCategory.Security.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
        req.bindJSON(this, formData);
        return super.configure(req, formData);
    }

    @Override
    public String getDisplayName() {
        return "Webhook External Secret Credentials Provider";
    }
}
