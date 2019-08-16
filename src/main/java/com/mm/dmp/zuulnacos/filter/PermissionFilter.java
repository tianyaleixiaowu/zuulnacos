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
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

import static com.mm.dmp.zuulnacos.Constant.USER_ID;
import static com.mm.dmp.zuulnacos.Constant.USER_TYPE;
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
    private AuthChecker authChecker;
    @Resource
    private AuthInfoHolder authInfoHolder;
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

        //获取userId和userRole
        String userId = claims.get(USER_ID) + "";
        String userType = (String) claims.get(USER_TYPE);

        //取到该用户的role、permission
        //从自己内存读取，可能为空，说明redis里没有，就需要从auth服务读取
        Set<String> userRoles = authInfoHolder.findByUser(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            String roles = authFeignClient.findRolesByUser(Long.valueOf(userId));
            userRoles = FastJsonUtils.toBean(roles, Set.class);
        }
        if (CollectionUtils.isEmpty(userRoles)) {
            throw new NoLoginException(407, "该用户尚未分配角色");
        }
        Set<String> roleCodes = authInfoHolder.findByRole(userRoles.iterator().next());
        if (CollectionUtils.isEmpty(roleCodes)) {
            String codes = authFeignClient.findCodesByRole(Long.valueOf(userRoles.iterator().next()));
            roleCodes = FastJsonUtils.toBean(codes, Set.class);
        }

        //访问  auth 服务的 GET  /project/my 接口
        int code = authChecker.check(
                serverHttpRequest,
                userType, //这里正常应该是userRoles。但是我的业务是根据USER_TYPE在代码里作为RequireRole的。按自己的实际填写
                roleCodes);
        switch (code) {
            case CODE_NO_APP:
                throw new NoLoginException(code, "不存在的服务");
            case CODE_404:
                throw new NoLoginException(code, "无此接口或GET POST方法不对");
            case CODE_NO_ROLE:
                throw new NoLoginException(code, "用户无该接口所需role");
            case CODE_NO_CODE:
                throw new NoLoginException(code, "用户无该接口所需权限");
            case CODE_OK:
                ctx.addZuulRequestHeader(USER_ID, userId);
                ctx.addZuulRequestHeader(USER_TYPE, userType);
            default:
                break;
        }
        return null;
    }
}
