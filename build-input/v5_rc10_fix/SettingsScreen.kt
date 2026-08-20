package com.infinitygreenpower.organizerform.feature.settings

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinitygreenpower.organizerform.core.ui.components.FormCard
import com.infinitygreenpower.organizerform.core.ui.components.SectionTitle
import com.infinitygreenpower.organizerform.core.ui.theme.*
import com.infinitygreenpower.organizerform.core.localization.localText
import com.infinitygreenpower.organizerform.data.db.OrganizerDatabase
import com.infinitygreenpower.organizerform.data.preferences.AppPreferences
import com.infinitygreenpower.organizerform.data.preferences.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(app:Application):AndroidViewModel(app){
    private val prefs=AppPreferences(app);private val db=OrganizerDatabase.get(app)
    val settings=prefs.settings.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),Settings())
    val migrationReport=db.migrationReportDao().observeLatest().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
    val message=MutableStateFlow<String?>(null)
    fun save(value:Settings)=viewModelScope.launch{prefs.save(value)}
    fun setLanguage(value:String)=viewModelScope.launch{
        // Compose observes this preference at the app root and switches language without
        // recreating the Activity, preventing the black-screen transition.
        prefs.update { setLanguage(value) }
    }
    fun importLogo(uri:Uri)=viewModelScope.launch{val context=getApplication<Application>();val dir=File(context.filesDir,"branding").apply{mkdirs()};val file=File(dir,"company-logo");runCatching{val input=context.contentResolver.openInputStream(uri)?:error("Logo stream unavailable");input.use{source->file.outputStream().use(source::copyTo)};prefs.save(settings.value.copy(logoPath=file.absolutePath,showCompanyLogo=true))}.onSuccess{message.value="Logo imported"}.onFailure{message.value="Could not import the selected logo"}}
    fun removeLogo()=viewModelScope.launch{val oldPath=settings.value.logoPath;runCatching{prefs.save(settings.value.copy(logoPath=null));oldPath?.let{File(it).delete()}}.onSuccess{message.value="Logo removed"}.onFailure{message.value="Could not remove the logo"}}
    fun clearFavorites()=viewModelScope.launch{db.catalogDao().clearFavorites()};fun clearRecent()=viewModelScope.launch{db.catalogDao().clearRecent()}
}

