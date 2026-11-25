package com.tecnocampus.LS2.protube_back.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleControllerTest {
    @Test
    void servesHello() {
        assertEquals("Hello, World!", new TestController().sayHello());
    }

    @Test
    void returnsExpectedViews() {
        var controller = new IndexController();
        assertEquals("index", controller.home().getViewName());
        assertEquals("logout", controller.logout().getViewName());
    }
}
