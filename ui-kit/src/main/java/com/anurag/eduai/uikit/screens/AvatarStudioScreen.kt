package com.anurag.eduai.uikit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anurag.eduai.uikit.avatar.TutorConfig
import com.anurag.eduai.uikit.avatar.core.AvatarState
import com.anurag.eduai.uikit.avatar.core.TutorCharacter
import com.anurag.eduai.uikit.avatar.daysUntilNextDrop
import com.anurag.eduai.uikit.avatar.rememberSavedTutorConfig
import com.anurag.eduai.uikit.avatar.rememberUnlockedAvatars
import com.anurag.eduai.uikit.avatar.shareAvatar
import com.anurag.eduai.uikit.avatar.weeklyAvatarPresets
import com.anurag.eduai.uikit.avatar.EduTutorAvatar
import com.anurag.eduai.uikit.avatar.AvatarUnlockStore
import com.anurag.eduai.uikit.avatar.AvatarPreset
import com.anurag.eduai.uikit.avatar.AdRewardOverlay
import com.anurag.eduai.uikit.avatar.AdRewardRequest
import com.anurag.eduai.uikit.components.EduChip
import com.anurag.eduai.uikit.components.EduPrimaryButton
import com.anurag.eduai.uikit.components.EduSecondaryButton
import com.anurag.eduai.uikit.components.ScrollableChipRow
import com.anurag.eduai.uikit.components.ScrollableHorizontalRail
import com.anurag.eduai.uikit.components.SectionHeader
import com.anurag.eduai.uikit.components.pressScaleClickable
import com.anurag.eduai.uikit.components.rememberEduFeedback
import com.anurag.eduai.uikit.screens.AvatarStudioCopy
import com.anurag.eduai.uikit.screens.defaultAvatarStudioCopy
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.anurag.eduai.uikit.theme.EduChipRole

/**
 * Playground for the ported avatars — pick Orb or the code-drawn Free character,
 * set a mood, and (for Free) cycle through every customization the Animation
 * project shipped: outfit, neck, hair, hair colour, glasses, frame colour,
 * under-eye line, cheek shading, mood/gesture overrides, and a 360° spin.
 */
