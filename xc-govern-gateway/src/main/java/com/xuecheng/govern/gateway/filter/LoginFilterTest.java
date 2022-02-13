package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//@Component
public class LoginFilterTest extends ZuulFilter {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    /**
     * filterType：返回字符串代表过滤器的类型，如下
     * pre：请求在被路由之前 执行
     * routing：在路由请求时调用
     * post：在routing和errror过滤器之后调用
     * error：处理请求时发生错误调用
     */
    public String filterType() {
        return "pre";
    }

    @Override
    /**
     *  filterOrder：此方法返回整型数值，通过此数值来定义过滤器的执行顺序，数字越小优先级越高。
     */
    public int filterOrder() {
        return 0;
    }

    @Override
    /**
     * 返回一个Boolean值，判断该过滤器是否需要执行。返回true表示要执行此过虑器，否则不执 行
     */
    public boolean shouldFilter() {
        return true;
    }

    @Override
    /**
     * 过滤器的业务逻辑
     */
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response = requestContext.getResponse();
        //取出头部信息Authorization
        String authorization = request.getHeader("Authorization");

        String fromCookie = getTokenFromCookie(request);
        String jwtFromHeader = getJwtFromHeader(request);
        String key1 = "user_token:" + fromCookie;
        String value = stringRedisTemplate.opsForValue().get(key1);
        long expire = getExpire(fromCookie);

        System.out.println("jwt短令牌"+fromCookie);
        System.out.println("header信息:"+jwtFromHeader);
        System.out.println("过期时间:"+expire);
        System.out.println("从redis获取的令牌"+value);


        if(StringUtils.isEmpty(authorization)){
            requestContext.setSendZuulResponse(false); //拒绝访问
            requestContext.setResponseStatusCode(200); //错误响应代码
            //响应自定义异常
            ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
            //转换为json格式
            String string = JSON.toJSONString(responseResult);
            requestContext.setResponseBody(string);
            response.setContentType("application/json;charset=utf-8");
            return null;
        }
        return null;
    }

    //取出用户的jwt令牌
    public String getTokenFromCookie(HttpServletRequest request) {
        Map<String, String> readCookie = CookieUtil.readCookie(request, "uid");
        String access_token = readCookie.get("uid");
        if (StringUtils.isEmpty(access_token)) {
            return null;
        }
        return access_token;
    }

    //从header中查询jwt令牌
    public String getJwtFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    //查询令牌的有效时间
    public long getExpire(String access_token) {
        String key = "user_token:" + access_token;
        Long expire = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire;
    }

    //单元测试
    public void test(){
        XcUserExt xcUserExt = new XcUserExt();
        xcUserExt.setUsername("zhangsan");
        xcUserExt.setPassword("123456");
    }
}
