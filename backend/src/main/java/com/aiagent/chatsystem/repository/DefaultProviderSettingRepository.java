package com.aiagent.chatsystem.repository;

import com.aiagent.chatsystem.model.DefaultProviderSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefaultProviderSettingRepository extends JpaRepository<DefaultProviderSetting, String> {
}
