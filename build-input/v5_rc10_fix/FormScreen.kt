package com.infinitygreenpower.organizerform.feature.form

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinitygreenpower.organizerform.core.ui.components.*
import com.infinitygreenpower.organizerform.core.localization.currentAppLanguage
import com.infinitygreenpower.organizerform.core.ui.theme.*
import com.infinitygreenpower.organizerform.data.db.CatalogItemEntity
import com.infinitygreenpower.organizerform.data.db.OrganizerEntity
import java.io.File
import com.infinitygreenpower.organizerform.data.preferences.AppPreferences
import com.infinitygreenpower.organizerform.data.preferences.Settings
import com.infinitygreenpower.organizerform.export.pdf.ExportBundle
import com.infinitygreenpower.organizerform.export.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FormScreen(formId: String? = null) {
    val application = LocalContext.current.applicationContext as Application
    val vm: FormViewModel = viewModel(
        key = "form-${formId ?: "active"}",
        factory = FormViewModel.factory(application, formId)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val catalog by vm.catalog.collectAsStateWithLifecycle()
    val organizers by vm.organizers.collectAsStateWithLifecycle()
    var sheet by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<DraftItem?>(null) }
    var replacing by remember { mutableStateOf<DraftItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val lang = currentAppLanguage()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { vm.importPhotos(it) }
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); vm.clearMessage() } }
    BackHandler(enabled = state.step > 1) { vm.goTo(state.step - 1) }

    Scaffold(
        containerColor = AppBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.step <= 4) FormActions(
                step = state.step,
                lang = lang,
                onSave = vm::saveDraft,
                onBack = { if (state.step > 1) vm.goTo(state.step - 1) },
                onContinue = { if (state.step < 4) vm.goTo(state.step + 1) else vm.preview() }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 48.dp, 18.dp, 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text(if (state.step == 5) tr(lang,"Form Preview","معاينة الاستمارة","پێشبینینی فۆرم") else "${tr(lang,"New Form","استمارة جديدة","فۆرمی نوێ")} — ${tr(lang,"Step","الخطوة","هەنگاو")} ${state.step} / 4", fontSize = 27.sp, fontWeight = FontWeight.SemiBold, color = Navy)
            }
            if (state.step <= 4) item { StepProgress(state.step, lang) }
            when (state.step) {
                1 -> clientStep(state, vm, lang)
                2 -> systemStep(state, vm, lang)
                3 -> itemsStep(state, vm, lang, { replacing = null; sheet = "catalog" }, { editing = DraftItem(); sheet = "item" }, { editing = it; sheet = "item" }, { replacing = it; sheet = "catalog" })
                4 -> finishStep(state, vm, lang, { sheet = "organizer" }, { photoPicker.launch("image/*") })
                5 -> previewStep(state, vm, lang)
            }
        }
    }
    if (sheet == "catalog") CatalogSheet(catalog, lang, onDismiss = { sheet = null }) { chosen ->
        replacing?.let { old -> vm.updateItem(old.copy(catalogId = chosen.id, name = chosen.name, category = chosen.category, specification = chosen.legacyDescription)) } ?: vm.addCatalogItem(chosen)
        replacing = null; sheet = null
    }
    if (sheet == "item") ItemEditorSheet(editing ?: DraftItem(), lang, onDismiss = { sheet = null }) { item -> if (editing?.name.isNullOrBlank()) vm.addCustom(item.name, item.category, item.quantity, item.unitPrice) else vm.updateItem(item); sheet = null }
    if (sheet == "organizer") OrganizerSheet(
        organizers = organizers,
        lang = lang,
        onDismiss = { sheet = null },
        onSelect = { vm.selectOrganizer(it); sheet = null },
        onAdd = { n, p, d -> vm.saveOrganizer(n, p, d) },
        onUpdate = vm::updateOrganizer,
        onDefault = vm::setDefaultOrganizer,
        onDelete = vm::deleteOrganizer
    )
}

private fun tr(lang: String, en: String, ar: String, ku: String) = when(lang) { "ar" -> ar; "ckb" -> ku; else -> en }

