package com.riskmanager.alert;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends MongoRepository<AlertRule, String> {

    List<AlertRule> findByServiceIdAndEnabledTrue(String serviceId);

    List<AlertRule> findByEnabledTrue();

    List<AlertRule> findByServiceId(String serviceId);
}
