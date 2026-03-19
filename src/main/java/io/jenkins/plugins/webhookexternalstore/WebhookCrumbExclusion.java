package io.jenkins.plugins.webhookexternalstore;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Ensure that the REST URL /webhook-credentials/update is excluded from CSRF protection
 * It's using different authentication method (Shared token for webhooks)
 */
@Extension
@SuppressWarnings("unused")
public class WebhookCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String path = request.getPathInfo();
        if (path != null
                && String.format("/%s/update", WebhookEndpoint.WEBHOOK_PATH).equals(path)) {
            chain.doFilter(request, response);
            return true;
        }

        return false;
    }
}
