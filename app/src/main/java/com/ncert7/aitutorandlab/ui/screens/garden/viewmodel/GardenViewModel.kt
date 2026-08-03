package com.ncert7.aitutorandlab.ui.screens.garden.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anurag.eduai.uikit.garden.quest.GardenSceneSnapshot
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ZONE_CAPACITY
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.data.local.dao.ConceptDao
import com.ncert7.aitutorandlab.data.local.dao.StudentDao
import com.ncert7.aitutorandlab.data.local.entities.GardenTheme
import com.ncert7.aitutorandlab.domain.garden.GardenPlantedListRow
import com.ncert7.aitutorandlab.domain.garden.GardenProgress
import com.ncert7.aitutorandlab.domain.garden.GardenStarterHighlight
import com.ncert7.aitutorandlab.repository.GardenRepository
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.ncert7.aitutorandlab.utils.GardenCopyFactory
import com.ncert7.aitutorandlab.utils.GardenWorldLabels
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GardenTabUiState(
    val scene: GardenSceneSnapshot? = null,
    val progress: GardenProgress? = null,
    val theme: Theme = Theme.GARDEN,
    val plantedRows: List<GardenPlantedListRow> = emptyList(),
    val themeNote: String? = null,
    val viewingZone: Int? = null,
    val showStarterPlantHighlight: Boolean = false,
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenRepository: GardenRepository,
    private val conceptDao: ConceptDao,
    private val studentDao: StudentDao,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {
    private val userId: String get() = sharedPrefs.getUserId().orEmpty()

    private val _uiState = MutableStateFlow(GardenTabUiState())
    val uiState: StateFlow<GardenTabUiState> = _uiState.asStateFlow()

    private var languageCode: String = "en"

    fun syncLanguage(code: String) {
        languageCode = normalizeLanguageCode(code)
    }

    fun refresh() {
        viewModelScope.launch {
            val studentId = resolveStudentId() ?: return@launch
            val progress = gardenRepository.getProgress(studentId) ?: return@launch
            val scene = gardenRepository.getSceneSnapshot(studentId) ?: return@launch
            val theme = gardenRepository.toComposeTheme(progress.theme)
            val items = gardenRepository.getPlantedItems(studentId)
            val rows = buildPlantedRows(items, theme)
            val starterSeen = sharedPrefs.hasSeenGardenStarterPlantHighlight(studentId)
            val showStarterHighlight = GardenStarterHighlight.shouldShow(progress, theme, starterSeen)
            _uiState.value =
                GardenTabUiState(
                    scene = scene,
                    progress = progress,
                    theme = theme,
                    plantedRows = rows,
                    themeNote = _uiState.value.themeNote,
                    viewingZone = _uiState.value.viewingZone ?: progress.currentZone,
                    showStarterPlantHighlight = showStarterHighlight,
                )
        }
    }

    fun acknowledgeStarterPlantHighlight() {
        viewModelScope.launch {
            val studentId = resolveStudentId() ?: return@launch
            sharedPrefs.setGardenStarterPlantHighlightSeen(studentId)
            _uiState.value = _uiState.value.copy(showStarterPlantHighlight = false)
        }
    }

    fun selectViewingZone(zoneIndex: Int) {
        val progress = _uiState.value.progress ?: return
        if (zoneIndex !in progress.unlockedZones) return
        _uiState.value = _uiState.value.copy(viewingZone = zoneIndex)
    }

    fun setPreferredSlot(slot: Int) {
        viewModelScope.launch {
            val studentId = resolveStudentId() ?: return@launch
            gardenRepository.setPreferredSlot(studentId, slot)
            sharedPrefs.setGardenStarterPlantHighlightSeen(studentId)
            refresh()
        }
    }

    fun chooseTheme(theme: Theme) {
        viewModelScope.launch {
            val studentId = resolveStudentId() ?: return@launch
            val persisted =
                when (theme) {
                    Theme.OUTPOST -> GardenTheme.OUTPOST
                    Theme.ISLAND -> GardenTheme.ISLAND
                    Theme.COLONY -> GardenTheme.COLONY
                    else -> GardenTheme.GARDEN
                }
            gardenRepository.setTheme(studentId, persisted)
            val homeCopy = GardenCopyFactory.homeCopy(languageCode)
            val themeCopy = GardenCopyFactory.themeCopy(languageCode, theme)
            val note = homeCopy.themeChosen(themeCopy.placeCollection)
            refresh()
            _uiState.value = _uiState.value.copy(themeNote = note)
        }
    }

    fun clearThemeNote() {
        _uiState.value = _uiState.value.copy(themeNote = null)
    }

    private suspend fun resolveStudentId(): String? {
        val student = studentDao.getStudentSync(userId)
        return student?.studentId?.takeIf { it.isNotBlank() } ?: userId.takeIf { it.isNotBlank() }
    }

    private suspend fun buildPlantedRows(
        items: List<com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity>,
        theme: Theme,
    ): List<GardenPlantedListRow> {
        val kannada = isKannadaLanguage(languageCode)
        return items.map { item ->
            val concept = conceptDao.getConcept(item.conceptId)
            val conceptLabel = concept?.getLocalizedName(languageCode)?.takeIf { it.isNotBlank() }
                ?: item.conceptId
            val kindLabel =
                when (item.kind.uppercase()) {
                    "STUDY" -> if (kannada) "ಅಧ್ಯಯನ" else "Study"
                    "REVISION" -> if (kannada) "ಪುನರಾವಲೋಕನ" else "Revision"
                    else -> if (kannada) "ಸಿಮ್" else "Sim"
                }
            val zone = ZONES.getOrNull(item.zone.coerceIn(0, ZONES.lastIndex)) ?: ZONES.first()
            val slotLabel = GardenWorldLabels.slotName(zone, theme, item.slot.coerceIn(0, 5), languageCode)
            val zoneLabel = GardenWorldLabels.zoneName(zone, theme, languageCode)
            GardenPlantedListRow(
                conceptLabel = conceptLabel,
                kindLabel = kindLabel,
                slotLabel = slotLabel,
                zoneIndex = item.zone,
                zoneLabel = zoneLabel,
            )
        }.reversed()
    }
}
