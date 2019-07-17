package com.mm.dmp.zuulnacos.filter.body;

import com.mm.dmp.zuulnacos.Constant;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author wuweifeng wrote on 2018/11/22.
 */
@Configuration
@WebFilter(filterName = "httpServletRequestWrapperFilter", urlPatterns = "/*")
public class HttpServletRequestWrapperFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ServletRequest requestWrapper = null;

        String contentType = request.getContentType();
        if (!Constant.APP_JSON.equals(contentType)) {
            chain.doFilter(request, response);
            return;
        }
        if (request instanceof HttpServletRequest) {
            requestWrapper = new BodyReaderHttpServletRequestWrapper((HttpServletRequest) request);
        }
        if (null == requestWrapper) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }

    }

    @Override
    public void destroy() {
    }
}
