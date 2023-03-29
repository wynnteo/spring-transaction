package com.example.demo.service;

import com.example.demo.constant.OrderStatus;
import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.exception.InsufficientStockException;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * Example of REQUIRED propagation. The method will execute within an existing transaction
     * if one exists, otherwise a new transaction will be created. This propagation option is
     * the default.
     *
     *  @param order the order to be placed
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void placeOrder(Order order) {
        // Save the order
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            // Find the product
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Update the stock quantity
            int newQuantity = product.getQuantity() - item.getQuantity();
            if (newQuantity < 0) {
                throw new RuntimeException("Insufficient stock");
            }
            product.setQuantity(newQuantity);
            productRepository.save(product);

            // Save the order item
            item.setOrder(order);
            orderItemRepository.save(item);
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    /**
     * Example of REQUIRED propagation with no rollback for a specific exception. The method will execute
     * within an existing transaction, or create a new transaction if none exists. If a RuntimeException is
     * thrown, the transaction will be rolled back, except for InsufficientStockException which will be ignored.
     *
     * @param order the order to be placed
     * @throws InsufficientStockException if there is insufficient stock for any of the products
     */
    @Transactional(propagation = Propagation.REQUIRED, noRollbackFor = {RuntimeException.class})
    public void placeOrderNoRollback(Order order){
        placeOrder(order);
    }

    /**
     * Example of REQUIRES_NEW propagation. The method will execute within a new transaction and
     * suspend the existing transaction (if any). If an exception is thrown, only the new transaction
     * will be rolled back, leaving the existing transaction (if any) unaffected. This propagation option
     * is useful when you want to ensure that a method is executed in a new transaction, regardless of the
     * transaction context of the caller
     *
     * @param item the order item to update the product quantity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProductQuantity(OrderItem item) {
        // Find the product
        Product product = productRepository.findById(item.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Update the stock quantity
        int newQuantity = product.getQuantity() - item.getQuantity();
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient stock");
        }
        product.setQuantity(newQuantity);
        productRepository.save(product);
    }

    /**
     * Example of SUPPORTS propagation. The method will execute within an existing transaction,
     * or execute non-transactionally if none exists. This propagation option is useful when you
     * want to execute a method within a transaction if one already exists, but allow it to
     * execute non-transactionally if one does not exist. This is useful for read-only methods
     * that can be executed both within and outside of a transaction.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    /**
     * Example of NOT_SUPPORTED propagation. The method will execute non-transactionally,
     * suspending any existing transaction if one exists. This propagation option is useful
     * when you want to execute a method outside the scope of any existing transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void sendEmail(Order order) {
        emailService.sendOrderConfirmation(order);
    }

    /**
     * Example of MANDATORY propagation. The method will execute within an existing transaction,
     * or throw a IllegalTransactionStateException if none exists. This propagation option is useful
     * when you want to ensure that a method is executed within a transaction and do not want
     * to allow it to be called non-transactionally.
     *
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    /**
     * Example of NEVER propagation. The current method must execute without an existing transaction.
     * If there is an existing transaction, an exception will be thrown. Otherwise, the database operations
     * are executed outside of any transaction context.
     * @param order
     */
    @Transactional(propagation = Propagation.NEVER)
    public void deleteOrder(Order order) {
        this.orderRepository.delete(order);
    }

    @Transactional(propagation = Propagation.NEVER)
    public void noTransaction() {

    }

    /**
     * Example of NESTED propagation. The method will execute within a nested transaction that is nested within
     * the current transaction, if one exists. If there is no current transaction, a new transaction will be started.
     * If an exception is thrown in the nested transaction, only the nested transaction will be rolled back.
     *
     * This propagation option is useful when you want to ensure that the work done within a method is committed only
     * if the calling method completes successfully, but you also want to handle the failure of the nested transaction
     * independently.
     *
     * @param order the order to be placed
     * @throws RuntimeException if there is insufficient stock or an error occurs while updating the product quantity
     */
    @Transactional(propagation = Propagation.NESTED)
    public void placeOrderNested(Order order) {
        // Save the order
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            // Find the product
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            // Update the stock quantity
            int newQuantity = product.getQuantity() - item.getQuantity();
            if (newQuantity < 0) {
                throw new RuntimeException("Insufficient stock");
            }
            product.setQuantity(newQuantity);
            productRepository.save(product);

            // Save the order item
            item.setOrder(order);
            orderItemRepository.save(item);

            try {
                // Update the product quantity in a new transaction
                updateProductQuantity(item);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error updating product quantity");
            }
        }
    }
}
