package com.tinaut1986.netpulse.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinaut1986.netpulse.R
import com.tinaut1986.netpulse.data.DiagnosticReport
import com.tinaut1986.netpulse.data.SavedDiagnostic
import com.tinaut1986.netpulse.ui.theme.*

// ──────────────────────────────────────────────────────────────────
// Main history list screen  (supports multi-select)
// ──────────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiagnosticHistoryScreen(
    entries: List<SavedDiagnostic>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onExport: (String) -> Unit,
    onDeleteMultiple: (List<String>) -> Unit,
    onExportMultiple: (List<String>) -> Unit,
    onExportAll: () -> Unit
) {
    // ── Selection state ──────────────────────────────────────────
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }

    fun exitSelection() {
        selectionMode = false
        selectedIds.clear()
    }

    // Exit selection mode on back press
    BackHandler(enabled = selectionMode) { exitSelection() }

    // ── Dialogs ──────────────────────────────────────────────────
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showDeleteSelectionDialog by remember { mutableStateOf(false) }
    var showExportAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header / action bar ───────────────────────────────────
        if (selectionMode) {
            SelectionActionBar(
                selectedCount = selectedIds.size,
                allSelected = selectedIds.size == entries.size,
                onSelectAll = {
                    if (selectedIds.size == entries.size) {
                        selectedIds.clear()
                    } else {
                        selectedIds.clear()
                        selectedIds.addAll(entries.map { it.id })
                    }
                },
                onDelete = { if (selectedIds.isNotEmpty()) showDeleteSelectionDialog = true },
                onExport = { if (selectedIds.isNotEmpty()) onExportMultiple(selectedIds.toList()) },
                onClose = { exitSelection() }
            )
        } else {
            NormalHeader(
                hasEntries = entries.isNotEmpty(),
                onDeleteAll = { showDeleteAllDialog = true },
                onExportAll = { showExportAllDialog = true }
            )
        }

        if (entries.isEmpty()) {
            HistoryEmptyState()
        } else {
            if (!selectionMode) {
                Text(
                    text = stringResource(R.string.history_count, entries.size),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val isSelected = selectedIds.contains(entry.id)
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                    ) {
                        HistoryEntryCard(
                            entry = entry,
                            isSelected = isSelected,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) {
                                    if (isSelected) selectedIds.remove(entry.id)
                                    else selectedIds.add(entry.id)
                                } else {
                                    onOpen(entry.id)
                                }
                            },
                            onLongClick = {
                                if (!selectionMode) {
                                    selectionMode = true
                                    selectedIds.add(entry.id)
                                }
                            },
                            onDelete = { onDelete(entry.id) },
                            onExport = { onExport(entry.id) }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ── Delete all dialog ─────────────────────────────────────────
    if (showDeleteAllDialog) {
        ConfirmDialog(
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = SignalRed) },
            title = stringResource(R.string.delete_all_title),
            body = stringResource(R.string.delete_all_body),
            confirmLabel = stringResource(R.string.delete_all),
            confirmColor = SignalRed,
            onConfirm = { onDeleteAll(); },
            onDismiss = { }
        )
    }

    // ── Export all dialog ─────────────────────────────────────────
    if (showExportAllDialog) {
        ConfirmDialog(
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryBlue) },
            title = stringResource(R.string.export_all_title),
            body = stringResource(R.string.export_all_body, entries.size),
            confirmLabel = stringResource(R.string.export_all),
            confirmColor = PrimaryBlue,
            onConfirm = { onExportAll(); },
            onDismiss = { }
        )
    }

    // ── Delete selection dialog ───────────────────────────────────
    if (showDeleteSelectionDialog) {
        ConfirmDialog(
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SignalRed) },
            title = stringResource(R.string.delete_selection_title),
            body = stringResource(R.string.delete_selection_body, selectedIds.size),
            confirmLabel = stringResource(R.string.delete_entry),
            confirmColor = SignalRed,
            onConfirm = {
                onDeleteMultiple(selectedIds.toList())
                exitSelection()
            },
            onDismiss = { }
        )
    }
}

