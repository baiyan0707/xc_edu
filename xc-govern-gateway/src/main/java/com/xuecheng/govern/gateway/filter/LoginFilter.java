package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    AuthService authService;

    @Override
    public String filterType() {
        return "pre";
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
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        //取出用户的jwt令牌
        String tokenFromCookie = authService.getTokenFromCookie(request);
        if(StringUtils.isEmpty(tokenFromCookie)){
            access_denied();
            return null;
        }
        //从header中查询jwt令牌
        String jwtFromHeader = authService.getJwtFromHeader(request);
        if(StringUtils.isEmpty(jwtFromHeader)){
            access_denied();
            return null;
        }
        //查询身份是否过期
        long expire = authService.getExpire(tokenFromCookie);
        if(expire < 0){
            access_denied();
            return null;
        }
        return null;
    }

    //响应失败消息
    private void access_denied(){
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletResponse response = requestContext.getResponse();
        requestContext.setSendZuulResponse(false); //拒绝访问
        requestContext.setResponseStatusCode(200); //错误响应代码
        //响应自定义异常
        ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
        //转换为json格式
        String string = JSON.toJSONString(responseResult);
        requestContext.setResponseBody(string);
        response.setContentType("application/json;charset=utf-8");
    }
}
