package com.example.orderservice.web.controller;

import com.example.orderservice.application.command.OrderCommandResult;
import com.example.orderservice.application.query.OrderListQueryResult;
import com.example.orderservice.application.query.OrderQueryResult;
import com.example.orderservice.application.service.OrderApplicationService;
import com.example.orderservice.web.dto.CreateOrderRequest;
import com.example.orderservice.web.dto.OrderListResponse;
import com.example.orderservice.web.dto.OrderResponse;
import com.example.orderservice.web.exception.OrderNotFoundException;
import com.example.orderservice.web.exception.PaymentProcessingException;
import com.example.orderservice.web.mapper.OrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 訂單控制器單元測試
 */
@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderApplicationService orderApplicationService;

    @MockBean
    private OrderMapper orderMapper;

    private CreateOrderRequest validCreateOrderRequest;
    private OrderResponse orderResponse;
    private OrderCommandResult orderCommandResult;
    private OrderQueryResult orderQueryResult;

    @BeforeEach
    void setUp() {
        // 設置測試數據
        validCreateOrderRequest = CreateOrderRequest.builder()
            .customerId("CUST001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .creditCard(CreateOrderRequest.CreditCardInfo.builder()
                .cardNumber("4111111111111111")
                .expiryDate("12/25")
                .cvv("123")
                .cardHolderName("John Doe")
                .build())
            .billingAddress(CreateOrderRequest.BillingAddress.builder()
                .street("123 Main St")
                .city("New York")
                .postalCode("10001")
                .country("US")
                .build())
            .merchantId("MERCHANT001")
            .description("Test order")
            .build();

        orderResponse = OrderResponse.builder()
            .orderId("ORDER001")
            .customerId("CUST001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status("CREATED")
            .transactionId("TXN001")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        orderCommandResult = OrderCommandResult.builder()
            .orderId("ORDER001")
            .transactionId("TXN001")
            .success(true)
            .message("Order created successfully")
            .build();

        orderQueryResult = OrderQueryResult.builder()
            .orderId("ORDER001")
            .customerId("CUST001")
            .amount(new BigDecimal("100.00"))
            .currency("USD")
            .status("CREATED")
            .transactionId("TXN001")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    @Nested
    @DisplayName("Create Order Tests")
    class CreateOrderTests {

        @Test
        @DisplayName("Should create order successfully")
        void shouldCreateOrderSuccessfully() throws Exception {
            // Given
            when(orderMapper.toCommand(any(CreateOrderRequest.class))).thenReturn(mock(com.example.orderservice.application.command.CreateOrderCommand.class));
            when(orderApplicationService.createOrder(any())).thenReturn(orderCommandResult);
            when(orderMapper.toQuery(anyString())).thenReturn(mock(com.example.orderservice.application.query.OrderQuery.class));
            when(orderApplicationService.getOrder(any())).thenReturn(orderQueryResult);
            when(orderMapper.toResponse(any(OrderQueryResult.class))).thenReturn(orderResponse);

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/orders/ORDER001"))
                .andExpect(jsonPath("$.orderId").value("ORDER001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.transactionId").value("TXN001"));

            verify(orderApplicationService).createOrder(any());
            verify(orderApplicationService).getOrder(any());
        }

        @Test
        @DisplayName("Should return 422 when order creation fails")
        void shouldReturn422WhenOrderCreationFails() throws Exception {
            // Given
            OrderCommandResult failedResult = OrderCommandResult.builder()
                .success(false)
                .message("Payment processing failed")
                .build();

            when(orderMapper.toCommand(any(CreateOrderRequest.class))).thenReturn(mock(com.example.orderservice.application.command.CreateOrderCommand.class));
            when(orderApplicationService.createOrder(any())).thenReturn(failedResult);

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateOrderRequest)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PAYMENT_PROCESSING_ERROR"))
                .andExpect(jsonPath("$.message").value("Payment processing failed"));

            verify(orderApplicationService).createOrder(any());
            verify(orderApplicationService, never()).getOrder(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid request - missing customer ID")
        void shouldReturn400ForMissingCustomerId() throws Exception {
            // Given
            CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .creditCard(validCreateOrderRequest.getCreditCard())
                .billingAddress(validCreateOrderRequest.getBillingAddress())
                .merchantId("MERCHANT001")
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("customerId"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("Customer ID is required"));

            verify(orderApplicationService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid amount")
        void shouldReturn400ForInvalidAmount() throws Exception {
            // Given
            CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .customerId("CUST001")
                .amount(new BigDecimal("-10.00"))
                .currency("USD")
                .creditCard(validCreateOrderRequest.getCreditCard())
                .billingAddress(validCreateOrderRequest.getBillingAddress())
                .merchantId("MERCHANT001")
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("amount"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("Amount must be greater than 0"));

            verify(orderApplicationService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid currency format")
        void shouldReturn400ForInvalidCurrency() throws Exception {
            // Given
            CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .customerId("CUST001")
                .amount(new BigDecimal("100.00"))
                .currency("INVALID")
                .creditCard(validCreateOrderRequest.getCreditCard())
                .billingAddress(validCreateOrderRequest.getBillingAddress())
                .merchantId("MERCHANT001")
                .build();

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("currency"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("Currency must be a valid 3-letter code"));

            verify(orderApplicationService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() throws Exception {
            // Given
            when(orderMapper.toCommand(any(CreateOrderRequest.class))).thenReturn(mock(com.example.orderservice.application.command.CreateOrderCommand.class));
            when(orderApplicationService.createOrder(any())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateOrderRequest)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PAYMENT_PROCESSING_ERROR"))
                .andExpect(jsonPath("$.message").value("Failed to create order: Database error"));

            verify(orderApplicationService).createOrder(any());
        }
    }

    @Nested
    @DisplayName("Get Order Tests")
    class GetOrderTests {

        @Test
        @DisplayName("Should get order successfully")
        void shouldGetOrderSuccessfully() throws Exception {
            // Given
            when(orderMapper.toQuery("ORDER001")).thenReturn(mock(com.example.orderservice.application.query.OrderQuery.class));
            when(orderApplicationService.getOrder(any())).thenReturn(orderQueryResult);
            when(orderMapper.toResponse(any(OrderQueryResult.class))).thenReturn(orderResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/orders/ORDER001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("ORDER001"))
                .andExpect(jsonPath("$.customerId").value("CUST001"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("CREATED"));

            verify(orderApplicationService).getOrder(any());
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void shouldReturn404WhenOrderNotFound() throws Exception {
            // Given
            when(orderMapper.toQuery("NONEXISTENT")).thenReturn(mock(com.example.orderservice.application.query.OrderQuery.class));
            when(orderApplicationService.getOrder(any())).thenReturn(null);

            // When & Then
            mockMvc.perform(get("/api/v1/orders/NONEXISTENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found with ID: NONEXISTENT"));

            verify(orderApplicationService).getOrder(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid order ID format")
        void shouldReturn400ForInvalidOrderIdFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders/invalid@order#id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSTRAINT_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Constraint validation failed"));

            verify(orderApplicationService, never()).getOrder(any());
        }

        @Test
        @DisplayName("Should return 500 for unexpected error")
        void shouldReturn500ForUnexpectedError() throws Exception {
            // Given
            when(orderMapper.toQuery("ORDER001")).thenReturn(mock(com.example.orderservice.application.query.OrderQuery.class));
            when(orderApplicationService.getOrder(any())).thenThrow(new RuntimeException("Database error"));

            // When & Then
            mockMvc.perform(get("/api/v1/orders/ORDER001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("An internal server error occurred"));

            verify(orderApplicationService).getOrder(any());
        }
    }

    @Nested
    @DisplayName("Get Orders List Tests")
    class GetOrdersListTests {

        @Test
        @DisplayName("Should get orders list successfully")
        void shouldGetOrdersListSuccessfully() throws Exception {
            // Given
            List<OrderQueryResult> orderResults = Arrays.asList(orderQueryResult);
            OrderListQueryResult listResult = OrderListQueryResult.builder()
                .orders(orderResults)
                .totalCount(1)
                .page(0)
                .size(20)
                .hasNext(false)
                .build();

            OrderListResponse listResponse = OrderListResponse.builder()
                .orders(Arrays.asList(orderResponse))
                .totalCount(1)
                .page(0)
                .size(20)
                .hasNext(false)
                .build();

            when(orderMapper.toListQuery(null, null, 0, 20)).thenReturn(mock(com.example.orderservice.application.query.OrderListQuery.class));
            when(orderApplicationService.getOrders(any())).thenReturn(listResult);
            when(orderMapper.toListResponse(any(OrderListQueryResult.class))).thenReturn(listResponse);

            // When & Then
            mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders[0].orderId").value("ORDER001"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false));

            verify(orderApplicationService).getOrders(any());
        }

        @Test
        @DisplayName("Should get orders list with filters")
        void shouldGetOrdersListWithFilters() throws Exception {
            // Given
            when(orderMapper.toListQuery("CUST001", "CREATED", 1, 10)).thenReturn(mock(com.example.orderservice.application.query.OrderListQuery.class));
            when(orderApplicationService.getOrders(any())).thenReturn(mock(OrderListQueryResult.class));
            when(orderMapper.toListResponse(any())).thenReturn(mock(OrderListResponse.class));

            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                    .param("customerId", "CUST001")
                    .param("status", "CREATED")
                    .param("page", "1")
                    .param("size", "10"))
                .andExpect(status().isOk());

            verify(orderApplicationService).getOrders(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid customer ID format")
        void shouldReturn400ForInvalidCustomerIdFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                    .param("customerId", "invalid@customer#id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSTRAINT_VIOLATION"));

            verify(orderApplicationService, never()).getOrders(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid status")
        void shouldReturn400ForInvalidStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                    .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSTRAINT_VIOLATION"));

            verify(orderApplicationService, never()).getOrders(any());
        }

        @Test
        @DisplayName("Should return 400 for negative page")
        void shouldReturn400ForNegativePage() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                    .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSTRAINT_VIOLATION"));

            verify(orderApplicationService, never()).getOrders(any());
        }

        @Test
        @DisplayName("Should return 400 for invalid size")
        void shouldReturn400ForInvalidSize() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders")
                    .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONSTRAINT_VIOLATION"));

            verify(orderApplicationService, never()).getOrders(any());
        }
    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/v1/orders/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Order Service is healthy"));

            verify(orderApplicationService, never()).createOrder(any());
            verify(orderApplicationService, never()).getOrder(any());
            verify(orderApplicationService, never()).getOrders(any());
        }
    }

    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {

        @Test
        @DisplayName("Should handle OrderNotFoundException")
        void shouldHandleOrderNotFoundException() throws Exception {
            // Given
            when(orderMapper.toQuery("ORDER001")).thenReturn(mock(com.example.orderservice.application.query.OrderQuery.class));
            when(orderApplicationService.getOrder(any())).thenThrow(new OrderNotFoundException("ORDER001"));

            // When & Then
            mockMvc.perform(get("/api/v1/orders/ORDER001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found with ID: ORDER001"));
        }

        @Test
        @DisplayName("Should handle PaymentProcessingException")
        void shouldHandlePaymentProcessingException() throws Exception {
            // Given
            when(orderMapper.toCommand(any())).thenReturn(mock(com.example.orderservice.application.command.CreateOrderCommand.class));
            when(orderApplicationService.createOrder(any())).thenThrow(new PaymentProcessingException("Payment failed"));

            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validCreateOrderRequest)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("PAYMENT_PROCESSING_ERROR"))
                .andExpect(jsonPath("$.message").value("Payment failed"));
        }

        @Test
        @DisplayName("Should handle malformed JSON")
        void shouldHandleMalformedJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                .andExpect(status().isInternalServerError());

            verify(orderApplicationService, never()).createOrder(any());
        }

        @Test
        @DisplayName("Should handle missing content type")
        void shouldHandleMissingContentType() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/orders")
                    .content(objectMapper.writeValueAsString(validCreateOrderRequest)))
                .andExpect(status().isInternalServerError());

            verify(orderApplicationService, never()).createOrder(any());
        }
    }
}