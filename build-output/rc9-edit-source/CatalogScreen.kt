package com.infinitygreenpower.organizerform.feature.catalog

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinitygreenpower.organizerform.core.localization.localText
import com.infinitygreenpower.organizerform.core.ui.theme.*
import com.infinitygreenpower.organizerform.data.db.*
import com.infinitygreenpower.organizerform.data.preferences.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class CatalogViewModel(app: Application) : AndroidViewModel(app) {
    private val db = OrganizerDatabase.get(app)
    private val dao = db.catalogDao()
    private val preferences = AppPreferences(app)

    val catalog = dao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun favorite(item: CatalogItemEntity) = viewModelScope.launch {
        dao.setFavorite(item.id, !item.favorite)
    }

    fun saveCatalogItem(
        existing: CatalogItemEntity?,
        name: String,
        category: String,
        specification: String,
        defaultPrice: String
    ) = viewModelScope.launch {
        val cleanName = name.trim()
        val cleanCategory = category.trim()
        if (cleanName.isBlank() || cleanCategory.isBlank()) return@launch

        val price = defaultPrice.trim().ifBlank { null }
        val currency = preferences.settings.first().currency
        val base = existing ?: CatalogItemEntity(
            id = UUID.randomUUID().toString(),
            category = cleanCategory,
            name = cleanName,
            legacyDescription = specification.trim(),
            sourceVersion = 0
        )
        dao.upsert(
            base.copy(
                category = cleanCategory,
                name = cleanName,
                legacyDescription = specification.trim(),
                lastUsedUnitPrice = price,
                lastUsedCurrency = if (price == null) null else currency
            )
        )
    }

    fun addToDraft(item: CatalogItemEntity, onReady: () -> Unit) = viewModelScope.launch {
        val formDao = db.formDao()
        val existing = formDao.observeActiveDraft().first()
        val now = System.currentTimeMillis()
        val form = existing?.form ?: FormEntity(
            UUID.randomUUID().toString(),
            createdAt = now,
            updatedAt = now
        )
        val activeCurrency = existing?.items?.firstOrNull()?.currency
            ?: preferences.settings.first().currency
        val remembered = item.lastUsedUnitPrice?.takeIf { item.lastUsedCurrency == activeCurrency }
        formDao.upsert(form.copy(updatedAt = now))
        formDao.upsertItems(
            (existing?.items.orEmpty()) + FormItemEntity(
                UUID.randomUUID().toString(),
                form.id,
                item.id,
                item.name,
                item.category,
                item.legacyDescription,
                "1",
                remembered,
                activeCurrency,
                existing?.items?.size ?: 0
            )
        )
        onReady()
    }
}

internal fun filterCatalogItems(
    all: List<CatalogItemEntity>,
    query: String,
    category: String
): List<CatalogItemEntity> {
    val filtered = all.filter {
        (query.isBlank() || it.name.contains(query, true) ||
            it.category.contains(query, true) || it.legacyDescription.contains(query, true)) &&
            when (category) {
                "All" -> true
                "Favorites" -> it.favorite
                "Recent" -> it.lastUsedAt != null
                else -> it.category == category
            }
    }
    return if (category == "Recent") filtered.sortedByDescending { it.lastUsedAt } else filtered
}