@Composable private fun StepProgress(step: Int, lang: String) {
    val labels = listOf(tr(lang,"Client","العميل","کڕیار"), tr(lang,"System","المنظومة","سیستەم"), tr(lang,"Items","المواد","ماددەکان"), tr(lang,"Finish","الإنهاء","کۆتایی"))
    Row(Modifier.fillMaxWidth()) { labels.forEachIndexed { i, label -> Column(Modifier.weight(1f),horizontalAlignment = Alignment.CenterHorizontally) { Surface(shape = RoundedCornerShape(50), color = if (i + 1 <= step) InfinityTeal else Border) { Text("${i+1}", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = if (i + 1 <= step) androidx.compose.ui.graphics.Color.White else Navy) }; Text(label,maxLines=1,fontSize = 10.sp, color = if (i + 1 == step) InfinityTeal else Muted) } } }
}

private fun androidx.compose.foundation.lazy.LazyListScope.clientStep(s: FormUiState, vm: FormViewModel, lang: String) {
    item { FormCard { SectionTitle(tr(lang,"Client Details","بيانات العميل","زانیاری کڕیار")); Spacer(Modifier.height(12.dp)); Field(s.clientName,{v->vm.update{copy(clientName=v)}},tr(lang,"Client name","اسم العميل","ناوی کڕیار"),"client" in s.errors); Field(s.phone,{v->vm.update{copy(phone=v)}},tr(lang,"Phone","الهاتف","تەلەفۆن"),"phone" in s.errors,KeyboardType.Phone); Field(s.location,{v->vm.update{copy(location=v)}},tr(lang,"Location","الموقع","شوێن"),"location" in s.errors); Field(s.date,{v->vm.update{copy(date=v)}},tr(lang,"Date (YYYY-MM-DD)","التاريخ","بەروار"),"date" in s.errors) } }
}
private fun androidx.compose.foundation.lazy.LazyListScope.systemStep(s: FormUiState, vm: FormViewModel, lang: String) {
    item { FormCard { SectionTitle(tr(lang,"System Setup","إعداد المنظومة","ڕێکخستنی سیستەم")); Spacer(Modifier.height(14.dp)); Segment(listOf("Hybrid","On-Grid","Off-Grid"),s.systemType,{ systemTypeLabel(it,lang) }){vm.update{copy(systemType=it)}}; Text(tr(lang,"Quick capacity","السعة السريعة","توانای خێرا"),Modifier.padding(top=16.dp,bottom=8.dp),fontWeight=FontWeight.Medium); CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf("5","8","10","12","16","20","30").forEach{ AssistChip(onClick={vm.update{copy(capacity=it)}},label={Text("$it kW")}) } }}; Field(s.capacity,{v->vm.update{copy(capacity=v)}},tr(lang,"Required capacity (kW)","السعة المطلوبة","توانای پێویست"),"capacity" in s.errors,KeyboardType.Decimal); Segment(listOf("Single Phase","Three Phase"),s.phase,{ phaseLabel(it,lang) }){vm.update{copy(phase=it)}} } }
}
private fun androidx.compose.foundation.lazy.LazyListScope.itemsStep(s: FormUiState, vm: FormViewModel, lang: String, catalog:()->Unit, custom:()->Unit, edit:(DraftItem)->Unit, replace:(DraftItem)->Unit) {
    item { BoxWithConstraints(Modifier.fillMaxWidth()){if(maxWidth<350.dp)Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(catalog,Modifier.fillMaxWidth()){Icon(Icons.Outlined.Inventory2,null);Text(tr(lang,"Browse Catalog","تصفح الكتالوج","کاتەلۆگ"))};OutlinedButton(custom,Modifier.fillMaxWidth()){Icon(Icons.Outlined.Add,null);Text(tr(lang,"Custom Item","مادة مخصصة","ماددەی تایبەت"))}}else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){ OutlinedButton(catalog,Modifier.weight(1f)){Icon(Icons.Outlined.Inventory2,null);Text(tr(lang,"Browse Catalog","تصفح الكتالوج","کاتەلۆگ"))}; OutlinedButton(custom,Modifier.weight(1f)){Icon(Icons.Outlined.Add,null);Text(tr(lang,"Custom Item","مادة مخصصة","ماددەی تایبەت"))} }} }
    if (s.items.isEmpty()) item { FormCard(Modifier.border(if("items" in s.errors) 2.dp else 0.dp, if("items" in s.errors) Danger else androidx.compose.ui.graphics.Color.Transparent,CardShape)){Text(tr(lang,"No items yet","لا توجد مواد","هێشتا ماددە نییە"),color=if("items" in s.errors) Danger else Muted)} }
    items(s.items,key={it.id}){ x -> FormCard(Modifier.fillMaxWidth().testTag("item-row-${x.id}")){ Text(x.name,fontWeight=FontWeight.SemiBold,fontSize=16.sp); Text(x.category,color=Muted,fontSize=12.sp); Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){ Row(verticalAlignment=Alignment.CenterVertically){IconButton({vm.changeQuantity(x.id,-1)}){Icon(Icons.Outlined.Remove,null)};Text("${x.quantity}",fontWeight=FontWeight.Bold);IconButton({vm.changeQuantity(x.id,1)}){Icon(Icons.Outlined.Add,null)}}; Text(x.lineTotal?.let{"${it.stripTrailingZeros()} ${s.currency}"}?:tr(lang,"No price","بلا سعر","بێ نرخ"),color=InfinityTeal,fontWeight=FontWeight.Medium) }; Row{TextButton({edit(x)}){Text(tr(lang,"Edit","تعديل","دەستکاری"))};TextButton({replace(x)}){Text(tr(lang,"Replace","استبدال","گۆڕین"))}; TextButton({vm.removeItem(x.id)}){Text(tr(lang,"Remove","حذف","سڕینەوە"),color=Danger)}}; Field(x.unitPrice,{v->vm.updateItem(x.copy(unitPrice=v))},tr(lang,"Unit price (optional)","سعر الوحدة (اختياري)","نرخی دانە (ئارەزوومەندانە)"),false,KeyboardType.Decimal) } }
    item { Text("${tr(lang,"Estimated total","المجموع التقديري","کۆی خەمڵاندراو")}: ${s.estimatedTotal.stripTrailingZeros()} ${s.currency}",Modifier.fillMaxWidth(),color=InfinityTeal,fontWeight=FontWeight.Bold,fontSize=18.sp) }
}
private fun androidx.compose.foundation.lazy.LazyListScope.finishStep(s: FormUiState, vm: FormViewModel, lang: String, organizer:()->Unit, photos:()->Unit) {
    item { FormCard { SectionTitle(tr(lang,"Inspection Organizer","منظم الكشف","ڕێکخەری پشکنین")); Text(s.organizerName.ifBlank{tr(lang,"Not selected","غير محدد","هەڵنەبژێردراوە")},Modifier.padding(vertical=8.dp),color=if("organizer" in s.errors) Danger else Navy); OutlinedButton(organizer){Text(tr(lang,"Choose Organizer","اختيار المنظم","ڕێکخەر هەڵبژێرە"))} } }
    item { FormCard { SectionTitle(tr(lang,"Load Capacity Note","ملاحظة التحميل","تێبینی بار")); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ MiniField(s.dayFrom,{v->vm.update{copy(dayFrom=v)}},tr(lang,"Day from A","نهار من","ڕۆژ لە"),Modifier.weight(1f));MiniField(s.dayTo,{v->vm.update{copy(dayTo=v)}},tr(lang,"Day to A","نهار إلى","ڕۆژ بۆ"),Modifier.weight(1f))}; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MiniField(s.nightAmps,{v->vm.update{copy(nightAmps=v)}},tr(lang,"Night A","أمبير الليل","ئەمپێری شەو"),Modifier.weight(1f));MiniField(s.nightHours,{v->vm.update{copy(nightHours=v)}},tr(lang,"Hours","ساعات","کاتژمێر"),Modifier.weight(1f))}; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MiniField(s.emergencyAmps,{v->vm.update{copy(emergencyAmps=v)}},tr(lang,"Emergency A","أمبير الضرورة","ئەمپێری پێویست"),Modifier.weight(1f));MiniField(s.emergencyDuration,{v->vm.update{copy(emergencyDuration=v)}},tr(lang,"Duration","المدة","ماوە"),Modifier.weight(1f))}; Surface(color=InfinityTealSoft,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Text(generatedLoadNote(s,lang),Modifier.padding(12.dp),fontSize=13.sp)}; Field(s.customNote,{v->vm.update{copy(customNote=v)}},tr(lang,"Custom note (optional)","ملاحظة مخصصة (اختياري)","تێبینی تایبەت"),false,singleLine=false) } }
    item { FormCard { SectionTitle(tr(lang,"Site Photos","صور الموقع","وێنەکانی شوێن")); OutlinedButton(photos,Modifier.padding(vertical=8.dp)){Icon(Icons.Outlined.AddAPhoto,null);Text(tr(lang,"Add Photos","إضافة صور","زیادکردنی وێنە"))}; if(s.photos.isEmpty())Text(tr(lang,"No photos attached","لا توجد صور مرفقة","هیچ وێنەیەک نییە"),color=Muted) } }
    items(s.photos,key={it.id}){p-> FormCard{ Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){val bmp=remember(p.path){decodeThumbnail(p.path)}; if(bmp!=null)Image(bmp.asImageBitmap(),null,Modifier.size(72.dp));Column(Modifier.weight(1f)){Text(p.displayName);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(p.included,{vm.togglePhoto(p.id)});Text(tr(lang,"Include in PDF","تضمين في PDF","لە PDF دابنێ"))}}}; Row{IconButton({vm.movePhoto(p.id,-1)}){Icon(Icons.Outlined.ArrowUpward,null)};IconButton({vm.movePhoto(p.id,1)}){Icon(Icons.Outlined.ArrowDownward,null)};IconButton({vm.removePhoto(p.id)}){Icon(Icons.Outlined.Delete,null,tint=Danger)}} } }
}
private fun androidx.compose.foundation.lazy.LazyListScope.previewStep(s: FormUiState, vm: FormViewModel, lang: String) {
    item { FormCard { SectionTitle(tr(lang,"Client","العميل","کڕیار")); Summary(tr(lang,"Name","الاسم","ناو"),s.clientName);Summary(tr(lang,"Phone","الهاتف","تەلەفۆن"),s.phone);Summary(tr(lang,"Location","الموقع","شوێن"),s.location);Summary(tr(lang,"Date","التاريخ","بەروار"),s.date) } }
    item { FormCard { SectionTitle(tr(lang,"System & Inspection","المنظومة والكشف","سیستەم و پشکنین"));Summary(tr(lang,"System","المنظومة","سیستەم"),"${systemTypeLabel(s.systemType,lang)} • ${s.capacity} kW • ${phaseLabel(s.phase,lang)}");Summary(tr(lang,"Organizer","المنظم","ڕێکخەر"),s.organizerName) } }
    item { FormCard { SectionTitle(tr(lang,"Required Materials","المواد المطلوبة","ماددە پێویستەکان"));s.items.forEach{Summary(it.name,"${it.quantity} × ${it.unitPrice.ifBlank{"—"}}")} ;HorizontalDivider();Summary(tr(lang,"Estimated Total","المجموع التقديري","کۆی خەمڵاندراو"),"${s.estimatedTotal.stripTrailingZeros()} ${s.currency}") } }
    item { FormCard { SectionTitle(tr(lang,"Load / Notes","التحميل / الملاحظات","بار / تێبینی"));Text(generatedLoadNote(s,lang),fontSize=13.sp);if(s.customNote.isNotBlank())Text(s.customNote,Modifier.padding(top=10.dp)) } }
    item { FormCard { Summary(tr(lang,"Site photos","صور الموقع","وێنەکانی شوێن"),"${s.photos.count{it.included}} / ${s.photos.size}") } }
    item { ExportActions(s,lang) }
    item { PrimaryButton(tr(lang,"Save Form","حفظ الاستمارة","فۆرم پاشەکەوت بکە"),vm::saveForm,Modifier.fillMaxWidth()); OutlinedButton({vm.goTo(4)},Modifier.fillMaxWidth().padding(top=8.dp)){Text(tr(lang,"Back to Edit","العودة للتعديل","گەڕانەوە بۆ دەستکاری"))} }
}

