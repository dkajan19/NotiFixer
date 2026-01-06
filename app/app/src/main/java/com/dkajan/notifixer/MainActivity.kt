package com.dkajan.notifixer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        private var splashAlreadyShown = false
        private val cachedAllApps = mutableListOf<AppItem>()
        private val cachedSavedApps = mutableListOf<AppItem>()
        private var lastPackageFingerprint: Int = 0
    }

    private val savedApps = mutableStateListOf<AppItem>()
    private val allApps = mutableStateListOf<AppItem>()
    private var isShizukuReady by mutableStateOf(false)
    private var showWarning by mutableStateOf(true)
    private var isDataLoaded by mutableStateOf(false)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { checkShizukuStatus() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { isShizukuReady = false }
    private val requestPermissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        isShizukuReady = grantResult == PackageManager.PERMISSION_GRANTED
        if (isShizukuReady) startAsyncLoading()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (cachedAllApps.isNotEmpty()) {
            allApps.addAll(cachedAllApps)
            savedApps.addAll(cachedSavedApps)
            isDataLoaded = true
        }

        val prefs = getSharedPreferences("noti_prefs", MODE_PRIVATE)
        showWarning = prefs.getBoolean("show_warning", true)
        val savedFilterType = prefs.getInt("filter_type", 0)
        val savedSortType = prefs.getString("sort_type", SortType.AppName.name) ?: SortType.AppName.name
        val savedIsReverse = prefs.getBoolean("is_reverse", false)

        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)

        checkShizukuStatus()

        setContent {
            AppTheme {
                var animationFinished by rememberSaveable { mutableStateOf(splashAlreadyShown) }
                val showSplashScreen = !splashAlreadyShown && !(animationFinished && (isDataLoaded || !isShizukuReady))

                LaunchedEffect(showSplashScreen) {
                    if (!showSplashScreen && animationFinished) {
                        splashAlreadyShown = true
                    }
                }

                AnimatedContent(
                    targetState = showSplashScreen,
                    transitionSpec = {
                        (fadeIn(tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)))
                            .togetherWith(fadeOut(tween(300)))
                    },
                    label = "SplashTransition"
                ) { isSplashVisible ->
                    if (isSplashVisible) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedSplashScreen(onAnimationFinished = {
                                animationFinished = true
                                if (!isShizukuReady) splashAlreadyShown = true
                            })
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (isShizukuReady) {
                                MainContent(
                                    pm = packageManager,
                                    savedApps = savedApps,
                                    allApps = allApps,
                                    showWarning = showWarning,
                                    onHideWarningPermanently = { neverShowAgain ->
                                        showWarning = false
                                        if (neverShowAgain) prefs.edit().putBoolean("show_warning", false).apply()
                                    },
                                    onResetWarning = {
                                        showWarning = true
                                        prefs.edit().putBoolean("show_warning", true).apply()
                                    },
                                    onDataChanged = { saveApps() },
                                    initialFilterType = savedFilterType,
                                    initialSortType = try { SortType.valueOf(savedSortType) } catch (e: Exception) { SortType.AppName },
                                    initialIsReverse = savedIsReverse,
                                    onFiltersChanged = { f, s, r -> saveFilters(f, s, r) }
                                )
                            } else {
                                ShizukuErrorScreen { checkShizukuStatus() }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkShizukuStatus() {
        if (Shizuku.pingBinder()) {
            val hasPermission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            isShizukuReady = hasPermission
            if (hasPermission) startAsyncLoading() else Shizuku.requestPermission(0)
        } else {
            isShizukuReady = false
        }
    }

    private fun startAsyncLoading() {
        MainScope().launch(Dispatchers.IO) {
            val pm = packageManager
            val currentAppsInfo = pm.getInstalledApplications(PackageManager.MATCH_ALL)
            val currentFingerprint = currentAppsInfo.map { it.packageName }.hashCode()

            if (currentFingerprint == lastPackageFingerprint && allApps.isNotEmpty()) {
                withContext(Dispatchers.Main) { isDataLoaded = true }
                return@launch
            }

            val loadedAllApps = currentAppsInfo.map { info ->
                val pInfo = pm.getPackageInfo(info.packageName, 0)
                AppItem(
                    info.loadLabel(pm).toString(),
                    info.packageName,
                    (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    pInfo.firstInstallTime,
                    pInfo.lastUpdateTime
                )
            }

            val sharedPref = getSharedPreferences("noti_prefs", MODE_PRIVATE)
            val packageSet = sharedPref.getStringSet("saved_packages", emptySet()) ?: emptySet()

            val loadedSavedApps = packageSet.mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, 0)
                    val pInfo = pm.getPackageInfo(pkg, 0)
                    AppItem(
                        info.loadLabel(pm).toString(),
                        pkg,
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        pInfo.firstInstallTime,
                        pInfo.lastUpdateTime,
                        mutableStateOf(checkCurrentPermission(pkg))
                    )
                } catch (e: Exception) { null }
            }

            withContext(Dispatchers.Main) {
                lastPackageFingerprint = currentFingerprint
                allApps.clear()
                allApps.addAll(loadedAllApps)
                savedApps.clear()
                savedApps.addAll(loadedSavedApps)
                saveApps()
                cachedAllApps.clear()
                cachedAllApps.addAll(loadedAllApps)
                cachedSavedApps.clear()
                cachedSavedApps.addAll(loadedSavedApps)
                isDataLoaded = true
            }
        }
    }

    private fun saveApps() {
        val sharedPref = getSharedPreferences("noti_prefs", MODE_PRIVATE)
        val packageSet = savedApps.map { it.packageName }.toSet()
        sharedPref.edit().putStringSet("saved_packages", packageSet).apply()
        cachedSavedApps.clear()
        cachedSavedApps.addAll(savedApps)
    }

    private fun saveFilters(filterType: Int, sortType: SortType, isReverse: Boolean) {
        getSharedPreferences("noti_prefs", MODE_PRIVATE).edit()
            .putInt("filter_type", filterType)
            .putString("sort_type", sortType.name)
            .putBoolean("is_reverse", isReverse)
            .apply()
    }

    override fun onResume() {
        super.onResume()
        if (isShizukuReady) {
            startAsyncLoading()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    pm: PackageManager,
    savedApps: MutableList<AppItem>,
    allApps: List<AppItem>,
    showWarning: Boolean,
    onHideWarningPermanently: (Boolean) -> Unit,
    onResetWarning: () -> Unit,
    onDataChanged: () -> Unit,
    initialFilterType: Int,
    initialSortType: SortType,
    initialIsReverse: Boolean,
    onFiltersChanged: (Int, SortType, Boolean) -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDismissDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(selectedTab) { if(pagerState.currentPage != selectedTab) pagerState.animateScrollToPage(selectedTab) }
    LaunchedEffect(pagerState.currentPage) { selectedTab = pagerState.currentPage }

    if (showDismissDialog) {
        var neverShowAgain by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showDismissDialog = false },
            title = { Text("Hide notice?") },
            text = {
                Column {
                    Text("This notice will reappear next time unless you check the box.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = neverShowAgain, onCheckedChange = { neverShowAgain = it })
                        Text("Never show again", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onHideWarningPermanently(neverShowAgain); showDismissDialog = false }) { Text("I understand") }
            },
            dismissButton = {
                TextButton(onClick = { showDismissDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(contentAlignment = Alignment.CenterStart) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.bell_ring), null, modifier = Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("NotiFixer", color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "Force persistent notifications via Shizuku.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.offset(y = 25.dp)
                        )
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            IconButton(onClick = onResetWarning, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.lightbulb),
                                    contentDescription = "Reset",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(onClick = { uriHandler.openUri("https://github.com/dkajan19/NotiFixer") }, modifier = Modifier.size(36.dp)) {
                                Icon(painterResource(R.drawable.github), "GitHub", modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Saved") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("All Apps") })
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                when (page) {
                    0 -> SavedTab(pm, savedApps, showWarning, { showDismissDialog = true }, onDataChanged)
                    1 -> AllAppsTab(
                        pm = pm,
                        allApps = allApps,
                        savedApps = savedApps,
                        initialFilterType = initialFilterType,
                        initialSortType = initialSortType,
                        initialIsReverse = initialIsReverse,
                        onFiltersChanged = onFiltersChanged,
                        onToggle = { app, shouldAdd ->
                            if (shouldAdd) {
                                scope.launch(Dispatchers.IO) {
                                    val isEnabled = checkCurrentPermission(app.packageName)
                                    withContext(Dispatchers.Main) {
                                        app.isEnabled.value = isEnabled
                                        savedApps.add(app)
                                        onDataChanged()
                                    }
                                }
                            } else {
                                savedApps.removeAll { it.packageName == app.packageName }
                                onDataChanged()
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SavedTab(pm: PackageManager, savedApps: MutableList<AppItem>, showWarning: Boolean, onDismissRequest: () -> Unit, onDataChanged: () -> Unit) {
    val scope = rememberCoroutineScope()
    var appToDelete by remember { mutableStateOf<AppItem?>(null) }

    val sortedSavedApps = remember(savedApps.size, savedApps.map { it.packageName }.hashCode()) {
        savedApps.sortedBy { it.name.lowercase() }
    }

    if (appToDelete != null) {
        AlertDialog(
            onDismissRequest = { appToDelete = null },
            title = { Text("Remove?") },
            text = { Text("Remove ${appToDelete?.name} from your saved list?") },
            confirmButton = {
                Button(
                    onClick = {
                        savedApps.remove(appToDelete)
                        onDataChanged()
                        appToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { appToDelete = null }) { Text("Cancel") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = savedApps.isEmpty(),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text("No saved apps.", color = MaterialTheme.colorScheme.outline)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            item(key = "warning_box") {
                AnimatedVisibility(
                    visible = showWarning,
                    enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                    exit = fadeOut(tween(250)) + shrinkVertically(tween(250))
                ) {
                    WarningBox(onDismiss = onDismissRequest)
                }
            }

            items(
                items = sortedSavedApps,
                key = { it.packageName },
                contentType = { "app_item" }
            ) { app ->
                ListItem(
                    modifier = Modifier
                        .animateItemPlacement(animationSpec = tween(250))
                        .pointerInput(Unit) { detectTapGestures(onLongPress = { appToDelete = app }) },
                    leadingContent = { AppIcon(app.packageName, pm) },
                    headlineContent = { Text(app.name) },
                    supportingContent = { Text(app.packageName) },
                    trailingContent = {
                        Switch(
                            checked = app.isEnabled.value,
                            onCheckedChange = { checked ->
                                app.isEnabled.value = checked
                                scope.launch(Dispatchers.IO) { runAdbCommand(app.packageName, checked) }
                            }
                        )
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AllAppsTab(
    pm: PackageManager,
    allApps: List<AppItem>,
    savedApps: List<AppItem>,
    initialFilterType: Int,
    initialSortType: SortType,
    initialIsReverse: Boolean,
    onFiltersChanged: (Int, SortType, Boolean) -> Unit,
    onToggle: (AppItem, Boolean) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filterType by rememberSaveable { mutableIntStateOf(initialFilterType) }
    var sortType by rememberSaveable { mutableStateOf(initialSortType) }
    var isReverse by rememberSaveable { mutableStateOf(initialIsReverse) }
    var expandedSort by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(filterType, sortType, isReverse) {
        onFiltersChanged(filterType, sortType, isReverse)
    }

    val filtered by remember(allApps.size, query, filterType, sortType, isReverse) {
        derivedStateOf {
            allApps.asSequence().filter {
                (it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)) &&
                        (filterType == 0 || (filterType == 1 && !it.isSystem) || (filterType == 2 && it.isSystem))
            }.let { sequence ->
                val comparator = when (sortType) {
                    SortType.AppName -> compareBy<AppItem> { it.name.lowercase() }
                    SortType.PackageName -> compareBy<AppItem> { it.packageName.lowercase() }
                    SortType.InstallDate -> compareBy { it.installTime }
                    SortType.UpdateDate -> compareBy { it.lastUpdateTime }
                }
                if (isReverse) sequence.sortedWith(comparator.reversed()) else sequence.sortedWith(comparator)
            }.toList()
        }
    }

    LaunchedEffect(sortType, isReverse, filterType, query) {
        scope.launch {
            delay(30)
            if (!listState.isScrollInProgress && listState.firstVisibleItemIndex > 0) {
                listState.scrollToItem(0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Find app by name or ID...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = filterType == 0, onClick = { if (filterType == 0) scope.launch { listState.scrollToItem(0) } else filterType = 0 }, label = { Text("All") })
            FilterChip(selected = filterType == 1, onClick = { if (filterType == 1) scope.launch { listState.scrollToItem(0) } else filterType = 1 }, label = { Text("User") })
            FilterChip(selected = filterType == 2, onClick = { if (filterType == 2) scope.launch { listState.scrollToItem(0) } else filterType = 2 }, label = { Text("System") })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(
                    onClick = { expandedSort = true },
                    label = { Text("Sort: ${sortType.name}") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.sort_variant),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                DropdownMenu(expanded = expandedSort, onDismissRequest = { expandedSort = false }) {
                    SortType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                if (sortType == type) scope.launch { listState.scrollToItem(0) } else sortType = type
                                expandedSort = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = isReverse,
                onClick = { isReverse = !isReverse },
                label = { Text("Reverse") },
                leadingIcon = {
                    val rotation by animateFloatAsState(
                        targetValue = if (isReverse) 180f else 0f,
                        animationSpec = tween(250),
                        label = "ArrowRotation"
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_down),
                        null,
                        Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedContent(
                targetState = filtered.isEmpty(),
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "EmptyStateTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Text(
                        text = if (query.isEmpty()) "No applications available" else "No apps found",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 0.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.packageName },
                    contentType = { "app_item" }
                ) { app ->
                    val isSaved = remember(savedApps.size) {
                        savedApps.any { it.packageName == app.packageName }
                    }
                    val date = remember(app.installTime) {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(app.installTime))
                    }

                    ListItem(
                        modifier = Modifier.animateItemPlacement(animationSpec = tween(250)),
                        leadingContent = { AppIcon(app.packageName, pm) },
                        headlineContent = { Text(app.name) },
                        supportingContent = {
                            Column {
                                Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("Installed: $date", fontSize = 10.sp)
                            }
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSaved,
                                onCheckedChange = { checked -> onToggle(app, checked) }
                            )
                        }
                    )
                }
            }
        }
    }
}