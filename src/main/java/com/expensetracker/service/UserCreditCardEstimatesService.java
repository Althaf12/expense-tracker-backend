package com.expensetracker.service;

import com.expensetracker.dto.UserCreditCardEstimatesRequest;
import com.expensetracker.dto.UserCreditCardEstimatesResponse;
import com.expensetracker.exception.UserCreditCardEstimatesNotFoundException;
import com.expensetracker.model.UserCreditCardEstimates;
import com.expensetracker.repository.UserCreditCardEstimatesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserCreditCardEstimatesService {

    private static final Logger logger = LoggerFactory.getLogger(UserCreditCardEstimatesService.class);

    private final UserCreditCardEstimatesRepository repository;

    @Autowired
    public UserCreditCardEstimatesService(UserCreditCardEstimatesRepository repository) {
        this.repository = repository;
    }

    public List<UserCreditCardEstimatesResponse> findAll(String userId) {
        return repository.findByUserIdOrderByCardName(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public UserCreditCardEstimatesResponse add(String userId, UserCreditCardEstimatesRequest request) {
        if (request.getCardName() == null || request.getCardName().isBlank()) {
            throw new IllegalArgumentException("cardName is required");
        }

        UserCreditCardEstimates estimate = new UserCreditCardEstimates();
        estimate.setUserId(userId);
        estimate.setCardName(request.getCardName().trim());
        estimate.setExpenseName(request.getExpenseName());
        estimate.setAmount(request.getAmount());
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        UserCreditCardEstimates saved = repository.save(estimate);
        logger.info("Added credit card estimate id={} for userId={}", saved.getUserCreditCardEstimatesId(), userId);
        return toResponse(saved);
    }

    @Transactional
    public UserCreditCardEstimatesResponse update(String userId, Integer id, UserCreditCardEstimatesRequest request) {
        UserCreditCardEstimates estimate = repository.findByUserCreditCardEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new UserCreditCardEstimatesNotFoundException(id));

        if (request.getCardName() != null && !request.getCardName().isBlank()) {
            estimate.setCardName(request.getCardName().trim());
        }
        if (request.getExpenseName() != null) {
            estimate.setExpenseName(request.getExpenseName());
        }
        if (request.getAmount() != null) {
            estimate.setAmount(request.getAmount());
        }
        estimate.setLastUpdateTmstp(LocalDateTime.now());

        UserCreditCardEstimates saved = repository.save(estimate);
        logger.info("Updated credit card estimate id={} for userId={}", id, userId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(String userId, Integer id) {
        UserCreditCardEstimates estimate = repository.findByUserCreditCardEstimatesIdAndUserId(id, userId)
                .orElseThrow(() -> new UserCreditCardEstimatesNotFoundException(id));
        repository.delete(estimate);
        logger.info("Deleted credit card estimate id={} for userId={}", id, userId);
    }

    private UserCreditCardEstimatesResponse toResponse(UserCreditCardEstimates e) {
        UserCreditCardEstimatesResponse r = new UserCreditCardEstimatesResponse();
        r.setUserCreditCardEstimatesId(e.getUserCreditCardEstimatesId());
        r.setUserId(e.getUserId());
        r.setCardName(e.getCardName());
        r.setExpenseName(e.getExpenseName());
        r.setAmount(e.getAmount());
        r.setLastUpdateTmstp(e.getLastUpdateTmstp());
        return r;
    }
}

