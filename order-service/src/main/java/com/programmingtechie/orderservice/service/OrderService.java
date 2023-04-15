package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDTO;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;

    private final WebClient webClient;

    public OrderService(OrderRepository orderRepository, WebClient webClient) {
        this.orderRepository = orderRepository;
        this.webClient = webClient;
    }

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();

        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDTOList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setItems(orderLineItemsList);

        List<String> skuCodes = order.getItems().stream().map(OrderLineItems::getSkuCode).toList();


        //call inventory Service, and place order if product is in stock
        InventoryResponse[] inventoryResponses = webClient.get()
                .uri("http://localhost:8083/api/inventory", uriBuilder ->
                        uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve().bodyToMono(InventoryResponse[].class)
                .block();


        if(inventoryResponses == null) {
            throw new IllegalArgumentException("Products ate not valid");
        }

        boolean allInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::getIsInStock);


        if (allInStock) {
            orderRepository.save(order);
            return;
        }

        throw new IllegalArgumentException("Product not on stock, please try again");


    }

    private OrderLineItems mapToDto(OrderLineItemsDTO itemDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(itemDto.getPrice());
        orderLineItems.setQuantity(itemDto.getQuantity());
        orderLineItems.setSkuCode(itemDto.getSkuCode());

        return orderLineItems;
    }
}