@Composable
private fun ExportActions(state: FormUiState, lang: String) {
    val context = LocalContext.current
    val settings by remember(context) { AppPreferences(context).settings }
        .collectAsStateWithLifecycle(Settings())
    val scope = rememberCoroutineScope()
    var pendingPdf by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var previewPdfPath by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackSuccess by remember { mutableStateOf(false) }

    fun copyPending(path: String?, target: android.net.Uri, label: String) {
        if (path == null) {
            feedbackSuccess = false
            feedback = tr(
                lang,
                "The export is no longer available. Generate it again.",
                "لم يعد ملف التصدير متوفراً. أنشئه من جديد.",
                "فایلی هەناردە چیتر بەردەست نییە. دووبارە دروستی بکە."
            )
            return
        }
        val saved = runCatching {
            File(path).inputStream().use { input ->
                val output = context.contentResolver.openOutputStream(target, "w")
                    ?: error("Output stream unavailable")
                output.use(input::copyTo)
            }
        }.isSuccess
        feedbackSuccess = saved
        feedback = if (saved) {
            tr(lang, "$label saved successfully", "تم حفظ $label بنجاح", "$label بە سەرکەوتوویی پاشەکەوت کرا")
        } else {
            tr(lang, "Could not save $label", "تعذر حفظ $label", "نەتوانرا $label پاشەکەوت بکرێت")
        }
    }

    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        target?.let { copyPending(pendingPdf, it, "PDF") }
    }
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { target ->
        target?.let { copyPending(pendingJson, it, "JSON") }
    }

    fun generate(done: (ExportBundle) -> Unit) {
        if (validationErrors(state).isNotEmpty()) {
            feedbackSuccess = false
            feedback = tr(
                lang,
                "Complete the required fields before export.",
                "أكمل الحقول المطلوبة قبل التصدير.",
                "پێش هەناردە خانە پێویستەکان پڕبکەرەوە."
            )
            return
        }
        scope.launch {
            busy = true
            feedback = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    // The PDF always follows the saved App & PDF language.
                    PdfExporter(context).create(state, settings, settings.language)
                }
            }
            busy = false
            result.onSuccess { bundle ->
                pendingPdf = bundle.pdf.absolutePath
                pendingJson = bundle.json.absolutePath
                done(bundle)
            }.onFailure {
                feedbackSuccess = false
                feedback = tr(
                    lang,
                    "Export failed. Check storage and attached photos, then try again.",
                    "فشل التصدير. تحقق من التخزين والصور المرفقة ثم حاول مجدداً.",
                    "هەناردە سەرکەوتوو نەبوو. بیرگە و وێنەکان بپشکنە و دووبارە هەوڵبدە."
                )
            }
        }
    }

    fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    fun share(bundle: ExportBundle, multiple: Boolean = false, whatsApp: Boolean = false) {
        val intent = if (multiple) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/octet-stream"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri(bundle.pdf), uri(bundle.json)))
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri(bundle.pdf))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (whatsApp) intent.setPackage("com.whatsapp")
        runCatching {
            context.startActivity(Intent.createChooser(intent, tr(lang, "Share organizer form", "مشاركة الاستمارة", "هاوبەشکردنی فۆرم")))
        }.onFailure {
            if (whatsApp) {
                context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri(bundle.pdf))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        tr(lang, "Share PDF", "مشاركة PDF", "هاوبەشکردنی PDF")
                    )
                )
            }
        }
    }

    FormCard(Modifier.fillMaxWidth()) {
        SectionTitle(tr(lang, "Export & Share", "التصدير والمشاركة", "هەناردە و هاوبەشکردن"))
        Text(
            tr(
                lang,
                "Preview the final PDF before saving or sharing it.",
                "عاين ملف PDF النهائي قبل حفظه أو مشاركته.",
                "پێش پاشەکەوتکردن یان هاوبەشکردن PDF ـی کۆتایی ببینە."
            ),
            color = Muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
        )
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))

        Button(
            onClick = { generate { previewPdfPath = it.pdf.absolutePath } },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tr(lang, "Preview PDF", "معاينة PDF", "پێشبینینی PDF"))
        }

        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 350.dp
            Column {
                if (narrow) {
                    OutlinedButton(
                        { generate { savePdf.launch(it.pdf.name) } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr(lang, "Save PDF", "حفظ PDF", "پاشەکەوتی PDF")) }
                    OutlinedButton(
                        { generate { share(it) } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr(lang, "Share PDF", "مشاركة PDF", "هاوبەشکردنی PDF")) }
                    OutlinedButton(
                        { generate { saveJson.launch(it.json.name) } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr(lang, "Save Data", "حفظ البيانات", "پاشەکەوتی داتا")) }
                    OutlinedButton(
                        { generate { share(it, true) } },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(tr(lang, "PDF + Data", "PDF + بيانات", "PDF + داتا")) }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            { generate { savePdf.launch(it.pdf.name) } },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(tr(lang, "Save PDF", "حفظ PDF", "پاشەکەوتی PDF")) }
                        OutlinedButton(
                            { generate { share(it) } },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(tr(lang, "Share PDF", "مشاركة PDF", "هاوبەشکردنی PDF")) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            { generate { saveJson.launch(it.json.name) } },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(tr(lang, "Save Data", "حفظ البيانات", "پاشەکەوتی داتا")) }
                        OutlinedButton(
                            { generate { share(it, true) } },
                            enabled = !busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(tr(lang, "PDF + Data", "PDF + بيانات", "PDF + داتا")) }
                    }
                }
                TextButton(
                    { generate { share(it, whatsApp = true) } },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("WhatsApp PDF") }
            }
        }
        feedback?.let {
            Text(
                it,
                color = if (feedbackSuccess) InfinityTeal else Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }

    previewPdfPath?.let { path ->
        PdfPreviewDialog(path = path, lang = lang, onDismiss = { previewPdfPath = null })
    }
}

