package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.repository.CategoryRepository
import javax.inject.Inject

class SeedCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): Outcome<Unit> = repository.seedDefaults()
}

data class CreateCategoryInput(
    val kind: CategoryKind,
    val name: String,
    val parentId: String? = null,
)

class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(input: CreateCategoryInput): Outcome<Category> {
        val name = input.name.trim()
        if (name.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (name.length > MAX_NAME_LENGTH) return Outcome.Failure(AppError.NAME_TOO_LONG)
        return repository.create(input.kind, name, input.parentId)
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
    }
}

class RenameCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: String, name: String): Outcome<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Failure(AppError.NAME_REQUIRED)
        if (trimmed.length > CreateCategoryUseCase.MAX_NAME_LENGTH) {
            return Outcome.Failure(AppError.NAME_TOO_LONG)
        }
        return repository.rename(id, trimmed)
    }
}
