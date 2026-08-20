from pathlib import Path
import re
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else '/tmp/igp-build/IGP')
pkg = root / 'app/src/main/java/com/infinitygreenpower/organizerform'

# --- FormViewModel: fresh forms must not auto-resume the active draft. ---
vm = pkg / 'feature/form/FormViewModel.kt'
t = vm.read_text()
if 'private val resumeActiveDraft: Boolean = false' not in t:
    old = '    private val initialFormId: String? = null,\n    private val savedStateHandle: SavedStateHandle'
    assert old in t, 'FormViewModel constructor fields not found'
    t = t.replace(old, '    private val initialFormId: String? = null,\n    private val resumeActiveDraft: Boolean = false,\n    private val savedStateHandle: SavedStateHandle', 1)

if 'resumeActiveDraft -> forms.observeActiveDraft().first()' not in t:
    pattern = re.compile(r'\s*val aggregate = initialFormId\?\.let \{ forms\.observe\(it\)\.first\(\) \} \?: forms\.observeActiveDraft\(\)\.first\(\)')
    replacement = '''\n            val aggregate = when {\n                initialFormId != null -> forms.observe(initialFormId).first()\n                resumeActiveDraft -> forms.observeActiveDraft().first()\n                else -> null\n            }'''
    t, n = pattern.subn(replacement, t, count=1)
    assert n == 1, 'active draft initialization not found'

if 'fun factory(application: Application, formId: String?, resumeActiveDraft: Boolean = false)' not in t:
    pattern = re.compile(
        r'        fun factory\(application:\s*Application,\s*formId:\s*String\?\)\s*'
        r'(?::\s*ViewModelProvider\.Factory\s*)?=\s*viewModelFactory\s*\{\s*'
        r'initializer\s*\{\s*FormViewModel\(application,\s*formId,\s*createSavedStateHandle\(\)\)\s*\}\s*\}',
        re.S,
    )
    replacement = '''        fun factory(application: Application, formId: String?, resumeActiveDraft: Boolean = false): ViewModelProvider.Factory = viewModelFactory {\n            initializer { FormViewModel(application, formId, resumeActiveDraft, createSavedStateHandle()) }\n        }'''
    t, n = pattern.subn(replacement, t, count=1)
    if n != 1:
        # Broader fallback for formatting variations: replace the entire factory function block.
        start = t.find('        fun factory(application: Application, formId: String?)')
        assert start >= 0, 'FormViewModel factory start not found'
        end = t.find('\n        }', start)
        assert end >= 0, 'FormViewModel factory end not found'
        end += len('\n        }')
        t = t[:start] + replacement + t[end:]
vm.write_text(t)

# --- FormScreen: fresh and resume-draft screens use distinct ViewModels. ---
form = pkg / 'feature/form/FormScreen.kt'
t = form.read_text()
if 'fun FormScreen(formId: String? = null, resumeActiveDraft: Boolean = false)' not in t:
    t, n = re.subn(r'fun FormScreen\(formId:\s*String\?\s*=\s*null\)\s*\{', 'fun FormScreen(formId: String? = null, resumeActiveDraft: Boolean = false) {', t, count=1)
    assert n == 1, 'FormScreen signature not found'

if '"active-draft" else "fresh"' not in t:
    old = 'key = "form-${formId ?: "active"}",'
    assert old in t, 'FormScreen ViewModel key not found'
    t = t.replace(old, 'key = "form-${formId ?: if (resumeActiveDraft) "active-draft" else "fresh"}",', 1)

if 'FormViewModel.factory(application, formId, resumeActiveDraft)' not in t:
    old = 'factory = FormViewModel.factory(application, formId)'
    assert old in t, 'FormScreen factory call not found'
    t = t.replace(old, 'factory = FormViewModel.factory(application, formId, resumeActiveDraft)', 1)
form.write_text(t)

# --- Navigation: New Form always gets a new session; Quick Preview resumes latest draft. ---
app = pkg / 'app/IGPOrganizerApp.kt'
t = app.read_text()
if '"new/{session}" -> Destination.NewForm.route' not in t:
    old = '    val selectedRoute = if (current == "edit/{formId}") Destination.Saved.route else current'
    assert old in t, 'selectedRoute anchor not found'
    new = '''    val selectedRoute = when (current) {\n        "edit/{formId}" -> Destination.Saved.route\n        "new/{session}" -> Destination.NewForm.route\n        "draft" -> Destination.NewForm.route\n        else -> current\n    }'''
    t = t.replace(old, new, 1)

