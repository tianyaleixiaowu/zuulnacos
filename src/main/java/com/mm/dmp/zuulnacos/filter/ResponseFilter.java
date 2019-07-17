package com.mm.dmp.zuulnacos.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;

/**
 * 返回的响应值
 * @author wuweifeng wrote on 2018/11/22.
 */
@Component
public class ResponseFilter extends ZuulFilter {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        try {
            Object zuulResponse = RequestContext.getCurrentContext().get("zuulResponse");
            if (zuulResponse != null) {
                RibbonHttpResponse resp = (RibbonHttpResponse) zuulResponse;
                String body = read(resp.getBody());
                logger.info("返回值为：");
                logger.info(body);
                logger.info("-------------------用户请求结束-----------------");
                resp.close();
                RequestContext.getCurrentContext().setResponseBody(body);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    
    private String read(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
}
