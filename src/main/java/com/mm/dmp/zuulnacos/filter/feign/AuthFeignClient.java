package com.mm.dmp.zuulnacos.filter.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author wuweifeng wrote on 2019/7/23.
 */
@FeignClient("auth")
public interface AuthFeignClient {

    @GetMapping(value = "/role/codes")
    String findCodesByRole(@RequestParam(name = "roleId") Long roleId);

}
