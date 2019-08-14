package com.mm.dmp.zuulnacos.filter;

import com.mm.dmp.zuulnacos.exception.NoLoginException;
import com.mm.dmp.zuulnacos.filter.feign.AuthFeignClient;
import com.mm.dmp.zuulnacos.tool.JwtUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tianyalei.zuul.zuulauth.tool.FastJsonUtils;
import com.tianyalei.zuul.zuulauth.zuul.AuthChecker;
import com.tianyalei.zuul.zuulauth.zuul.AuthInfoHolder;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

import static com.mm.dmp.zuulnacos.Constant.*;
import static com.tianyalei.zuul.zuulauth.zuul.AuthChecker.*;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * @author wuweifeng wrote on 2019/8/12.
 */
@Component
public class PermissionFilter extends ZuulFilter {
    @Resource
    private JwtUtils jwtUtils;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private RouteLocator routeLocator;
    @Resource
    private AuthChecker authChecker;
    @Resource
    private AuthFeignClient authFeignClient;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return 2;
    }

    @Override
    public boolean shouldFilter() {
        RequestContext ctx = RequestContext.getCurrentContext();
        return (boolean) ctx.get("continue");
    }

    @Override
    public Object run() throws ZuulException {
        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest serverHttpRequest = ctx.getRequest();

        String jwtToken = serverHttpRequest.getHeader(AUTHORIZATION);
        if (jwtToken == null) {
            //没有Authorization
            throw new NoLoginException();
        }
        Claims claims = jwtUtils.getClaimByToken(jwtToken);
        if (claims == null) {
            throw new NoLoginException();
        }
        logger.info("token的过期时间是：" + (claims.getExpiration()));
        if (jwtUtils.isTokenExpired(claims.getExpiration())) {
            throw new NoLoginException();
        }

        //校验role
        String userId = claims.get(USER_ID) + "";
        String roleId = claims.get(ROLE_ID) + "";
        String userType = (String) claims.get(USER_TYPE);

        //从redis读取，可能为空，就需要从auth服务读取
        Set<String> userCodes = AuthInfoHolder.findByRole(roleId);
        if (CollectionUtils.isEmpty(userCodes)) {
            String codes = authFeignClient.findCodesByRole(Long.valueOf(roleId));
            userCodes = FastJsonUtils.toBean(codes, Set.class);
        }

        //类似于  /zuuldmp/core/test
        String requestPath = serverHttpRequest.getRequestURI();
        //获取请求的method
        String method = serverHttpRequest.getMethod().toUpperCase();
        //获取所有路由信息，找到该请求对应的appName
        List<Route> routeList = routeLocator.getRoutes();
        //Route{id='one', fullPath='/zuuldmp/auth/**', path='/**', location='auth', prefix='/zuuldmp/auth',
        String appName = null;
        String path = null;
        for (Route route : routeList) {
            if (requestPath.startsWith(route.getPrefix())) {
                //取到该请求对应的微服务名字
                appName = route.getLocation();
                path = requestPath.replace(route.getPrefix(), "");
            }
        }
        if (appName == null) {
            throw new NoLoginException(404, "不存在的服务");
        }

        //取到该用户的role、permission
        //访问  auth 服务的 GET  /project/my 接口
        int code = authChecker.check(appName,
                method,
                path,
                userType,
                userCodes);
        switch (code) {
            case CODE_NO_APP:
                throw new NoLoginException(code, "权限不够");
            case CODE_404:
                throw new NoLoginException(code, "无此接口或GET POST方法不对");
            case CODE_NO_ROLE:
                throw new NoLoginException(code, "用户无该接口所需role");
            case CODE_NO_CODE:
                throw new NoLoginException(code, "用户无该接口所需权限");
            case CODE_OK:
                ctx.addZuulRequestHeader(USER_ID, userId);
                ctx.addZuulRequestHeader(USER_TYPE, userType);
                ctx.addZuulRequestHeader(ROLE_ID, roleId);
            default:
                break;
        }
        return null;
    }
}
