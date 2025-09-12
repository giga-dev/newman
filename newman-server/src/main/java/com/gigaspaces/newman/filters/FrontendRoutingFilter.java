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

        if (!path.startsWith("/api")    // Send API calls to the backend
                && !path.startsWith("/events")   // Send WebSocket calls to the backend
                    && !path.contains(".")) {    // Not sure what this is for (likely file names delimiter)
            // Forward all other requests to index.html for Vue.js to handle
            request.getRequestDispatcher("/index.html").forward(request, response);
        } else {
            // Proceed with static file requests, API requests and WebSocket requests
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {}
}

