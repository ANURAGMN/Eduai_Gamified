package com.ncert7.aitutorandlab.ui.screens.subjectscreen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.repository.SubjectRepository
import com.ncert7.aitutorandlab.ui.screens.subjectscreen.dataclass.SubjectScreenState
import com.ncert7.aitutorandlab.ui.models.SubjectUiModel
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.resolveSubjectIconUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val repository: SubjectRepository,
    private val sharedPreferenceUtils: SharedPreferenceUtils
) : ViewModel() {

    private val _state = MutableStateFlow(SubjectScreenState())
    val state: StateFlow<SubjectScreenState> = _state.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects(languageCode: String = getCurrentLanguageCode()) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val subjectEntities = repository.getSubjectsForClass(7)

                val subjectUiModels = subjectEntities.map { entity ->
                    SubjectUiModel(
                        id = entity.subjectId,
                        name = entity.getLocalizedName(languageCode),
                        color = BrandPrimary,
                        totalChapters = entity.totalChapters,
                        iconUrl = resolveSubjectIconUrl(entity.subjectId, entity.getLocalizedName(languageCode), entity.iconUrl),
                    )
                }

                _state.value = _state.value.copy(
                    subjects = subjectUiModels,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun setClassLevel(classLevel: Int) {
        _state.value = _state.value.copy(classLevel = classLevel)
        loadSubjects()
    }

    fun reloadSubjects(languageCode: String) {
        loadSubjects(languageCode)
    }

    fun onSubjectSelected(subjectId: String) {
        sharedPreferenceUtils.setSubjectSelectionId(subjectId)
    }
}