private data class PdfPageRender(val pageCount: Int, val pageIndex: Int, val bitmap: Bitmap)

private fun renderPdfPage(file: File, requestedPage: Int): PdfPageRender {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            require(renderer.pageCount > 0) { "PDF contains no pages" }
            val index = requestedPage.coerceIn(0, renderer.pageCount - 1)
            renderer.openPage(index).use { page ->
                val scale = minOf(2.2f, 1400f / page.width.toFloat()).coerceAtLeast(1f)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return PdfPageRender(renderer.pageCount, index, bitmap)
            }
        }
    }
}

@Composable
private fun PdfPreviewDialog(path: String, lang: String, onDismiss: () -> Unit) {
    var pageIndex by remember(path) { mutableIntStateOf(0) }
    var pageCount by remember(path) { mutableIntStateOf(0) }
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loading by remember(path) { mutableStateOf(true) }
    var error by remember(path) { mutableStateOf<String?>(null) }

    LaunchedEffect(path, pageIndex) {
        loading = true
        error = null
        val result = withContext(Dispatchers.IO) {
            runCatching { renderPdfPage(File(path), pageIndex) }
        }
        result.onSuccess { rendered ->
            pageCount = rendered.pageCount
            if (pageIndex != rendered.pageIndex) pageIndex = rendered.pageIndex
            bitmap = rendered.bitmap
        }.onFailure {
            bitmap = null
            error = tr(
                lang,
                "Could not render the PDF preview.",
                "تعذر عرض معاينة PDF.",
                "نەتوانرا پێشبینینی PDF پیشان بدرێت."
            )
        }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = AppBackground) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            tr(lang, "PDF Preview", "معاينة PDF", "پێشبینینی PDF"),
                            color = Navy,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (pageCount > 0) {
                            Text(
                                tr(
                                    lang,
                                    "Page ${pageIndex + 1} of $pageCount",
                                    "الصفحة ${pageIndex + 1} من $pageCount",
                                    "لاپەڕە ${pageIndex + 1} لە $pageCount"
                                ),
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(onDismiss) { Icon(Icons.Outlined.Close, contentDescription = null, tint = Navy) }
                }
                HorizontalDivider()
                Box(
                    Modifier.fillMaxWidth().weight(1f).padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loading -> CircularProgressIndicator()
                        error != null -> Text(error.orEmpty(), color = Danger)
                        bitmap != null -> Surface(
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 4.dp,
                            color = androidx.compose.ui.graphics.Color.White
                        ) {
                            Image(
                                bitmap!!.asImageBitmap(),
                                contentDescription = tr(lang, "PDF page", "صفحة PDF", "لاپەڕەی PDF"),
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                        enabled = !loading && pageIndex > 0,
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Previous", "السابق", "پێشوو")) }
                    Button(
                        onClick = { pageIndex = (pageIndex + 1).coerceAtMost((pageCount - 1).coerceAtLeast(0)) },
                        enabled = !loading && pageCount > 0 && pageIndex < pageCount - 1,
                        modifier = Modifier.weight(1f)
                    ) { Text(tr(lang, "Next", "التالي", "دواتر")) }
                }
            }
        }
    }
}

