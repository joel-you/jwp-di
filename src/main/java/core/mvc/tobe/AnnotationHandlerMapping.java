package core.mvc.tobe;

import com.google.common.collect.Maps;
import core.annotation.web.RequestMapping;
import core.annotation.web.RequestMethod;
import core.di.factory.BeanFactory;
import core.di.factory.BeanScanner;
import core.mvc.HandlerMapping;
import core.mvc.tobe.support.ArgumentResolver;
import core.mvc.tobe.support.HttpRequestArgumentResolver;
import core.mvc.tobe.support.HttpResponseArgumentResolver;
import core.mvc.tobe.support.ModelArgumentResolver;
import core.mvc.tobe.support.PathVariableArgumentResolver;
import core.mvc.tobe.support.RequestParamArgumentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.util.ReflectionUtils.newInstance;
import static java.util.Arrays.asList;

public class AnnotationHandlerMapping implements HandlerMapping {
    private static final Logger logger = LoggerFactory.getLogger(AnnotationHandlerMapping.class);

    private static final List<ArgumentResolver> argumentResolvers = asList(
            new HttpRequestArgumentResolver(),
            new HttpResponseArgumentResolver(),
            new RequestParamArgumentResolver(),
            new PathVariableArgumentResolver(),
            new ModelArgumentResolver()
    );

    private static final ParameterNameDiscoverer nameDiscoverer = new LocalVariableTableParameterNameDiscoverer();

    private BeanFactory beanFactory;

    private Map<HandlerKey, HandlerExecution> handlerExecutions = Maps.newHashMap();

    public AnnotationHandlerMapping(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void initialize() {
        logger.info("## Initialized Annotation Handler Mapping");

        final Map<Class<?>, Object> controllers = beanFactory.getControllers();

        Map<HandlerKey, HandlerExecution> handlers = new HashMap<>();
        for (Map.Entry<Class<?>, Object> controller : controllers.entrySet()) {
            addHandlerExecution(handlers, controller.getValue(), controller.getKey().getMethods());
        }

        handlerExecutions.putAll(handlers);
    }

    private void addHandlerExecution(Map<HandlerKey, HandlerExecution> handlers, final Object target, Method[] methods) {
        Arrays.stream(methods)
                .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                .forEach(method -> {
                    RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                    HandlerKey handlerKey = new HandlerKey(requestMapping.value(), requestMapping.method());
                    HandlerExecution handlerExecution = new HandlerExecution(nameDiscoverer, argumentResolvers, target, method);
                    handlers.put(handlerKey, handlerExecution);
                    logger.info("Add - method: {}, path: {}, HandlerExecution: {}", requestMapping.method(), requestMapping.value(), method.getName());
                });
    }

    public Object getHandler(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        RequestMethod rm = RequestMethod.valueOf(request.getMethod().toUpperCase());
        logger.debug("requestUri : {}, requestMethod : {}", requestUri, rm);
        return getHandlerInternal(new HandlerKey(requestUri, rm));
    }

    private HandlerExecution getHandlerInternal(HandlerKey requestHandlerKey) {
        for (HandlerKey handlerKey : handlerExecutions.keySet()) {
            if (handlerKey.isMatch(requestHandlerKey)) {
                return handlerExecutions.get(handlerKey);
            }
        }

        return null;
    }
}
