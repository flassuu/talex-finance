package io.github.flassuu.talex.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val currency: String,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("account_id"),
        Index("category_id"),
        Index("date"),
        Index(value = ["source_rule_id", "schedule_period"], unique = true),
    ],
)
data class EntryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val type: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    val date: String,
    val description: String,
    val payee: String? = null,
    val notes: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "transfer_group_id") val transferGroupId: String? = null,
    @ColumnInfo(name = "source_rule_id") val sourceRuleId: String? = null,
    @ColumnInfo(name = "schedule_period") val schedulePeriod: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("parent_id")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val name: String,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,
    @ColumnInfo(name = "stable_key") val stableKey: String? = null,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name_normalized"], unique = true)],
)
data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_normalized") val nameNormalized: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "entry_tags",
    primaryKeys = ["entry_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class EntryTagEntity(
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
)

data class AccountWithBalance(
    @Embedded val account: AccountEntity,
    @ColumnInfo(name = "balance_minor") val balanceMinor: Long,
)

data class EntryWithLines(
    @Embedded val entry: EntryEntity,
    @Relation(parentColumn = "id", entityColumn = "entry_id") val lines: List<EntryLineEntity>,
)

@Entity(tableName = "transfer_groups")
data class TransferGroupEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "source_entry_id") val sourceEntryId: String,
    @ColumnInfo(name = "destination_entry_id") val destinationEntryId: String,
    @ColumnInfo(name = "source_account_id") val sourceAccountId: String,
    @ColumnInfo(name = "destination_account_id") val destinationAccountId: String,
    @ColumnInfo(name = "source_amount_minor") val sourceAmountMinor: Long,
    @ColumnInfo(name = "destination_amount_minor") val destinationAmountMinor: Long,
    @ColumnInfo(name = "source_currency") val sourceCurrency: String,
    @ColumnInfo(name = "destination_currency") val destinationCurrency: String,
    val rate: String,
    val date: String,
    @ColumnInfo(name = "rate_date") val rateDate: String,
    @ColumnInfo(name = "rate_source") val rateSource: String,
    val description: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "rates",
    primaryKeys = ["base_currency", "quote_currency", "date"],
)
data class RateEntity(
    @ColumnInfo(name = "base_currency") val baseCurrency: String,
    @ColumnInfo(name = "quote_currency") val quoteCurrency: String,
    val date: String,
    val rate: String,
    val source: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entry_id"), Index("category_id")],
)
data class EntryLineEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_id") val entryId: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val note: String? = null,
    val position: Int,
)

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "account_id") val accountId: String,
    val type: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    val description: String,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    val frequency: String,
    @ColumnInfo(name = "anchor_date") val anchorDate: String,
    @ColumnInfo(name = "next_due_date") val nextDueDate: String,
    val active: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
