package com.gigaspaces.newman.filters;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class FrontendRoutingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // Skip API calls and other file paths (e.g., images, CSS, JS)
        if (!path.startsWith("/api") && !path.contains(".")) {
            // Forward all other requests to index.html for Vue.js to handle
            request.getRequestDispatcher("/index.html").forward(request, response);
        } else {
            // Proceed with static file requests or API requests
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {}
}

