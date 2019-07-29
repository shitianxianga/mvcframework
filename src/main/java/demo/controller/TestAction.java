package demo.controller;

import demo.service.TestService;
import demo.service.TestService1;
import mvcframework.annotation.Autowired;
import mvcframework.annotation.Controller;
import mvcframework.annotation.RequsetMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequsetMapping("test")
public class TestAction {
    @Autowired
    private TestService1 testService1;
    @RequsetMapping("query")
    public void query(HttpServletRequest req, HttpServletResponse resp) {


        try {
            resp.getWriter().write("nihao");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    }

