package com.example.demo.service;

import com.example.demo.entity.OrderItem;
import com.example.demo.entity.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    /**
     * Example of REQUIRED propagation. The method will execute within an existing transaction
     * if one exists, otherwise a new transaction will be created. This propagation option is
     * the default.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Product createProductWithRequiredPropagation() {
        Product product = new Product();
        product.setName("This product created with REQUIRED propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        return productRepository.save(product);
    }

    /**
     * Example of REQUIRES_NEW propagation. The method will execute within a new transaction and
     * suspend the existing transaction (if any). If an exception is thrown, only the new transaction
     * will be rolled back, leaving the existing transaction (if any) unaffected. This propagation option
     * is useful when you want to ensure that a method is executed in a new transaction, regardless of the
     * transaction context of the caller
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Product createProductWithRequiresNewPropagation() {
        Product product = new Product();
        product.setName("This product created with REQUIRES_NEW propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        return productRepository.save(product);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProductQuantityWithRequiresNewPropagation(OrderItem item) {
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
    public Product createProductWithSupportsPropagation() {
        Product product = new Product();
        product.setName("This product created with SUPPORTS propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        return productRepository.save(product);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Product createProductWithSupportsPropagationAndException() {
        Product product = new Product();
        product.setName("This product created with SUPPORTS propagation and exception.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        productRepository.save(product);
        throw new RuntimeException("DummyException: Simulating an error");
    }

    /**
     * Example of NOT_SUPPORTED propagation. The method will execute non-transactionally,
     * suspending any existing transaction if one exists. This propagation option is useful
     * when you want to execute a method outside the scope of any existing transaction.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Product createProductWithNotSupportedPropagation() {
        Product product = new Product();
        product.setName("This product created with NOT_SUPPORTED propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        return productRepository.save(product);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createProductWithNotSupportedPropagationWithException() {
        Product product = new Product();
        product.setName("This product created with NOT_SUPPORTED propagation and exception.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        productRepository.save(product);
        throw new RuntimeException("DummyException: Simulating an error");
    }

    /**
     * Example of MANDATORY propagation. The method will execute within an existing transaction,
     * or throw a IllegalTransactionStateException if none exists. This propagation option is useful
     * when you want to ensure that a method is executed within a transaction and do not want
     * to allow it to be called non-transactionally.
     *
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Product createProductWithMandatoryPropagation() {
        Product product = new Product();
        product.setName("This product created with MANDATORY propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        return productRepository.save(product);
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
     */
    @Transactional(propagation = Propagation.NESTED)
    public void createProductWithNestedPropagation() {
        Product product = new Product();
        product.setName("This product created with NESTED propagation.");
        product.setQuantity(10);
        product.setPrice(BigDecimal.valueOf(10.0));
        productRepository.save(product);

        // Throw a runtime exception to simulate an error
        throw new RuntimeException("DummyException: Simulating an error");
    }
}
