package com.aiagent.chatsystem.service;

import com.aiagent.chatsystem.dto.AuthResponse;
import com.aiagent.chatsystem.dto.LoginRequest;
import com.aiagent.chatsystem.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