if 'navController.navigate("new/${System.nanoTime()}")' not in t:
    start = t.find('    val select: (Destination) -> Unit = { destination ->')
    assert start >= 0, 'navigation select block not found'
    end = t.find('\n\n    Scaffold(', start)
    assert end >= 0, 'navigation select block end not found'
    new_select = '''    val select: (Destination) -> Unit = { destination ->\n        if (destination == Destination.NewForm) {\n            navController.navigate("new/${System.nanoTime()}") {\n                popUpTo(navController.graph.findStartDestination().id) { saveState = true }\n                launchSingleTop = true\n                restoreState = false\n            }\n        } else {\n            val leavingSavedEdit = navController.currentDestination?.route == "edit/{formId}"\n            if (leavingSavedEdit) {\n                navController.popBackStack(Destination.Saved.route, inclusive = false)\n            }\n            navController.navigate(destination.route) {\n                popUpTo(navController.graph.findStartDestination().id) { saveState = true }\n                launchSingleTop = true\n                restoreState = destination != Destination.Saved\n            }\n        }\n    }'''
    t = t[:start] + new_select + t[end:]

if 'navController.navigate("draft")' not in t:
    old = '                            onPreview = { select(Destination.NewForm) }'
    assert old in t, 'Home Quick Preview anchor not found'
    new = '''                            onPreview = {\n                                navController.navigate("draft") { launchSingleTop = true }\n                            }'''
    t = t.replace(old, new, 1)

if 'route = "new/{session}"' not in t:
    old = '                    composable(Destination.NewForm.route) { FormScreen() }'
    assert old in t, 'New Form composable anchor not found'
    new = '''                    composable(\n                        route = "new/{session}",\n                        arguments = listOf(navArgument("session") { type = NavType.StringType })\n                    ) { FormScreen() }\n                    composable("draft") { FormScreen(resumeActiveDraft = true) }'''
    t = t.replace(old, new, 1)
app.write_text(t)

# --- Saved Forms: include drafts so starting a fresh form never makes prior work inaccessible. ---
dao = pkg / 'data/db/Daos.kt'
t = dao.read_text()
t = t.replace('SELECT * FROM forms WHERE isDraft = 0 ORDER BY updatedAt DESC\") fun observeSavedAggregates()', 'SELECT * FROM forms ORDER BY updatedAt DESC\") fun observeSavedAggregates()', 1)
assert 'SELECT * FROM forms ORDER BY updatedAt DESC\") fun observeSavedAggregates()' in t, 'Saved drafts query not updated'
dao.write_text(t)

saved = pkg / 'feature/saved/SavedFormsScreen.kt'
t = saved.read_text()
if 'localText("Draft","مسودة","ڕەشنووس")' not in t:
    needle = 'Text(a.form.formDate,color=Muted,fontSize=12.sp);'
    if needle in t:
        t = t.replace(needle, needle + 'if(a.form.isDraft){Text(localText("Draft","مسودة","ڕەشنووس"),color=InfinityTeal,fontWeight=FontWeight.SemiBold,fontSize=12.sp)};', 1)
    else:
        # The behavior is still correct if the compact card formatting changed; drafts remain listed.
        print('warning: Draft badge placement anchor changed; drafts will still be listed')
saved.write_text(t)

# --- Ensure RC12 soft-input behavior is present even if manifest formatting differed. ---
manifest = root / 'app/src/main/AndroidManifest.xml'
mt = manifest.read_text()
if 'android:windowSoftInputMode="adjustResize"' not in mt:
    mt, n = re.subn(r'(android:name="\.MainActivity"[^>]*android:exported="true")', r'\1\n            android:windowSoftInputMode="adjustResize"', mt, count=1, flags=re.S)
    assert n == 1, 'Could not add adjustResize to MainActivity'
manifest.write_text(mt)

# --- Version ---
build = root / 'app/build.gradle.kts'
t = build.read_text()
if 'versionCode = 47' not in t:
    t, n = re.subn(r'versionCode\s*=\s*46', 'versionCode = 47', t, count=1)
    assert n == 1, 'RC12 versionCode 46 not found'
if 'versionName = "5.0.0-rc13"' not in t:
    t, n = re.subn(r'versionName\s*=\s*"5\.0\.0-rc12"', 'versionName = "5.0.0-rc13"', t, count=1)
    assert n == 1, 'RC12 versionName not found'
build.write_text(t)

# Final source-level validation.
checks = {
    'fresh draft logic': 'resumeActiveDraft -> forms.observeActiveDraft().first()' in vm.read_text(),
    'fresh route': 'new/{session}' in app.read_text(),
    'draft route': 'composable("draft")' in app.read_text(),
    'saved includes drafts': 'SELECT * FROM forms ORDER BY updatedAt DESC' in dao.read_text(),
    'adjustResize': 'android:windowSoftInputMode="adjustResize"' in manifest.read_text(),
    'rc13 code': 'versionCode = 47' in build.read_text(),
}
missing = [k for k,v in checks.items() if not v]
assert not missing, f'RC13 validation failed: {missing}'
print('RC13 patch validation OK:', ', '.join(checks))
