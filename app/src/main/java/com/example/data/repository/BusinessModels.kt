package com.example.data.repository

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Immutable
data class BusinessCrmDeal(
    val id: String = UUID.randomUUID().toString(),
    val clientName: String,
    val contactEmail: String = "",
    val dealValueCents: Long = 0L,
    val stage: String = STAGE_LEAD,
    val notes: String = "",
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val STAGE_LEAD = "LEAD"
        const val STAGE_NEGOTIATION = "NEGOTIATION"
        const val STAGE_ACTIVE_CLIENT = "ACTIVE_CLIENT"
        const val STAGE_DELIVERED = "DELIVERED"
        val ALL_STAGES = listOf(STAGE_LEAD, STAGE_NEGOTIATION, STAGE_ACTIVE_CLIENT, STAGE_DELIVERED)

        fun stageLabel(stage: String): String = when (stage) {
            STAGE_LEAD -> "Lead"
            STAGE_NEGOTIATION -> "Negotiation"
            STAGE_ACTIVE_CLIENT -> "Active Client"
            STAGE_DELIVERED -> "Delivered"
            else -> stage
        }

        fun parseList(json: String): List<BusinessCrmDeal> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            BusinessCrmDeal(
                                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                clientName = obj.optString("clientName", "Client"),
                                contactEmail = obj.optString("contactEmail", ""),
                                dealValueCents = obj.optLong("dealValueCents", 0L).coerceAtLeast(0L),
                                stage = obj.optString("stage", STAGE_LEAD).takeIf { it in ALL_STAGES } ?: STAGE_LEAD,
                                notes = obj.optString("notes", ""),
                                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeList(list: List<BusinessCrmDeal>): String {
            val arr = JSONArray()
            list.forEach { d ->
                arr.put(
                    JSONObject().apply {
                        put("id", d.id)
                        put("clientName", d.clientName)
                        put("contactEmail", d.contactEmail)
                        put("dealValueCents", d.dealValueCents)
                        put("stage", d.stage)
                        put("notes", d.notes)
                        put("updatedAtMillis", d.updatedAtMillis)
                    }
                )
            }
            return arr.toString()
        }
    }
}

@Immutable
data class BusinessOkrItem(
    val id: String = UUID.randomUUID().toString(),
    val objective: String,
    val keyResult: String,
    val currentValue: Double = 0.0,
    val targetValue: Double = 100.0,
    val unitLabel: String = "%",
    val ownerName: String = ""
) {
    val progressRatio: Float
        get() = if (targetValue > 0.0) (currentValue / targetValue).toFloat().coerceIn(0f, 1f) else 0f

    companion object {
        fun parseList(json: String): List<BusinessOkrItem> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            BusinessOkrItem(
                                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                objective = obj.optString("objective", "Objective"),
                                keyResult = obj.optString("keyResult", "Key Result"),
                                currentValue = obj.optDouble("currentValue", 0.0).coerceAtLeast(0.0),
                                targetValue = obj.optDouble("targetValue", 100.0).coerceAtLeast(1.0),
                                unitLabel = obj.optString("unitLabel", "%"),
                                ownerName = obj.optString("ownerName", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeList(list: List<BusinessOkrItem>): String {
            val arr = JSONArray()
            list.forEach { o ->
                arr.put(
                    JSONObject().apply {
                        put("id", o.id)
                        put("objective", o.objective)
                        put("keyResult", o.keyResult)
                        put("currentValue", o.currentValue)
                        put("targetValue", o.targetValue)
                        put("unitLabel", o.unitLabel)
                        put("ownerName", o.ownerName)
                    }
                )
            }
            return arr.toString()
        }
    }
}

@Immutable
data class BusinessSopDoc(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val templateType: String = "SOP",
    val content: String = "",
    val resourceUrl: String? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CANVAS = "CANVAS"
        const val TYPE_PITCH_DECK = "PITCH_DECK"
        const val TYPE_PRICING = "PRICING"
        const val TYPE_SOP = "SOP"

        fun defaultTemplateContent(templateType: String, projectName: String): Pair<String, String> = when (templateType) {
            TYPE_CANVAS -> "$projectName — Business Model Canvas" to """
                • Customer Segments: Early adopters & B2B/SaaS buyers
                • Value Proposition: 10x faster execution & unified workflow
                • Channels: Organic demos, Product Hunt, Partner referrals
                • Revenue Streams: Monthly subscription / retainer tiers
                • Key Cost Drivers: Cloud infra, AI API usage, distribution
            """.trimIndent()
            TYPE_PITCH_DECK -> "$projectName — 10-Slide Pitch Deck" to """
                1. Problem & Urgency
                2. Solution & Live Product Demo
                3. Why Now & Market Size (TAM/SAM/SOM)
                4. Traction & Customer Proof
                5. Business Model & Unit Economics
                6. Go-To-Market Engine & Ask
            """.trimIndent()
            TYPE_PRICING -> "$projectName — Service & SaaS Pricing Sheet" to """
                • Starter Tier ($29/mo): Core workspace & 1 seat
                • Pro Growth ($79/mo): Automation, analytics & 5 team seats
                • Agency / Enterprise ($299/mo): Custom SLA, CRM sync & unlimited seats
            """.trimIndent()
            else -> "$projectName — Standard Operating Procedure" to """
                • Trigger: Weekly sprint review or client kickoff
                • Owner: Delivery Lead
                • Steps: Audit blockers -> Assign owners -> Sync with Team OS -> Ship release notes
            """.trimIndent()
        }

        fun parseList(json: String): List<BusinessSopDoc> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        add(
                            BusinessSopDoc(
                                id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                                title = obj.optString("title", "Business Doc"),
                                templateType = obj.optString("templateType", TYPE_SOP),
                                content = obj.optString("content", ""),
                                resourceUrl = obj.optString("resourceUrl", "").takeIf { it.isNotBlank() },
                                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeList(list: List<BusinessSopDoc>): String {
            val arr = JSONArray()
            list.forEach { d ->
                arr.put(
                    JSONObject().apply {
                        put("id", d.id)
                        put("title", d.title)
                        put("templateType", d.templateType)
                        put("content", d.content)
                        d.resourceUrl?.let { put("resourceUrl", it) }
                        put("updatedAtMillis", d.updatedAtMillis)
                    }
                )
            }
            return arr.toString()
        }
    }
}

object PollVotesCodec {
    fun parseVotes(json: String): Map<String, Int> {
        if (json.isBlank() || json == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = linkedMapOf<String, Int>()
            obj.keys().forEach { uid ->
                val idx = obj.optInt(uid, -1)
                if (idx >= 0) map[uid] = idx
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun encodeVotes(votes: Map<String, Int>): String {
        val obj = JSONObject()
        votes.forEach { (uid, idx) ->
            if (uid.isNotBlank() && idx >= 0) {
                obj.put(uid, idx)
            }
        }
        return obj.toString()
    }

    fun vote(json: String, uid: String, optionIndex: Int): String {
        val updated = parseVotes(json).toMutableMap()
        if (uid.isNotBlank() && optionIndex >= 0) {
            updated[uid] = optionIndex
        }
        return encodeVotes(updated)
    }

    fun parseOptions(json: String): List<String> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotBlank()) add(v)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeOptions(options: List<String>): String {
        val arr = JSONArray()
        options.map { it.trim() }.filter { it.isNotBlank() }.take(8).forEach { arr.put(it) }
        return arr.toString()
    }
}
