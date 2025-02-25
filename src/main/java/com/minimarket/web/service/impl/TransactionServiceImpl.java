package com.minimarket.web.service.impl;

import com.minimarket.web.dto.request.TransactionRequest;
import com.minimarket.web.dto.response.TransactionResponse;
import com.minimarket.web.model.product.Product;
import com.minimarket.web.model.transaction.PaymentMethod;
import com.minimarket.web.model.transaction.Transaction;
import com.minimarket.web.model.transaction.TransactionItem;
import com.minimarket.web.model.user.Customer;
import com.minimarket.web.repository.ProductRepository;
import com.minimarket.web.repository.TransactionRepository;
import com.minimarket.web.repository.UserRepository;
import com.minimarket.web.service.interfaces.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        Customer customer = (Customer) userRepository.findById(transactionRequest.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    
        Transaction transaction = new Transaction();
        transaction.setCustomer(customer);
        transaction.setPaymentMethod(PaymentMethod.valueOf(transactionRequest.getPaymentMethod().toUpperCase()));
    
        double[] total = {0.0};
    
        List<TransactionItem> items = transactionRequest.getItems().stream().map(itemRequest -> {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
    
            if (product.getStock() < itemRequest.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }
    
            product.setStock(product.getStock() - itemRequest.getQuantity());
            productRepository.save(product);
    
            double itemTotal = product.getPrice() * itemRequest.getQuantity();
            total[0] += itemTotal;
    
            TransactionItem transactionItem = new TransactionItem();
            transactionItem.setProduct(product);
            transactionItem.setQuantity(itemRequest.getQuantity());
            transactionItem.setPrice(product.getPrice());
            transactionItem.setTransaction(transaction);
    
            return transactionItem;
        }).collect(Collectors.toList());
    
        if (items.isEmpty() || total[0] <= 0) {
            throw new RuntimeException("Transaksi Tidak Valid, Tolong Pilih Item terlebih dahulu");
        }
    
        transaction.setItems(items);
        transaction.setTotal(total[0]);
    
        Transaction savedTransaction = transactionRepository.save(transaction);
    
        return mapToResponse(savedTransaction);
    }
    

    @Override
    public TransactionResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return mapToResponse(transaction);
    }

    @Override
    @Transactional
    public void updateTransactionStatus(Long id, String status) {
        System.out.println("Updating status for Transaction ID: " + id + " to " + status);
    
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    
        transaction.setStatus(status);
        transactionRepository.save(transaction);
    
        System.out.println("Transaction status updated successfully in the database.");
    }
    
       
    
    @Override
    public List<TransactionResponse> getAllTransactionsByCustomerId(Long customerId) {
        return transactionRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCustomer().getFullName(),
                transaction.getCustomer().getEmail(),
                transaction.getPaymentMethod(),
                transaction.getTotal(),
                transaction.getCreatedAt(),
                transaction.getStatus(), // Tambahkan status
                transaction.getItems().stream().map(item -> new TransactionResponse.Item(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getProduct().getImageUrl(),
                        item.getQuantity(),
                        item.getPrice(),
                        item.getPrice() * item.getQuantity()
                )).collect(Collectors.toList())
        );
    }    

}
