package io.jenkins.plugins.webhookexternalstore;

import io.jenkins.plugins.webhookexternalstore.exceptions.CredentialsConvertionException;
import java.util.Map;
import java.util.Objects;
import net.sf.json.JSONObject;

/**
 * Represents the JSON payload received via webhook for credential creation/update.
 */
public class WebhookPayload {

    private final String id;
    private final String description;
    private final String type;
    private final Map<String, Object> secret;

    /**
     * Constructor for WebhookPayload.
     * @param id ID of the credential
     * @param description Description of the credential
     * @param type Type of the credential
     * @param secret Map containing secret fields
     */
    public WebhookPayload(String id, String description, String type, Map<String, Object> secret) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.description = description;
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.secret = Objects.requireNonNull(secret, "secret cannot be null");
    }

    /**
     * Create a WebhookPayload from a JSONObject.
     *
     * @param json the JSON object
     * @return the webhook payload
     * @throws CredentialsConvertionException if required fields are missing
     */
    public static WebhookPayload fromJSON(JSONObject json) throws CredentialsConvertionException {
        String id = json.optString("id", null);
        if (id == null || id.isBlank()) {
            throw new CredentialsConvertionException("Missing required field: id");
        }
        String type = json.optString("type", null);
        if (type == null || type.isBlank()) {
            throw new CredentialsConvertionException("Missing required field: type");
        }
        String description = json.optString("description", null);
        if (description == null || description.isBlank()) {
            throw new CredentialsConvertionException("Missing required field: description");
        }

        // The secret fields
        JSONObject secretJson = json.optJSONObject("secret");
        if (secretJson == null) {
            throw new CredentialsConvertionException("Missing required object: secret");
        }

        return new WebhookPayload(id, description, type, secretJson);
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    /**
     * Return a secret value field from the secret map.
     * @param key the key to look up in the secret map
     * @return the secret value as a String, or null if not found or not a String
     */
    public String getSecretValue(String key) {
        Object value = secret.get(key);
        return (value instanceof String) ? (String) value : null;
    }

    @Override
    public String toString() {
        return "WebhookPayload{" + "type='"
                + type + '\'' + ", description='"
                + description + '\'' + ", id='"
                + id + '\'' + '}';
    }
}
