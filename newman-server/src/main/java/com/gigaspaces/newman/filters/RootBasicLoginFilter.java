package com.gigaspaces.newman.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RootBasicLoginFilter implements Filter {

    private static final String AUTH_PREFIX = "Basic ";
    private static final String REQUIRED_USERNAME = "root";
    private static final String REQUIRED_PASSWORD = "root";
    private static final String USERS_ENDPOINT_PREFIX = "/api/users";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Check if the request URI starts with "/api/users"
            if (httpRequest.getRequestURI().startsWith(USERS_ENDPOINT_PREFIX)) {
                // Get the Authorization header
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith(AUTH_PREFIX)) {
                    // Decode the Basic Auth credentials
                    String base64Credentials = authHeader.substring(AUTH_PREFIX.length());
                    String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);

                    // Split into username and password
                    String[] parts = credentials.split(":", 2);
                    if (parts.length == 2) {
                        String username = parts[0];
                        String password = parts[1];
                        // Validate credentials to be 'root'
                        if (REQUIRED_USERNAME.equals(username) && REQUIRED_PASSWORD.equals(password)) {
                            chain.doFilter(request, response); // Allow access
                            return;
                        }
                    }
                }

                // Reject unauthorized access
                httpResponse.setHeader("WWW-Authenticate", "Basic realm=\"Protected API\"");
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }

            // If the request is not for /api/users, allow it to pass through without authentication
            chain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {

    }
}
