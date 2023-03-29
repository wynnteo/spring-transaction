package com.example.demo;

import com.example.demo.constant.OrderStatus;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.EmailServiceImpl;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EmailServiceImpl emailService;

    @Test(expected = RuntimeException.class)
    public void testPlaceOrderWithRequiredPropagationAndException() {
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testPlaceOrderWithRequiredPropagationAndException");
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        // Add order items
        OrderItem item1 = new OrderItem();
        Product product1 = new Product();
        product1.setName("Product 1 created in testPlaceOrderWithRequiredPropagationAndException");
        product1.setPrice(BigDecimal.valueOf(10.0));
        product1.setQuantity(5);
        productRepository.save(product1);

        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        Product product2 = new Product();
        product2.setName("Product 2 created in testPlaceOrderWithRequiredPropagationAndException");
        product2.setPrice(BigDecimal.valueOf(20.0));
        product2.setQuantity(10);
        productRepository.save(product2);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(15); // Here will cause exception because quantity greater than stock.
        items.add(item2);

        order.setItems(items);
        orderService.placeOrder(order);

        // Verify that the order was not saved
        Order savedOrder = orderRepository.findById(order.getId()).orElse(null);
        assertNull(savedOrder);

        // Verify that the product 1 quantities were not updated
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElse(null);
        assertNotNull(updatedProduct1);
        assertEquals(5, updatedProduct1.getQuantity());

        // Verify that the product 2 quantities were not updated
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(10, updatedProduct2.getQuantity());
    }

    @Test(expected = RuntimeException.class)
    public void testPlaceOrderWithRequiredPropagationAndExceptionAndNoRollBack() {
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testPlaceOrderWithRequiredPropagationAndExceptionAndNoRollBack");
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = new ArrayList<>();
        // Add order items
        OrderItem item1 = new OrderItem();
        Product product1 = new Product();
        product1.setName("Product 1 created in testPlaceOrderWithRequiredPropagationAndExceptionAndNoRollBack");
        product1.setPrice(java.math.BigDecimal.valueOf(10.0));
        product1.setQuantity(5);
        productRepository.save(product1);

        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        items.add(item1);

        OrderItem item2 = new OrderItem();
        Product product2 = new Product();
        product2.setName("Product 2 created in testPlaceOrderWithRequiredPropagationAndExceptionAndNoRollBack");
        product2.setPrice(java.math.BigDecimal.valueOf(20.0));
        product2.setQuantity(10);
        productRepository.save(product2);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(15);
        items.add(item2);

        order.setItems(items);

        // Place the order without rolling back on InsufficientStockException
        orderService.placeOrderNoRollback(order);

        // Verify that the order was saved
        Optional<Order> savedOrder = orderRepository.findById(order.getId());
        assertNotNull(savedOrder);

        // Verify that the product quantities were updated for product 1
        Product updatedProduct1 = productRepository.findById(product1.getId()).orElse(null);
        assertNotNull(updatedProduct1);
        assertEquals(3, updatedProduct1.getQuantity());

        // Verify that the product quantities were not updated for product 2.
        Product updatedProduct2 = productRepository.findById(product2.getId()).orElse(null);
        assertNotNull(updatedProduct2);
        assertEquals(10, updatedProduct2.getQuantity());
    }

    @Test
    @Transactional
    public void testUpdateProductQuantityWithRequiresNewPropagationAndInnerException() {
        // Create one order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testUpdateProductQuantityWithRequiresNewPropagationAndInnerException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        try {
            OrderItem item = new OrderItem();
            item.setProduct(new Product());
            productService.updateProductQuantityWithRequiresNewPropagation(item);
        } catch (RuntimeException e) {

        }

       // Verify that the order was created
        Order savedOrder = orderRepository.findById(order.getId()).get();
        assertEquals(order.getCustomerName(), savedOrder.getCustomerName());
    }

    @Test(expected = RuntimeException.class)
    @Transactional
    public void testCreateProductWithRequiresNewPropagationAndOuterException() {
        // Create one order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testCreateProductWithRequiresNewPropagationAndOuterException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        productService.createProductWithRequiresNewPropagation();
        throw new RuntimeException("DummyException: To rollback outer transaction.");
    }

    @Test
    @Transactional
    public void testCreateProductWithSupportPropagationWithTransactionAndException() {
        // Create a new order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testCreateProductWithSupportPropagationWithTransactionAndException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        productService.createProductWithSupportsPropagationAndException();
        // roll-back everything because existing transaction exist.
    }

    @Test(expected = RuntimeException.class)
    public void testCreateProductWithSupportPropagationWithoutTransactionAndException() {
        // Create a new order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testCreateProductWithSupportPropagationWithoutTransactionAndException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        // nothing to roll-back cause no physical transaction exist.
        productService.createProductWithSupportsPropagationAndException();
    }

    @Test(expected = RuntimeException.class)
    @Transactional
    public void testCreateProductWithNotSupportedPropagationWithTransactionAndException() {
        // Create a new order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testCreateProductWithNotSupportedPropagationWithTransactionAndException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        productService.createProductWithNotSupportedPropagationWithException();

        // Order will rollback and product was created.
    }

    @Test(expected = RuntimeException.class)
    public void testCreateProductWithNotSupportedPropagationWithoutTransactionAndException() {
        // Create a new order
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testCreateProductWithNotSupportedPropagationWithoutTransactionAndException");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        productService.createProductWithNotSupportedPropagationWithException();

        // Both will order and product were created.
    }

    @Test
    public void testMandatoryPropagationWithoutTransaction() {
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testMandatoryPropagationWithoutTransaction");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);
        assertThrows(IllegalTransactionStateException.class, () -> {
                orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        });
    }

    @Test
    @Transactional
    public void testMandatoryPropagationWithTransaction() {
        Order order = new Order();
        order.setCustomerEmail("sgwebfreelancer@gmail.com");
        order.setCustomerName("testMandatoryPropagationWithTransaction");
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        orderService.updateOrderStatus(order.getId(), OrderStatus.COMPLETED);

        Order retrievedOrder = orderRepository.findById(order.getId()).get();
        assertEquals(OrderStatus.COMPLETED, retrievedOrder.getStatus());
    }

    @Test
    @Transactional
    public void testNeverPropagationWithTransaction() {
        assertThrows(IllegalTransactionStateException.class, () -> {
            orderService.deleteOrder(new Order());
        });
    }

    @Test
    public void testNeverPropagationWithoutTransaction() {
       orderService.noTransaction();
    }

//    @Test
//    @Transactional
//    public void testCreateProductWithNestedPropagation() {
//        Order order = new Order();
//        order.setCustomerEmail("sgwebfreelancer@gmail.com");
//        order.setCustomerName("testCreateProductWithNestedPropagation");
//        order.setStatus(OrderStatus.PENDING);
//        orderRepository.save(order);
//        try {
//            productService.createProductWithNestedPropagation();
//        } catch (Exception e) {
//            throw e;
//        }
//
//        // Check that the order was saved
//        Order savedOrder = orderRepository.findById(order.getId()).orElse(null);
//        assertNotNull(savedOrder);
//        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
//
//        // Check that the products were not saved
//        List<Product> products = productRepository.findAll();
//        assertTrue(products.isEmpty());
//    }
}