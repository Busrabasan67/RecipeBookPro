package com.recipebookpro.domain.usecase;
import com.recipebookpro.domain.repository.AuthRepository;
public class GetCurrentUserUseCase {
    private final AuthRepository authRepository;
    public GetCurrentUserUseCase(AuthRepository authRepository) { this.authRepository = authRepository; }
    public String getUserId() { return authRepository.getCurrentUserId(); }
    public String getUserEmail() { return authRepository.getCurrentUserEmail(); }
}
