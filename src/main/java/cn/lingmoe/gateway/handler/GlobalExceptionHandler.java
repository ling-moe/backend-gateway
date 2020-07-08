package cn.lingmoe.gateway.handler;

import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import cn.lingmoe.common.Result.Ophilia;
import cn.lingmoe.core.exception.base.BaseException;
import cn.lingmoe.core.exception.security.ForbiddenException;
import cn.lingmoe.core.exception.security.UnauthorizedException;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.server.*;

/**
 * @author jiaxu.cui@hand-china.com 2020/5/28 下午10:01
 */
public class GlobalExceptionHandler extends DefaultErrorWebExceptionHandler {
    /**
     * Create a new {@code DefaultErrorWebExceptionHandler} instance.
     *
     * @param errorAttributes    the error attributes
     * @param resourceProperties the resources configuration properties
     * @param errorProperties    the error configuration properties
     * @param applicationContext the current application context
     */
    public GlobalExceptionHandler(ErrorAttributes errorAttributes, ResourceProperties resourceProperties, ErrorProperties errorProperties, ApplicationContext applicationContext) {
        super(errorAttributes, resourceProperties, errorProperties, applicationContext);
    }

    /**
     * 获取异常属性
     */
    @Override
    protected Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
        int status = 500;
        String code = "otherException";
        Throwable ex = super.getError(request);
        if (ex instanceof UnauthorizedException){
            status = 401;
            code = ((UnauthorizedException) ex).getCode();
        } else if (ex instanceof ForbiddenException) {
            status = 403;
            code = ((ForbiddenException) ex).getCode();
        }else if (ex instanceof BaseException) {
            status = 200;
            code = ((BaseException) ex).getCode();
        }
        Map<String, Object> map = BeanUtil.beanToMap(new Ophilia(code, ex.getMessage()));
        map.put("status", status);
        return map;
    }

    /**
     * 指定响应处理方法为JSON处理的方法
     * @param errorAttributes
     */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    /**
     * 根据code获取对应的HttpStatus
     * @param errorAttributes
     */
    @Override
    protected int getHttpStatus(Map<String, Object> errorAttributes) {
        return (int) errorAttributes.get("status");
    }
}
