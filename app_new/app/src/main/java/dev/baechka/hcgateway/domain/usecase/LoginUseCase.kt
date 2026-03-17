package dev.baechka.hcgateway.domain.usecase

import dev.baechka.hcgateway.data.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): Result<Unit> {
        if (username.isBlank()) {
            return Result.failure(Exception("Введите логин"))
        }
        if (password.isBlank()) {
            return Result.failure(Exception("Введите пароль"))
        }
        return authRepository.login(username, password)
    }
}
