package com.anurag.eduai.uikit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.anurag.eduai.uikit.avatar.SavedTutorAvatar
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.theme.EduAiDimens
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole
import com.anurag.eduai.uikit.theme.forRole

data class FriendUpdate(
    val name: String,
    val event: String,
    val cheers: Int,
    val role: EduChipRole = EduChipRole.Accent,
    val seen: Boolean = false,
    /** Pending friend request — rail tap accepts instead of cheering. */
    val isRequest: Boolean = false,
    val requestFriendId: String = "",
    /** Feed cheer target; -1 when this card is a request or placeholder. */
    val feedItemId: Long = -1L,
)

data class RevisionItem(
    val topic: String,
    val score: Int,
    val conceptId: String = "",
    val chapterId: String = "",
)

data class BookmarkItem(
    val key: String,
    val typeLabel: String,
    val role: EduChipRole,
    val conceptId: String = "",
    val simulationId: String = "",
    val isPlaceholder: Boolean = false,
)

data class SubjectTile(
    val name: String,
    val role: EduChipRole = EduChipRole.Pro,
    val subjectId: String = "",
    /** Optional remote icon (from the backend); falls back to a subject glyph when null. */
    val iconUrl: String? = null,
    /** Second line on the row (e.g. "14 chapters"). Falls back to the rail's CTA label when null. */
    val subtitle: String? = null,
    /** Completion fraction 0f..1f (completed chapters / total). Null → no ring shown. */
    val progress: Float? = null,
)

/**
 * Compact tutor greeting for the Home feed — a minimized Free avatar in a bubble
 * with a short line of encouragement. Keeps the tutor present without taking a
 * full screen.
 */
