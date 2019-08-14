package com.mm.dmp.zuulnacos.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * @author wuweifeng wrote on 2018/11/22.
 */
@Component
public class IgnoreFilter extends ZuulFilter {
    @Value("${gate.ignore.startWith}")
    private String startWith;
    @Value("${gate.ignore.contain}")
    private String contain;

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest serverHttpRequest = ctx.getRequest();

        //类似于  /zuuldmp/core/test
        String requestPath = serverHttpRequest.getRequestURI();
        logger.info("请求的地址是" + requestPath);
        // 不进行拦截的地址
        if (isStartWith(requestPath) || isContains(requestPath)) {
            //不进行后续的过滤
            ctx.set("continue", false);
        } else {
            ctx.set("continue", true);
        }
        return null;
    }

    /**
     * 是否包含某种特征
     */
    private boolean isContains(String requestUri) {
        for (String s : contain.split(",")) {
            if (requestUri.contains(s.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * URI是否以什么打头
     */
    private boolean isStartWith(String requestUri) {
        for (String s : startWith.split(",")) {
            if (requestUri.startsWith(s.trim())) {
                return true;
            }
        }
        return false;
    }
}
