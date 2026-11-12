package io.github.flassuu.talex.domain.usecase

import io.github.flassuu.talex.core.result.AppError
import io.github.flassuu.talex.core.result.Outcome
import io.github.flassuu.talex.domain.model.Category
import io.github.flassuu.talex.domain.model.CategoryKind
import io.github.flassuu.talex.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class CategoryUseCasesTest {

    @Test
    fun createRejectsBlankName() = runTest {
        val repository = FakeCategoryRepository()
        val result = CreateCategoryUseCase(repository)(
            CreateCategoryInput(CategoryKind.EXPENSE, "   "),
        )
        assertEquals(Outcome.Failure(AppError.NAME_REQUIRED), result)
        assertNull(repository.lastName)
    }

    @Test
    fun createRejectsNameThatIsTooLong() = runTest {
        val repository = FakeCategoryRepository()
        val result = CreateCategoryUseCase(repository)(
            CreateCategoryInput(CategoryKind.EXPENSE, "x".repeat(CreateCategoryUseCase.MAX_NAME_LENGTH + 1)),
        )
        assertEquals(Outcome.Failure(AppError.NAME_TOO_LONG), result)
        assertNull(repository.lastName)
    }

    @Test
    fun createTrimsNameAndForwardsKind() = runTest {
        val repository = FakeCategoryRepository()
        val result = CreateCategoryUseCase(repository)(
            CreateCategoryInput(CategoryKind.INCOME, "  Salary  "),
        )
        assertEquals(true, result is Outcome.Success)
        assertEquals("Salary", repository.lastName)
        assertEquals(CategoryKind.INCOME, repository.lastKind)
    }

    @Test
    fun renameRejectsBlankName() = runTest {
        val repository = FakeCategoryRepository()
        val result = RenameCategoryUseCase(repository)("id", "  ")
        assertEquals(Outcome.Failure(AppError.NAME_REQUIRED), result)
    }

    @Test
    fun renameForwardsTrimmedName() = runTest {
        val repository = FakeCategoryRepository()
        val result = RenameCategoryUseCase(repository)("id", "  Groceries  ")
        assertEquals(Outcome.Success(Unit), result)
        assertEquals("Groceries", repository.lastName)
    }

    private class FakeCategoryRepository : CategoryRepository {
        var lastName: String? = null
        var lastKind: CategoryKind? = null

        override fun observeCategories(): Flow<List<Category>> = flowOf(emptyList())
        override suspend fun get(id: String): Category? = null

        override suspend fun create(kind: CategoryKind, name: String, parentId: String?): Outcome<Category> {
            lastName = name
            lastKind = kind
            return Outcome.Success(Category(UUID.randomUUID().toString(), kind, name, parentId, archived = false))
        }

        override suspend fun rename(id: String, name: String): Outcome<Unit> {
            lastName = name
            return Outcome.Success(Unit)
        }

        override suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun setUnnecessary(id: String, unnecessary: Boolean): Outcome<Unit> =
            Outcome.Success(Unit)
        override suspend fun delete(id: String): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun seedDefaults(): Outcome<Unit> = Outcome.Success(Unit)
    }
}