@Composable
fun HomeTutorBubble(
    title: String = "Your tutor",
    message: String = "Ready for today's quests? Let's go!",
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = EduAiTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                .background(colors.surface2)
                .then(
                    if (onClick != null) {
                        Modifier.pressScaleClickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accentBg),
        ) {
            SavedTutorAvatar(
                state = AvatarState.Happy,
                modifier = Modifier.matchParentSize(),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun FriendsInviteRail(
    onAddFriend: () -> Unit = {},
    onInviteShare: () -> Unit = {},
    modifier: Modifier = Modifier,
    sectionTitle: String = "Friends' updates",
    addLabel: String = "Add",
    addCardTitle: String = "Add a friend",
    addCardBody: String = "Enter their friend code to link accounts.",
    inviteTitle: String = "Invite friends",
    inviteSubtitle: String = "Share your code",
    shareLabel: String = "Share",
) {
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        SectionHeader(title = sectionTitle, seeAllLabel = addLabel, onSeeAllClick = onAddFriend)
        HorizontalRail {
            RailCard(onClick = onAddFriend, modifier = Modifier.width(170.dp)) {
                IconBubble(icon = Icons.Outlined.EmojiEvents, fg = colors.pro, bg = colors.proBg)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = addCardTitle,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = addCardBody,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
            }
            InviteCard(
                onInvite = onInviteShare,
                title = inviteTitle,
                subtitle = inviteSubtitle,
                shareLabel = shareLabel,
            )
        }
    }
}

@Composable
fun FriendsUpdatesRail(
    friends: List<FriendUpdate>,
    onSeeAll: () -> Unit = {},
    onCheer: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    sectionTitle: String = "Friends' updates",
    seeAllLabel: String = "See all",
    acceptLabel: String = "Accept",
) {
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        SectionHeader(title = sectionTitle, seeAllLabel = seeAllLabel, onSeeAllClick = onSeeAll)
        HorizontalRail {
            friends.forEachIndexed { index, friend ->
                val (fg, bg) = colors.forRole(friend.role)
                RailCard(onClick = { onCheer(index) }, modifier = Modifier.width(130.dp)) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(bg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = friendIcon(friend.role),
                                contentDescription = null,
                                tint = fg,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                        if (!friend.seen) {
                            NotificationDot(
                                modifier = Modifier.align(Alignment.TopEnd),
                                size = 8.dp,
                                borderColor = colors.surface2,
                            )
                        }
                    }
                    Text(
                        text = friend.name,
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = friend.event,
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (friend.isRequest) acceptLabel else "👍 ${friend.cheers}",
                        color = if (friend.isRequest) colors.accent else colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = if (friend.isRequest) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarksRail(
    bookmarks: List<BookmarkItem>,
    onSeeAll: () -> Unit = {},
    onOpen: (BookmarkItem) -> Unit = {},
    modifier: Modifier = Modifier,
    sectionTitle: String = "Bookmarks",
    seeAllLabel: String = "See all (${bookmarks.size})",
) {
    if (bookmarks.isEmpty()) return
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        SectionHeader(
            title = sectionTitle,
            seeAllLabel = seeAllLabel,
            onSeeAllClick = onSeeAll,
        )
        HorizontalRail {
            bookmarks.take(5).forEach { item ->
                val (fg, bg) = colors.forRole(item.role)
                RailCard(onClick = { onOpen(item) }, modifier = Modifier.width(140.dp)) {
                    BookmarkTypeRow(icon = bookmarkIcon(item.role), fg = fg, bg = bg, label = item.typeLabel)
                    Text(
                        text = item.key,
                        color = colors.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun RevisionRail(
    items: List<RevisionItem>,
    modifier: Modifier = Modifier,
    onOpen: (RevisionItem) -> Unit = {},
    sectionTitle: String = "Needs revision",
    scoreLabelFor: (Int) -> String = { "$it% last quiz" },
) {
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        HorizontalRail {
            items.forEach { item ->
                Column(
                    modifier =
                        Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(EduAiDimens.cardRadius))
                            .background(colors.warningBg)
                            .pressScaleClickable(onClick = { onOpen(item) }, pressedScale = 0.96f)
                            .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Text(
                        text = item.topic,
                        color = colors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        text = scoreLabelFor(item.score),
                        color = colors.warning,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectsRail(
    title: String,
    subjects: List<SubjectTile>,
    onOpen: (SubjectTile) -> Unit = {},
    modifier: Modifier = Modifier,
    ctaLabel: String = "Continue",
    // App fills this to render the live remote icon (ui-kit has no image loader). Default
    // is the built-in subject glyph.
    iconContent: @Composable (SubjectTile, Color) -> Unit = { subject, tint ->
        Icon(
            imageVector = subjectMaterialIcon(subject.name),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(30.dp),
        )
    },
) {
    val colors = EduAiTheme.colors
    Column(modifier = modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(
            text = title,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        // Full-width list rows: icon chip · name + detail · chevron. Each row is one tap
        // target → opens the subject's chapters. Role tint only on the icon chip so the
        // card surface stays theme-safe (no white-on-white in dark mode).
        subjects.forEachIndexed { index, subject ->
            if (index > 0) Spacer(modifier = Modifier.height(10.dp))
            SubjectRow(
                subject = subject,
                ctaLabel = ctaLabel,
                onOpen = onOpen,
                iconContent = iconContent,
            )
        }
    }
}

@Composable
private fun SubjectRow(
    subject: SubjectTile,
    ctaLabel: String,
    onOpen: (SubjectTile) -> Unit,
    iconContent: @Composable (SubjectTile, Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val (fg, bg) = colors.forRole(subject.role)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .pressScaleClickable(onClick = { onOpen(subject) }, pressedScale = 0.98f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Role-tinted icon chip, glyph tinted with the role foreground.
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            iconContent(subject, fg)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subject.name,
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subject.subtitle ?: ctaLabel,
                color = colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        subject.progress?.let { fraction ->
            SubjectProgressRing(progress = fraction, color = fg)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Compact completion ring with a centred percentage — trailing element on a subject row. */
@Composable
private fun SubjectProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = EduAiTheme.colors
    val fraction = progress.coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    Box(
        modifier = modifier.size(38.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 4.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = colors.border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            if (fraction > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = "$percent%",
            color = colors.text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BookmarkTypeRow(
    icon: ImageVector,
    fg: Color,
    bg: Color,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        }
        Text(text = label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Subject-specific glyphs so each tile reads at a glance (Math, Science, etc.). */
fun subjectMaterialIcon(name: String): ImageVector = subjectIcon(name)

private fun subjectIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        "math" in n || "ಗಣಿತ" in n -> Icons.Outlined.Calculate
        "physic" in n || "ಭೌತ" in n -> Icons.Outlined.Bolt
        "chem" in n || "ರಸಾಯನ" in n -> Icons.Outlined.Biotech
        "bio" in n || "ಜೀವ" in n -> Icons.Outlined.Spa
        "science" in n || "ವಿಜ್ಞಾನ" in n -> Icons.Outlined.Science
        "english" in n || "ಇಂಗ್ಲಿಷ್" in n || "kannada" in n || "ಕನ್ನಡ" in n -> Icons.Outlined.MenuBook
        "social" in n || "history" in n || "geograph" in n || "ಸಮಾಜ" in n -> Icons.Outlined.Public
        else -> Icons.Outlined.AutoAwesome
    }
}

private fun friendIcon(role: EduChipRole): ImageVector =
    when (role) {
        EduChipRole.Accent -> Icons.Outlined.LocalFireDepartment
        EduChipRole.Success -> Icons.Outlined.EmojiEvents
        EduChipRole.Pro -> Icons.Outlined.EmojiEvents
        else -> Icons.Outlined.SportsEsports
    }

private fun bookmarkIcon(role: EduChipRole): ImageVector =
    when (role) {
        EduChipRole.Pro -> Icons.Outlined.SportsEsports
        EduChipRole.Warning -> Icons.Outlined.Refresh
        else -> Icons.Outlined.AutoAwesome
    }

@Composable
private fun InviteCard(
    onInvite: () -> Unit,
    title: String = "Invite friends",
    subtitle: String = "Share your code",
    shareLabel: String = "Share",
) {
    val colors = EduAiTheme.colors
    RailCard(onClick = onInvite, modifier = Modifier.width(150.dp)) {
        IconBubble(icon = Icons.Outlined.PersonAddAlt, fg = colors.accent, bg = colors.accentBg)
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = title, color = colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = colors.textSecondary, fontSize = 11.sp, lineHeight = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(EduAiDimens.chipRadius))
                    .background(colors.accent)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = shareLabel, color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun IconBubble(
    icon: ImageVector,
    fg: Color,
    bg: Color,
) {
    Box(
        modifier = Modifier.size(30.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
    }
}