// ── Normal header (delete all + export all) ───────────────────────
@Composable
private fun NormalHeader(
    hasEntries: Boolean,
    onDeleteAll: () -> Unit,
    onExportAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (hasEntries) {
            Row {
                IconButton(
                    onClick = onExportAll,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.IosShare,
                        contentDescription = stringResource(R.string.export_all),
                        tint = PrimaryBlue
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDeleteAll,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = SignalRed.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = stringResource(R.string.delete_all),
                        tint = SignalRed.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ── Selection-mode action bar ─────────────────────────────────────
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    allSelected: Boolean,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = PrimaryBlue.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = stringResource(R.string.n_selected, selectedCount),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            // Select all / deselect all
            IconButton(onClick = onSelectAll) {
                Icon(
                    if (allSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = stringResource(R.string.select_all),
                    tint = PrimaryBlue
                )
            }
            // Export selected
            IconButton(
                onClick = onExport,
                enabled = selectedCount > 0
            ) {
                Icon(
                    Icons.Default.IosShare,
                    contentDescription = stringResource(R.string.export_entry),
                    tint = if (selectedCount > 0) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
            // Delete selected
            IconButton(
                onClick = onDelete,
                enabled = selectedCount > 0
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_entry),
                    tint = if (selectedCount > 0) SignalRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

// ── Single history entry card ─────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryEntryCard(
    entry: SavedDiagnostic,
    isSelected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scoreColor = when {
        entry.overallScore >= 80 -> SignalGreen
        entry.overallScore >= 50 -> SignalYellow
        else -> SignalRed
    }

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue else Color.Transparent,
        animationSpec = tween(150),
        label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(150),
        label = "bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox (selection mode) or score circle
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(scoreColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${entry.overallScore}",
                            color = scoreColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "/100",
                            color = scoreColor.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            lineHeight = 8.sp
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Main info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.ssid,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(3.dp))
                if (entry.problems.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = SignalRed, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.n_problems, entry.problems.size), color = SignalRed, fontSize = 11.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SignalGreen, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.no_problems), color = SignalGreen, fontSize = 11.sp)
                    }
                }
            }

            // Context menu (only in normal mode)
            if (!selectionMode) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.open_entry)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                            onClick = { showMenu = false; onClick() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_entry)) },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { showMenu = false; onExport() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_entry), color = SignalRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SignalRed) },
                            onClick = { showMenu = false; showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SignalRed) },
            title = stringResource(R.string.delete_entry_title),
            body = stringResource(R.string.delete_entry_body, entry.label),
            confirmLabel = stringResource(R.string.delete_entry),
            confirmColor = SignalRed,
            onConfirm = { onDelete(); },
            onDismiss = { }
        )
    }
}

// ── Shared confirm dialog ─────────────────────────────────────────
@Composable
private fun ConfirmDialog(
    icon: @Composable () -> Unit,
    title: String,
    body: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
            ) { Text(confirmLabel, color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

// ── Detail screen: reuses NetworkQualityScreen cards ─────────────
@Composable
fun DiagnosticDetailScreen(
    entry: SavedDiagnostic,
    report: DiagnosticReport,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onBackground)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.ssid, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(entry.label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
            }
            IconButton(
                onClick = onExport,
                colors = IconButtonDefaults.iconButtonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f))
            ) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.export_entry), tint = PrimaryBlue)
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QualityScoreCard(report.overallScore)
            Spacer(Modifier.height(12.dp))

            if (report.problems.isNotEmpty()) {
                ProblemsCard(report.problems)
                Spacer(Modifier.height(12.dp))
            }

            report.gatewayDiag?.let {
                TargetCard(it, Icons.Default.Router, PrimaryBlue)
                Spacer(Modifier.height(8.dp))
            }
            report.dns1Diag?.let {
                TargetCard(it, Icons.Default.Dns, PrimaryPurple)
                Spacer(Modifier.height(8.dp))
            }
            report.googleDnsDiag?.let {
                TargetCard(it, Icons.Default.Public, SignalGreen)
                Spacer(Modifier.height(8.dp))
            }
            report.internetDiag?.let {
                TargetCard(it, Icons.Default.Cloud, SignalYellow)
                Spacer(Modifier.height(8.dp))
            }

            DnsResolutionCard(report.dnsResolutionMs)
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────
@Composable
fun HistoryEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.history_empty),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
