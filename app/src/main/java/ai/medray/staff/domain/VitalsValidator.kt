package ai.medray.staff.domain

import ai.medray.staff.data.model.Vitals

enum class VitalsSeverity {
    NORMAL,
    WARNING,
    CRITICAL
}

data class VitalsEvaluation(
    val bpSeverity: VitalsSeverity = VitalsSeverity.NORMAL,
    val bpMessage: String? = null,
    val tempSeverity: VitalsSeverity = VitalsSeverity.NORMAL,
    val tempMessage: String? = null,
    val pulseSeverity: VitalsSeverity = VitalsSeverity.NORMAL,
    val pulseMessage: String? = null,
    val spo2Severity: VitalsSeverity = VitalsSeverity.NORMAL,
    val spo2Message: String? = null,
    val overallSeverity: VitalsSeverity = VitalsSeverity.NORMAL
)

object VitalsValidator {

    fun validateBp(bp: String?): Boolean {
        if (bp.isNullOrBlank()) return true
        val parts = bp.trim().split("/")
        if (parts.size != 2) return false
        val systolic = parts[0].trim().toIntOrNull() ?: return false
        val diastolic = parts[1].trim().toIntOrNull() ?: return false
        return systolic in 40..300 && diastolic in 20..200 && systolic > diastolic
    }

    fun parseBp(bp: String?): Pair<Int, Int>? {
        if (bp.isNullOrBlank()) return null
        val parts = bp.trim().split("/")
        if (parts.size != 2) return null
        val s = parts[0].trim().toIntOrNull() ?: return null
        val d = parts[1].trim().toIntOrNull() ?: return null
        return Pair(s, d)
    }

    fun evaluate(vitals: Vitals): VitalsEvaluation {
        var bpSev = VitalsSeverity.NORMAL
        var bpMsg: String? = null
        val bp = parseBp(vitals.vitalsBp)
        if (bp != null) {
            val (sys, dia) = bp
            when {
                sys >= 180 || dia >= 120 -> {
                    bpSev = VitalsSeverity.CRITICAL
                    bpMsg = "Hypertensive Crisis ($sys/$dia)"
                }
                sys >= 140 || dia >= 90 -> {
                    bpSev = VitalsSeverity.WARNING
                    bpMsg = "Stage 2 Hypertension ($sys/$dia)"
                }
                sys > 130 || dia > 80 -> {
                    bpSev = VitalsSeverity.WARNING
                    bpMsg = "Stage 1 Hypertension ($sys/$dia)"
                }
                sys < 90 || dia < 60 -> {
                    bpSev = VitalsSeverity.WARNING
                    bpMsg = "Hypotension ($sys/$dia)"
                }
            }
        }

        var spo2Sev = VitalsSeverity.NORMAL
        var spo2Msg: String? = null
        if (vitals.vitalsSpo2 != null) {
            when {
                vitals.vitalsSpo2 < 90 -> {
                    spo2Sev = VitalsSeverity.CRITICAL
                    spo2Msg = "Critical Hypoxia (${vitals.vitalsSpo2}%)"
                }
                vitals.vitalsSpo2 < 95 -> {
                    spo2Sev = VitalsSeverity.WARNING
                    spo2Msg = "Low Oxygen (${vitals.vitalsSpo2}%)"
                }
            }
        }

        var tempSev = VitalsSeverity.NORMAL
        var tempMsg: String? = null
        if (vitals.vitalsTemperatureF != null) {
            when {
                vitals.vitalsTemperatureF >= 103.0 -> {
                    tempSev = VitalsSeverity.CRITICAL
                    tempMsg = "High Fever (${vitals.vitalsTemperatureF}°F)"
                }
                vitals.vitalsTemperatureF >= 99.5 -> {
                    tempSev = VitalsSeverity.WARNING
                    tempMsg = "Fever (${vitals.vitalsTemperatureF}°F)"
                }
                vitals.vitalsTemperatureF < 95.0 -> {
                    tempSev = VitalsSeverity.WARNING
                    tempMsg = "Hypothermia (${vitals.vitalsTemperatureF}°F)"
                }
            }
        }

        var pulseSev = VitalsSeverity.NORMAL
        var pulseMsg: String? = null
        if (vitals.vitalsPulseBpm != null) {
            when {
                vitals.vitalsPulseBpm >= 130 -> {
                    pulseSev = VitalsSeverity.CRITICAL
                    pulseMsg = "Severe Tachycardia (${vitals.vitalsPulseBpm} bpm)"
                }
                vitals.vitalsPulseBpm >= 100 -> {
                    pulseSev = VitalsSeverity.WARNING
                    pulseMsg = "Tachycardia (${vitals.vitalsPulseBpm} bpm)"
                }
                vitals.vitalsPulseBpm < 50 -> {
                    pulseSev = VitalsSeverity.WARNING
                    pulseMsg = "Bradycardia (${vitals.vitalsPulseBpm} bpm)"
                }
            }
        }

        val maxSev = listOf(bpSev, spo2Sev, tempSev, pulseSev).maxByOrNull { it.ordinal } ?: VitalsSeverity.NORMAL

        return VitalsEvaluation(
            bpSeverity = bpSev,
            bpMessage = bpMsg,
            tempSeverity = tempSev,
            tempMessage = tempMsg,
            pulseSeverity = pulseSev,
            pulseMessage = pulseMsg,
            spo2Severity = spo2Sev,
            spo2Message = spo2Msg,
            overallSeverity = maxSev
        )
    }
}
