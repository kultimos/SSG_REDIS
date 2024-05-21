package com.kul.controller;

import com.kul.servier.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.models.auth.In;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Api(tags = "订单接口")
public class OrderController {

    @Resource
    private OrderService orderService;

    @ApiOperation("新增订单")
    @GetMapping("/add")
    public void add() {
        orderService.addOrder();
    }

    @ApiOperation("按照id查询订单")
    @GetMapping("/selectById/{id}")
    public String selectById(@PathVariable("id") Integer id) {
        return orderService.getOrderKey(id);
    }
}
