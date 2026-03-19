package io.jenkins.plugins.webhookexternalstore;

import hudson.Extension;
import hudson.model.UnprotectedRootAction;
import io.jenkins.plugins.webhookexternalstore.exceptions.CredentialsConvertionException;
import java.io.BufferedReader;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.POST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End point that expose /webhook-credentials/update via POST request
 */
@Extension
@SuppressWarnings("unused")
public class WebhookEndpoint implements UnprotectedRootAction {

    /**
     * Logger instance for logging webhook endpoint activities.
     */
    private static final Logger LOG = LoggerFactory.getLogger(WebhookEndpoint.class);

    /**
     * The URL path for the webhook endpoint.
     */
    static final String WEBHOOK_PATH = "webhook-credentials";

    @Override
    public String getIconFileName() {
        return null; // No icon in navigation
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return WEBHOOK_PATH;
    }

    @POST
    @SuppressWarnings({"unused", "lgtm[jenkins/no-permission-check]"}) // API protected by Bearer token
    public void doUpdate(StaplerRequest request, StaplerResponse response) throws IOException {
        try {

            // Authenticate the request
            if (!authenticateRequest(request)) {
                LOG.warn(
                        "Unauthorized webhook request from {}: missing or invalid Bearer token",
                        request.getRemoteAddr());
                response.sendError(401, "Unauthorized: Bearer token authentication required");
                return;
            }

            WebhookPayload payload = parsePayload(request);
            LOG.debug("Received authenticated webhook payload: {}", payload);

            WebhookCredentialsProvider provider = WebhookCredentialsProvider.getInstance();

            // Add or update the credential
            provider.addOrUpdateCredential(payload);
            LOG.info("Successfully processed webhook credential: id={}, type={}", payload.getId(), payload.getType());
            response.setStatus(200);

        } catch (CredentialsConvertionException e) {
            LOG.warn("Failed to convert webhook payload to credential: {}", e.getMessage());
            response.sendError(400, e.getMessage());
        } catch (Exception e) {
            LOG.error("Unexpected error processing webhook request: {}", e.getMessage(), e);
            response.sendError(500, "Unexpected error processing webhook request: " + e.getMessage());
        }
    }

    /**
     * Parse the JSON payload from the webhook request.
     *
     * @param request the HTTP request
     * @return the parsed webhook payload, or null if parsing fails
     */
    private WebhookPayload parsePayload(StaplerRequest request) throws CredentialsConvertionException {
        try {
            JSONObject json = JSONObject.fromObject(getString(request));
            return WebhookPayload.fromJSON(json);

        } catch (IOException e) {
            LOG.error("Error parsing webhook JSON payload");
            throw new CredentialsConvertionException("Error parsing webhook JSON payload");
        }
    }

    /**
     * Read the request body as a string.
     * @param request the HTTP request
     * @return the request body as a string
     * @throws CredentialsConvertionException if empty body or invalid content type
     * @throws IOException if an I/O error occurs
     */
    private static String getString(StaplerRequest request) throws CredentialsConvertionException, IOException {
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains("application/json")) {
            throw new CredentialsConvertionException(
                    "Invalid Content-Type. Expected application/json but got: " + contentType);
        }
        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }
        String jsonString = jsonBuffer.toString();
        if (jsonString.trim().isEmpty()) {
            throw new CredentialsConvertionException("Empty JSON payload in webhook request");
        }
        return jsonString;
    }

    /**
     * Authenticate the webhook request using Bearer token.
     *
     * @param request the HTTP request
     * @return true if authentication is successful, false otherwise
     */
    private boolean authenticateRequest(StaplerRequest request) {
        WebhookConfiguration config = WebhookConfiguration.getInstance();

        if (!config.isTokenConfigured()) {
            LOG.error("No Bearer token configured for webhook authentication");
            return false;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.trim().isEmpty()) {
            LOG.debug("Missing Authorization header in webhook request");
            return false;
        }

        if (!authHeader.startsWith("Bearer ")) {
            LOG.debug("Invalid Authorization header format, expected Bearer token");
            return false;
        }

        // Extract and validate token
        String providedToken = authHeader.substring("Bearer ".length()).trim();
        boolean isValid = config.isValidBearerToken(providedToken);

        if (isValid) {
            LOG.debug("Webhook request authenticated successfully");
        } else {
            LOG.debug("Invalid Bearer token provided in webhook request");
        }

        return isValid;
    }
}
