package com.freelanceflow.repository;

import com.freelanceflow.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByContractIdOrderByCreatedAtAsc(String contractId);
}