@Composable fun SettingsScreen(vm:SettingsViewModel= viewModel()){
    val s by vm.settings.collectAsStateWithLifecycle();val migration by vm.migrationReport.collectAsStateWithLifecycle();val message by vm.message.collectAsStateWithLifecycle();val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){it?.let(vm::importLogo)};val context=LocalContext.current;val versionName=remember(context){runCatching{context.packageManager.getPackageInfo(context.packageName,0).versionName}.getOrNull().orEmpty().ifBlank{"unknown"}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp,48.dp,18.dp,24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{Text(localText("Settings","الإعدادات","ڕێکخستنەکان"),fontSize=28.sp,fontWeight=FontWeight.SemiBold,color=Navy);message?.let{Text(it,color=InfinityTeal,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))}}
        item{FormCard{SectionTitle(localText("Company Identity","هوية الشركة","ناسنامەی کۆمپانیا"));SettingField(s.companyName,{vm.save(s.copy(companyName=it))},localText("Company name","اسم الشركة","ناوی کۆمپانیا"));SettingField(s.pdfTitle,{vm.save(s.copy(pdfTitle=it))},localText("PDF form title","عنوان استمارة PDF","ناونیشانی فۆرمی PDF"));s.logoPath?.let{path->val logo=remember(path){decodeSettingsLogo(path)};logo?.let{Image(it.asImageBitmap(),null,Modifier.height(80.dp).fillMaxWidth())}};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton({picker.launch("image/*")}){Text(localText("Choose Logo","اختيار الشعار","لۆگۆ هەڵبژێرە"))};if(s.logoPath!=null)TextButton(vm::removeLogo){Text(localText("Remove Logo","إزالة الشعار","لۆگۆ بسڕەوە"),color=Danger)}}}}
        item{FormCard{SectionTitle(localText("Language & Currency","اللغة والعملة","زمان و دراو"));Text(localText("App & PDF language","لغة التطبيق و PDF","زمانی ئەپ و PDF"),Modifier.padding(top=10.dp));CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf("en" to "English","ar" to "العربية","ckb" to "کوردی").forEachIndexed{i,(code,label)->val selected=s.language==code;SegmentedButton(selected,{vm.setLanguage(code)},SegmentedButtonDefaults.itemShape(i,3),Modifier.weight(1f).heightIn(min=56.dp),icon={}){Column(Modifier.fillMaxWidth(),horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally){Text(label,fontSize=11.sp,maxLines=1,textAlign=TextAlign.Center);Text(if(selected) "✓" else " ",fontSize=11.sp,lineHeight=11.sp,textAlign=TextAlign.Center)}}}}};SettingField(s.currency,{vm.save(s.copy(currency=it.uppercase()))},localText("Currency code","رمز العملة","کۆدی دراو"))}}
        item{FormCard{SectionTitle(localText("PDF Content","محتوى PDF","ناوەڕۆکی PDF"));SwitchRow(localText("Show item prices","إظهار أسعار المواد","نرخی ماددە پیشان بدە"),s.showPrices){vm.save(s.copy(showPrices=it))};SwitchRow(localText("Show estimated total","إظهار المجموع التقديري","کۆی خەمڵاندراو پیشان بدە"),s.showEstimatedTotal){vm.save(s.copy(showEstimatedTotal=it))};SwitchRow(localText("Show company logo","إظهار شعار الشركة","لۆگۆی کۆمپانیا پیشان بدە"),s.showCompanyLogo){vm.save(s.copy(showCompanyLogo=it))};SwitchRow(localText("Show organizer details","إظهار بيانات المنظم","زانیاری ڕێکخەر پیشان بدە"),s.showOrganizer){vm.save(s.copy(showOrganizer=it))};SwitchRow(localText("Show load/custom note","إظهار التحميل والملاحظة","بار و تێبینی پیشان بدە"),s.showLoadNote){vm.save(s.copy(showLoadNote=it))};SwitchRow(localText("Include selected photos","تضمين الصور المحددة","وێنە هەڵبژێردراوەکان دابنێ"),s.includePhotos){vm.save(s.copy(includePhotos=it))}}}
        item{FormCard{SectionTitle(localText("Organizer & Catalog Tools","أدوات المنظم والكتالوج","ئامرازەکانی ڕێکخەر و کاتەلۆگ"));OutlinedButton(vm::clearFavorites,Modifier.fillMaxWidth()){Text(localText("Clear Favorites","مسح المفضلة","دڵخوازەکان بسڕەوە"))};OutlinedButton(vm::clearRecent,Modifier.fillMaxWidth()){Text(localText("Clear Recent","مسح الأخيرة","دواییەکان بسڕەوە"))}}}
        item{FormCard{SectionTitle(localText("Legacy Data Migration","ترحيل البيانات القديمة","گواستنەوەی داتای کۆن"));migration?.let{r->Text(migrationStatus(r.status),color=if(r.status=="FAILED"||r.status=="BLOCKED_NON_EMPTY")Danger else InfinityTeal,fontWeight=FontWeight.Bold);Text(localText("Source: ${r.sourceRecords} • Imported: ${r.importedRecords} • Skipped: ${r.skippedRecords}","المصدر: ${r.sourceRecords} • تم الاستيراد: ${r.importedRecords} • تم التخطي: ${r.skippedRecords}","سەرچاوە: ${r.sourceRecords} • هاوردەکراو: ${r.importedRecords} • تێپەڕێندراو: ${r.skippedRecords}"),fontSize=12.sp,modifier=Modifier.padding(top=6.dp));Text(r.details,fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=6.dp))}?:Text(localText("Migration check is pending.","فحص الترحيل قيد الانتظار.","پشکنینی گواستنەوە چاوەڕوانە."),color=Muted)}}
        item{FormCard{SectionTitle(localText("About","حول","دەربارە"));Text("IGP Organizer Form");Text(localText("Version $versionName","الإصدار $versionName","وەشان $versionName"),color=InfinityTeal,fontWeight=FontWeight.Bold,fontSize=12.sp)}}
    }
}
@Composable private fun SettingField(v:String,on:(String)->Unit,label:String){OutlinedTextField(v,on,Modifier.fillMaxWidth().padding(vertical=5.dp),label={Text(label)},singleLine=true,shape=RoundedCornerShape(15.dp))}
@Composable private fun SwitchRow(label:String,checked:Boolean,on:(Boolean)->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label);Switch(checked,on)}}
private fun decodeSettingsLogo(path:String):android.graphics.Bitmap?{val bounds=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true};android.graphics.BitmapFactory.decodeFile(path,bounds);if(bounds.outWidth<=0||bounds.outHeight<=0)return null;var sample=1;while(bounds.outWidth/sample>600||bounds.outHeight/sample>240)sample*=2;return android.graphics.BitmapFactory.decodeFile(path,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})}
@Composable private fun migrationStatus(status:String)=when(status){
    "COMPLETE"->localText("Migration complete","اكتمل الترحيل","گواستنەوە تەواو بوو")
    "PARTIAL"->localText("Migration complete with warnings","اكتمل الترحيل مع تحذيرات","گواستنەوە لەگەڵ ئاگادارکردنەوە تەواو بوو")
    "NO_DATA"->localText("No legacy data found","لم يتم العثور على بيانات قديمة","هیچ داتایەکی کۆن نەدۆزرایەوە")
    "BLOCKED_NON_EMPTY"->localText("Migration safely paused","تم إيقاف الترحيل بأمان","گواستنەوە بە سەلامەتی وەستێنرا")
    "FAILED"->localText("Migration needs attention","الترحيل يحتاج إلى مراجعة","گواستنەوە پێویستی بە پێداچوونەوەیە")
    else->localText("Migration in progress","الترحيل قيد التنفيذ","گواستنەوە بەردەوامە")
}