@Composable private fun FormActions(step:Int,lang:String,onSave:()->Unit,onBack:()->Unit,onContinue:()->Unit){Surface(Modifier.imePadding(),shadowElevation=6.dp){Column(Modifier.navigationBarsPadding().padding(12.dp)){OutlinedButton(onSave,Modifier.fillMaxWidth().testTag("save-draft")){Icon(Icons.Outlined.Save,null);Text(tr(lang,"Save Draft","حفظ المسودة","ڕەشنووس پاشەکەوت بکە"))};CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){if(step>1)OutlinedButton(onBack,Modifier.weight(1f)){Text(tr(lang,"Back","رجوع","گەڕانەوە"),textAlign=TextAlign.Center)};PrimaryButton(if(step==4)tr(lang,"Preview Form","معاينة الاستمارة","پێشبینینی فۆرم")else tr(lang,"Continue","متابعة","بەردەوام بە"),onContinue,Modifier.weight(1f))}}}}}
@Composable private fun Field(value:String,onValue:(String)->Unit,label:String,error:Boolean=false,keyboard:KeyboardType=KeyboardType.Text,singleLine:Boolean=true){OutlinedTextField(value,onValue,Modifier.fillMaxWidth().padding(vertical=5.dp),label={Text(label)},isError=error,singleLine=singleLine,keyboardOptions=KeyboardOptions(keyboardType=keyboard),shape=RoundedCornerShape(15.dp))}
@Composable private fun MiniField(value:String,onValue:(String)->Unit,label:String,modifier:Modifier){OutlinedTextField(value,onValue,modifier.padding(vertical=4.dp),label={Text(label,fontSize=10.sp)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true)}
@Composable
private fun Segment(
    values: List<String>,
    selected: String,
    labelFor: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            values.forEachIndexed { i, v ->
                val isSelected = selected == v
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onSelect(v) },
                    shape = SegmentedButtonDefaults.itemShape(i, values.size),
                    modifier = Modifier.weight(1f).heightIn(min = 62.dp),
                    icon = {}
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            labelFor(v),
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp,
                            maxLines = 2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            if (isSelected) "✓" else " ",
                            fontSize = 13.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
private fun systemTypeLabel(value:String,lang:String)=when(value){
    "Hybrid"->tr(lang,"Hybrid","هجين","هایبرید")
    "On-Grid"->tr(lang,"On-Grid","متصل بالشبكة","پەیوەست بە تۆڕ")
    "Off-Grid"->tr(lang,"Off-Grid","منفصل عن الشبكة","ناپەیوەست بە تۆڕ")
    else->value
}
private fun phaseLabel(value:String,lang:String)=when(value){
    "Single Phase"->tr(lang,"Single Phase","أحادي الطور","یەک فاز")
    "Three Phase"->tr(lang,"Three Phase","ثلاثي الطور","سێ فاز")
    else->value
}
@Composable private fun Summary(label:String,value:String){Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Muted);Text(value,color=Navy,fontWeight=FontWeight.Medium)}}
private fun decodeThumbnail(path:String):android.graphics.Bitmap?{val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(path,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0)return null;var sample=1;while(bounds.outWidth/sample>320||bounds.outHeight/sample>320)sample*=2;return BitmapFactory.decodeFile(path,BitmapFactory.Options().apply{inSampleSize=sample})}