@Composable
fun CatalogScreen(onOpenForm: () -> Unit, vm: CatalogViewModel = viewModel()) {
    val all by vm.catalog.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var confirm by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var editing by remember { mutableStateOf<CatalogItemEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    val categories = remember(all) {
        listOf("All", "Favorites", "Recent") + all.map { it.category }.distinct()
    }
    val filtered = remember(all, query, category) {
        filterCatalogItems(all, query, category)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 48.dp, 18.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        localText("Product Catalog", "كتالوج المنتجات", "کاتەلۆگی بەرهەم"),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy
                    )
                    Text(
                        "${all.size} ${localText("items • offline", "مادة • دون اتصال", "ماددە • ئۆفلاین")}",
                        color = Muted
                    )
                }
                FilledTonalButton(
                    onClick = { editing = null; showEditor = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(localText("Add Item", "إضافة مادة", "زیادکردنی ماددە"), fontSize = 12.sp)
                }
            }
        }
        item {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth(),
                label = { Text(localText("Search products…", "ابحث عن المنتجات…", "گەڕان بەدوای بەرهەم…")) },
                singleLine = true
            )
        }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                categories.forEach {
                    FilterChip(category == it, { category = it }, { Text(it) })
                }
            }
        }
        items(filtered, key = { it.id }) { x ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                ListItem(
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(x.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Column {
                            Text(x.category)
                            if (x.legacyDescription.isNotBlank()) {
                                Text(x.legacyDescription, color = Muted, fontSize = 11.sp, maxLines = 2)
                            }
                            if (x.sourceVersion == 0) {
                                Text(localText("Custom catalog item", "مادة كتالوج مخصصة", "ماددەی تایبەتی کاتەلۆگ"), color = InfinityTeal, fontSize = 10.sp)
                            }
                        }
                    },
                    leadingContent = {
                        IconButton({ vm.favorite(x) }) {
                            Icon(
                                if (x.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                                null,
                                tint = if (x.favorite) Danger else Muted
                            )
                        }
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton({ editing = x; showEditor = true }) {
                                Icon(Icons.Outlined.Edit, localText("Edit item", "تعديل المادة", "دەستکاری ماددە"), tint = Navy)
                            }
                            Button({ confirm = x }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(localText("+ Add", "+ إضافة", "+ زیادکردن"), fontSize = 11.sp)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showEditor) {
        CatalogEditorSheet(
            item = editing,
            onDismiss = { showEditor = false; editing = null },
            onSave = { name, cat, spec, price ->
                vm.saveCatalogItem(editing, name, cat, spec, price)
                showEditor = false
                editing = null
            }
        )
    }

    confirm?.let { x ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(localText("Add to active draft?", "إضافة إلى المسودة الحالية؟", "زیادکردن بۆ ڕەشنووسی چالاک؟")) },
            text = { Text(x.name) },
            confirmButton = {
                TextButton({ vm.addToDraft(x) { confirm = null; onOpenForm() } }) {
                    Text(localText("Add & Open Form", "إضافة وفتح الاستمارة", "زیادکردن و کردنەوەی فۆرم"))
                }
            },
            dismissButton = {
                TextButton({ confirm = null }) {
                    Text(localText("Cancel", "إلغاء", "هەڵوەشاندنەوە"))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogEditorSheet(
    item: CatalogItemEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var category by remember(item?.id) { mutableStateOf(item?.category.orEmpty()) }
    var specification by remember(item?.id) { mutableStateOf(item?.legacyDescription.orEmpty()) }
    var price by remember(item?.id) { mutableStateOf(item?.lastUsedUnitPrice.orEmpty()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (item == null) localText("Add Catalog Item", "إضافة مادة إلى الكتالوج", "زیادکردنی ماددە بۆ کاتەلۆگ")
                else localText("Edit Catalog Item", "تعديل مادة الكتالوج", "دەستکاری ماددەی کاتەلۆگ"),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Navy
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(localText("Item name", "اسم المادة", "ناوی ماددە")) },
                singleLine = true
            )
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(localText("Category", "الفئة", "پۆل")) },
                singleLine = true
            )
            OutlinedTextField(
                value = specification,
                onValueChange = { specification = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(localText("Specification / Description", "المواصفات / الوصف", "تایبەتمەندی / وەسف")) },
                minLines = 3,
                maxLines = 5
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(localText("Default unit price (optional)", "سعر الوحدة الافتراضي (اختياري)", "نرخی بنەڕەتی دانە (ئارەزوومەندانە)")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onDismiss, Modifier.weight(1f)) {
                    Text(localText("Cancel", "إلغاء", "هەڵوەشاندنەوە"))
                }
                Button(
                    onClick = { onSave(name, category, specification, price) },
                    enabled = name.isNotBlank() && category.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(localText("Save Item", "حفظ المادة", "ماددە پاشەکەوت بکە"))
                }
            }
        }
    }
}
