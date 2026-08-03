package com.ncert7.aitutorandlab.domain.examplan

enum class PlanFeasibilitySeverity {
    ERROR,
    WARNING,
}

data class PlanFeasibilityIssue(
    val severity: PlanFeasibilitySeverity,
    val message: String,
)

data class PlanFeasibilityResult(
    val requiredPlanDays: Int,
    val lessonDays: Int,
    val reviseDays: Int,
    val mockExamDays: Int,
    val availableCalendarDays: Int,
    val totalTrialItems: Int,
    val issues: List<PlanFeasibilityIssue>,
) {
    val blockingErrors: List<PlanFeasibilityIssue> =
        issues.filter { it.severity == PlanFeasibilitySeverity.ERROR }

    val warnings: List<PlanFeasibilityIssue> =
        issues.filter { it.severity == PlanFeasibilitySeverity.WARNING }

    val canSave: Boolean = blockingErrors.isEmpty()
}
