package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.DynamicProviderRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DynamicProviderRegistrationRepository extends JpaRepository<DynamicProviderRegistration, UUID> {

    List<DynamicProviderRegistration> findAllByOrderByCreatedAtAsc();

    Optional<DynamicProviderRegistration> findByProviderKeyIgnoreCase(String providerKey);

    boolean existsByProviderKeyIgnoreCase(String providerKey);

    void deleteByProviderKeyIgnoreCase(String providerKey);
}
