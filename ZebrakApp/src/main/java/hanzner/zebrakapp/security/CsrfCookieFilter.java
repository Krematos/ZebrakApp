package hanzner.zebrakapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Object csrfTokenObj = request.getAttribute(CsrfToken.class.getName());
        if (csrfTokenObj == null) {
            csrfTokenObj = request.getAttribute("_csrf");
        }
        if (csrfTokenObj == null) {
            csrfTokenObj = request.getAttribute("org.springframework.security.web.csrf.DeferredCsrfToken");
        }
        
        if (csrfTokenObj instanceof CsrfToken csrfToken) {
            csrfToken.getToken();
        } else if (csrfTokenObj instanceof Supplier<?> supplier) {
            Object supplied = supplier.get();
            if (supplied instanceof CsrfToken csrfToken) {
                csrfToken.getToken();
            }
        }
        filterChain.doFilter(request, response);
    }
}