@OptIn(ExperimentalMaterial3Api::class) @Composable private fun CatalogSheet(catalog:List<CatalogItemEntity>,lang:String,onDismiss:()->Unit,onAdd:(CatalogItemEntity)->Unit){var q by remember{mutableStateOf("")};ModalBottomSheet(onDismiss){Column(Modifier.fillMaxHeight(.85f).padding(16.dp)){Text(tr(lang,"Product Catalog","كتالوج المنتجات","کاتەلۆگی بەرهەم"),fontSize=22.sp,fontWeight=FontWeight.Bold);Field(q,{q=it},tr(lang,"Search","بحث","گەڕان"));LazyColumn{items(catalog.filter{q.isBlank()||it.name.contains(q,true)||it.category.contains(q,true)},key={it.id}){x->ListItem(headlineContent={Text(x.name)},supportingContent={Text(x.category)},trailingContent={IconButton({onAdd(x)}){Icon(Icons.Outlined.AddCircle,null,tint=InfinityTeal)}})}}}}}
@OptIn(ExperimentalMaterial3Api::class) @Composable private fun ItemEditorSheet(base:DraftItem,lang:String,onDismiss:()->Unit,onSave:(DraftItem)->Unit){var item by remember{mutableStateOf(base)};ModalBottomSheet(onDismiss){Column(Modifier.padding(18.dp).imePadding()){Text(tr(lang,"Add / Edit Material","إضافة / تعديل مادة","زیادکردن / دەستکاری ماددە"),fontSize=21.sp,fontWeight=FontWeight.Bold);Field(item.name,{item=item.copy(name=it)},tr(lang,"Item name","اسم المادة","ناوی ماددە"));Field(item.category,{item=item.copy(category=it)},tr(lang,"Category","الفئة","پۆل"));Field(item.quantity.toString(),{item=item.copy(quantity=it.toIntOrNull()?.coerceAtLeast(1)?:1)},tr(lang,"Quantity","الكمية","بڕ"),keyboard=KeyboardType.Number);Field(item.unitPrice,{item=item.copy(unitPrice=it)},tr(lang,"Unit price (optional)","سعر الوحدة","نرخی دانە"),keyboard=KeyboardType.Decimal);PrimaryButton(tr(lang,"Save Material","حفظ المادة","ماددە پاشەکەوت بکە"),{onSave(item)},Modifier.fillMaxWidth())}}}
@OptIn(ExperimentalMaterial3Api::class) @Composable private fun OrganizerSheet(organizers:List<OrganizerEntity>,lang:String,onDismiss:()->Unit,onSelect:(OrganizerEntity)->Unit,onAdd:(String,String,Boolean)->Unit,onUpdate:(OrganizerEntity,String,String,Boolean)->Unit,onDefault:(OrganizerEntity)->Unit,onDelete:(OrganizerEntity)->Unit){var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var makeDefault by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<OrganizerEntity?>(null)};var deleting by remember{mutableStateOf<OrganizerEntity?>(null)};fun clearEditor(){editing=null;name="";phone="";makeDefault=false};ModalBottomSheet(onDismissRequest=onDismiss){LazyColumn(Modifier.padding(18.dp).imePadding()){item{Text(tr(lang,"Choose Organizer","اختيار المنظم","ڕێکخەر هەڵبژێرە"),fontSize=21.sp,fontWeight=FontWeight.Bold)};items(organizers,key={it.id}){o->Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=androidx.compose.ui.graphics.Color.White)){Column(Modifier.padding(10.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(o.name,fontWeight=FontWeight.SemiBold);Text(o.phone.orEmpty(),fontSize=12.sp,color=Muted)};if(o.isDefault)AssistChip({},label={Text(tr(lang,"Default","افتراضي","بنەڕەت"))})};Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton({onSelect(o)}){Text(tr(lang,"Select","اختيار","هەڵبژێرە"))};IconButton({onDefault(o)}){Icon(if(o.isDefault)Icons.Outlined.Star else Icons.Outlined.StarBorder,tr(lang,"Set default","تعيين افتراضي","بنەڕەت دیاری بکە"))};IconButton({editing=o;name=o.name;phone=o.phone.orEmpty();makeDefault=o.isDefault}){Icon(Icons.Outlined.Edit,tr(lang,"Edit","تعديل","دەستکاری"))};IconButton({deleting=o}){Icon(Icons.Outlined.Delete,tr(lang,"Delete","حذف","سڕینەوە"),tint=Danger)}}}}};item{HorizontalDivider(Modifier.padding(vertical=12.dp));Text(if(editing==null)tr(lang,"Add Organizer","إضافة منظم","زیادکردنی ڕێکخەر")else tr(lang,"Edit Organizer","تعديل المنظم","دەستکاری ڕێکخەر"),fontWeight=FontWeight.Bold);Field(name,{name=it},tr(lang,"Name","الاسم","ناو"));Field(phone,{phone=it},tr(lang,"Phone (optional)","الهاتف (اختياري)","تەلەفۆن (ئارەزوومەندانە)"),keyboard=KeyboardType.Phone);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(makeDefault,{makeDefault=it});Text(tr(lang,"Use as default","استخدام كافتراضي","وەک بنەڕەت بەکاریبهێنە"))};PrimaryButton(if(editing==null)tr(lang,"Add and Select","إضافة واختيار","زیادکردن و هەڵبژاردن")else tr(lang,"Save Changes","حفظ التعديلات","گۆڕانکارییەکان پاشەکەوت بکە"),{if(name.isNotBlank()){editing?.let{onUpdate(it,name,phone,makeDefault)}?:onAdd(name,phone,makeDefault);clearEditor()}},Modifier.fillMaxWidth());if(editing!=null)TextButton(::clearEditor,Modifier.fillMaxWidth()){Text(tr(lang,"Cancel Edit","إلغاء التعديل","دەستکاری هەڵبوەشێنەوە"))}}}};deleting?.let{o->AlertDialog(onDismissRequest={deleting=null},title={Text(tr(lang,"Delete organizer?","حذف المنظم؟","ڕێکخەر بسڕدرێتەوە؟"))},text={Text(o.name)},confirmButton={TextButton({onDelete(o);deleting=null}){Text(tr(lang,"Delete","حذف","سڕینەوە"),color=Danger)}},dismissButton={TextButton({deleting=null}){Text(tr(lang,"Cancel","إلغاء","هەڵوەشاندنەوە"))}})}}
