package dev.zhipei.businessops.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JsonRequestSizeFilter extends OncePerRequestFilter {
    static final long MAX_JSON_BYTES = 64 * 1024;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain)
        throws ServletException, IOException {

        String contentType = request.getContentType();
        boolean json =
            contentType != null
                && contentType.toLowerCase().startsWith(MediaType.APPLICATION_JSON_VALUE);

        if (json && request.getContentLengthLong() > MAX_JSON_BYTES) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"Request body too large\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