@Composable
fun EduAvatarStudioScreen(
    modifier: Modifier = Modifier,
    copy: AvatarStudioCopy = defaultAvatarStudioCopy(),
    onConfigPersisted: (config: TutorConfig, presetId: String?) -> Unit = { _, _ -> },
    showRewardedAds: (suspend (sessionId: String, totalAds: Int, actionLabel: String) -> Boolean)? = null,
) {
    val colors = EduAiTheme.colors
    val context = LocalContext.current
    val saved = rememberSavedTutorConfig()

    // Draft seeded from the saved look; edits are live in the preview until saved.
    // Enums are not saveable by default — use remember (not rememberSaveable) to avoid runtime crash.
    var character by remember(saved.character) { mutableStateOf(saved.character) }
    var state by remember { mutableStateOf(AvatarState.Idle) }
    var outfit by rememberSaveable { mutableIntStateOf(saved.outfit) }
    var neck by rememberSaveable { mutableIntStateOf(saved.neck) }
    var hair by rememberSaveable { mutableIntStateOf(saved.hair) }
    var hairColor by rememberSaveable { mutableIntStateOf(saved.hairColor) }
    var glasses by rememberSaveable { mutableIntStateOf(saved.glasses) }
    var frameColor by rememberSaveable { mutableIntStateOf(saved.frameColor) }
    var eyeLine by rememberSaveable { mutableStateOf(saved.eyeLine) }
    var cheeks by rememberSaveable { mutableStateOf(saved.cheeks) }
    var mood by rememberSaveable { mutableIntStateOf(0) }
    var gesture by rememberSaveable { mutableIntStateOf(0) }
    var spin by rememberSaveable { mutableIntStateOf(0) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }

    val feedback = rememberEduFeedback()

    val weekly = remember { weeklyAvatarPresets() }
    val unlockedIds = rememberUnlockedAvatars()
    val daysLeft = remember { daysUntilNextDrop() }
    var adRequest by remember { mutableStateOf<AdRewardRequest?>(null) }

    fun currentDraftConfig() =
        TutorConfig(
            character = character,
            outfit = outfit,
            neck = neck,
            hair = hair,
            hairColor = hairColor,
            glasses = glasses,
            frameColor = frameColor,
            eyeLine = eyeLine,
            cheeks = cheeks,
        )

    val isDraftSaved = currentDraftConfig() == saved

    Box(modifier = modifier.fillMaxSize()) {
      Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.surface1)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 48.dp),
      ) {
        SectionHeader(title = copy.sectionTitle)

        // Character toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(TutorCharacter.Orb, TutorCharacter.Free).forEach { c ->
                EduChip(
                    label = c.label,
                    role = if (character == c) EduChipRole.Accent else EduChipRole.Neutral,
                    onClick = { character = c },
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            EduTutorAvatar(
                character = character,
                state = state,
                modifier = Modifier.fillMaxSize().padding(12.dp),
                outfitVariant = outfit,
                hairStyle = hair,
                hairColor = hairColor,
                glassesStyle = glasses,
                glassesColor = frameColor,
                neckStyle = neck,
                underEyeLine = eyeLine,
                cheekShading = cheeks,
                moodOverride = mood,
                gestureOverride = gesture,
                spinTrigger = spin,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        StudioLabel(copy.mood)
        ScrollableChipRow(hintText = copy.swipeMoods) {
            AvatarState.entries.forEach { s ->
                CycleChip(text = s.name, selected = state == s, onClick = { state = s })
            }
        }

        if (character == TutorCharacter.Free) {
            Spacer(modifier = Modifier.height(18.dp))
            StudioLabel(copy.customize)
            ScrollableChipRow(hintText = copy.swipeOptions) {
                CycleChip(copy.outfit(outfit)) { outfit = (outfit + 1) % 4 }
                CycleChip(copy.neck(neck)) { neck = (neck + 1) % 3 }
                CycleChip(copy.hair(hair)) { hair = (hair + 1) % 3 }
                CycleChip(copy.hairColor(hairColor)) { hairColor = (hairColor + 1) % 3 }
                CycleChip(copy.glasses(glasses)) { glasses = (glasses + 1) % 3 }
                CycleChip(copy.frame(frameColor)) { frameColor = (frameColor + 1) % 3 }
                CycleChip(copy.eyeLine(eyeLine)) { eyeLine = !eyeLine }
                CycleChip(copy.cheeks(cheeks)) { cheeks = !cheeks }
            }

            Spacer(modifier = Modifier.height(18.dp))
            StudioLabel(copy.expression)
            ScrollableChipRow(hintText = copy.swipeExpressions) {
                CycleChip(copy.moodLabel(mood)) { mood = (mood + 1) % 6 }
                CycleChip(copy.gestureLabel(gesture)) { gesture = (gesture + 1) % 6 }
                CycleChip(copy.spin360, role = EduChipRole.Accent) { spin += 1 }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        EduPrimaryButton(
            text =
                when {
                    isDraftSaved -> copy.saved
                    else -> copy.saveWithAds
                },
            onClick = {
                if (isDraftSaved) return@EduPrimaryButton
                saveErrorMessage = null
                adRequest =
                    AdRewardRequest(
                        sessionId = "save_custom",
                        actionLabel = copy.savingTutor,
                    )
            },
            fillMaxWidth = true,
        )
        Spacer(modifier = Modifier.height(10.dp))
        EduSecondaryButton(
            text = copy.shareWithFriends,
            onClick = {
                shareAvatar(
                    context = context,
                    avatarName = "my custom tutor",
                    config = currentDraftConfig(),
                )
            },
            fillMaxWidth = true,
        )
        Text(
            text = copy.saveHint,
            color = colors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        saveErrorMessage?.let { message ->
            Text(
                text = message,
                color = colors.warning,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = copy.weeklyAvatars,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = copy.newInDays(daysLeft),
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = copy.unlockHint,
            color = colors.textMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        ScrollableHorizontalRail(hintText = copy.swipeAvatars) {
            weekly.forEach { preset ->
                WeeklyAvatarCard(
                    preset = preset,
                    copy = copy,
                    unlocked = unlockedIds.contains(preset.id),
                    onUnlock = {
                        adRequest =
                            AdRewardRequest(
                                sessionId = "unlock_${preset.id}",
                                actionLabel = copy.unlockingPreset(preset.name),
                            )
                    },
                    onUse = {
                        // Persist only via host (Room/Firestore + TutorConfigStore under mutex).
                        onConfigPersisted(preset.config, preset.id)
                        character = preset.config.character
                        outfit = preset.config.outfit
                        neck = preset.config.neck
                        hair = preset.config.hair
                        hairColor = preset.config.hairColor
                        glasses = preset.config.glasses
                        frameColor = preset.config.frameColor
                        eyeLine = preset.config.eyeLine
                        cheeks = preset.config.cheeks
                        feedback.claim()
                    },
                    onShare = { shareAvatar(context, preset.name, preset.config) },
                )
            }
        }
      }

      AdRewardOverlay(
          request = adRequest,
          showRewardedAds = showRewardedAds,
          onComplete = { sessionId ->
              when {
                  sessionId == "save_custom" -> {
                      val config = currentDraftConfig()
                      // Persist only via host so Room/Firestore and SharedPrefs stay in sync.
                      onConfigPersisted(config, null)
                      feedback.claim()
                      saveErrorMessage = null
                  }
                  sessionId.startsWith("unlock_") -> {
                      val presetId = sessionId.removePrefix("unlock_")
                      weekly.find { it.id == presetId }?.let {
                          AvatarUnlockStore.unlock(context, it.id)
                      }
                      feedback.reward()
                  }
              }
              adRequest = null
          },
          onCancel = { sessionId ->
              if (sessionId == "save_custom") {
                  saveErrorMessage = copy.saveAdFailed
              }
              adRequest = null
          },
      )
    }
}

@Composable
private fun WeeklyAvatarCard(
    preset: AvatarPreset,
    copy: AvatarStudioCopy,
    unlocked: Boolean,
    onUnlock: () -> Unit,
    onUse: () -> Unit,
    onShare: () -> Unit,
) {
    val colors = EduAiTheme.colors
    Column(
        modifier =
            Modifier
                .width(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface2)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface1),
            contentAlignment = Alignment.Center,
        ) {
            EduTutorAvatar(
                character = preset.config.character,
                state = AvatarState.Idle,
                modifier = Modifier.fillMaxSize().padding(6.dp),
                outfitVariant = preset.config.outfit,
                hairStyle = preset.config.hair,
                hairColor = preset.config.hairColor,
                glassesStyle = preset.config.glasses,
                glassesColor = preset.config.frameColor,
                neckStyle = preset.config.neck,
                underEyeLine = preset.config.eyeLine,
                cheekShading = preset.config.cheeks,
            )
            if (!unlocked) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = copy.locked,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(preset.name, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            preset.tagline,
            color = colors.textMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (unlocked) {
            EduPrimaryButton(text = copy.use, onClick = onUse, fillMaxWidth = true)
            Spacer(modifier = Modifier.height(6.dp))
            EduSecondaryButton(text = copy.share, onClick = onShare, fillMaxWidth = true)
        } else {
            EduPrimaryButton(text = copy.unlockWithAds, onClick = onUnlock, fillMaxWidth = true)
        }
    }
}

@Composable
private fun StudioLabel(text: String) {
    val colors = EduAiTheme.colors
    Text(
        text = text,
        color = colors.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun CycleChip(
    text: String,
    selected: Boolean = false,
    role: EduChipRole? = null,
    onClick: () -> Unit,
) {
    val colors = EduAiTheme.colors
    val active = selected || role == EduChipRole.Accent
    Text(
        text = text,
        color = if (active) colors.onAccent else colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(if (active) colors.accent else colors.surface2)
                .pressScaleClickable(onClick = onClick, pressedScale = 0.94f)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
