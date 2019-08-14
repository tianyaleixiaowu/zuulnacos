package com.mm.dmp.zuulnacos;

/**
 * @author wuweifeng wrote on 2018/10/30.
 */
public interface Constant {
    String USER_ID = "userId";
    String ROLE_ID = "roleId";
    String USER_TYPE = "userType";

    String AUTHORIZATION = "Authorization";

    /**
     * 用户角色
     */
    String CACHE_USER_ROLE_KEY = "user_role_key";
    /**
     * 权限保存10个小时，redis存储的user权限
     */
    int CACHE_USER_ROLE_EXPIE = 10;
    /**
     * role对应的菜单缓存的key
     */
    String CACHE_ROLE_MENU_KEY = "role_menu_key";

    String CONTENT_TYPE = "content-type";
    String APP_JSON = "application/json";

}
