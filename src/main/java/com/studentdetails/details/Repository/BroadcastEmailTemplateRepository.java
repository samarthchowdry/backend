package com.studentdetails.details.Repository;

import com.studentdetails.details.Domain.BroadcastEmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BroadcastEmailTemplateRepository extends JpaRepository<BroadcastEmailTemplate, Long> {

    /**
     * Returns the most recently created template based on id.
     */
    BroadcastEmailTemplate findTopByOrderByIdDesc();
}


