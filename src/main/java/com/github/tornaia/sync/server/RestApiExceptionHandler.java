package com.github.tornaia.sync.server;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public final class RestApiExceptionHandler {

    @ExceptionHandler(value = Throwable.class)
    public static ModelAndView handle(HttpServletResponse response, Exception e) {
        return null;
    }

    private RestApiExceptionHandler() {
    }
}
