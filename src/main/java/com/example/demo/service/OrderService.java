
package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByEmail(String email) {
        return repository.findByUserEmail(email);
    }
}
