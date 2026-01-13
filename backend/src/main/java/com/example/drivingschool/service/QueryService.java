package com.example.drivingschool.service;


import com.example.drivingschool.model.Query;
import com.example.drivingschool.repository.QueryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QueryService {

    @Autowired
    private QueryRepository queryRepository;

    // Method to save a new query (used by the public intake form)
    public Query saveQuery(Query query) {
        return queryRepository.save(query);
    }

    // Method to get all queries (used by admin/staff UI)
    public List<Query> getAllQueries() {
        return queryRepository.findAll();
    }

    // Method to update a query status (used by admin/staff UI)
    public Query updateQuery(Long id, Query updatedQuery) {
        return queryRepository.findById(id).map(query -> {
            query.setFollowUpRequired(updatedQuery.isFollowUpRequired());
            query.setResolved(updatedQuery.isResolved());
            // Optionally allow updating other fields
            return queryRepository.save(query);
        }).orElse(null);
    }
}