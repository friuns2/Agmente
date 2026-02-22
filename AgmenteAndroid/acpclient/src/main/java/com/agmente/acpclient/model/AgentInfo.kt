package com.agmente.acpclient.model

data class AgentProfile(
    val id: String?,
    val name: String,
    val title: String? = null,
    val version: String? = null,
    val description: String? = null,
    val modes: List<AgentModeOption> = emptyList(),
    val capabilities: AgentCapabilityState = AgentCapabilityState(),
    val verifications: List<AgentCapabilityVerification> = emptyList()
) {
    val displayName: String get() = title ?: name

    val displayNameWithVersion: String
        get() = if (!version.isNullOrEmpty()) "$displayName v$version" else displayName

    val requiresUnescapedSlashesInJSONRPC: Boolean
        get() = name.trim().lowercase() == "codex-acp"

    companion object {
        fun parse(result: Map<String, JSONValue>): AgentProfile {
            val agentObj = result["agentInfo"]?.objectValue
                ?: result["agent"]?.objectValue
                ?: emptyMap()

            val name = agentObj["name"]?.stringValue ?: "Agent"
            val id = agentObj["id"]?.stringValue
            val title = agentObj["title"]?.stringValue
            val version = agentObj["version"]?.stringValue
            val description = agentObj["description"]?.stringValue

            var modes = emptyList<AgentModeOption>()
            val modesObj = result["modes"]?.objectValue
            val availableModes = modesObj?.get("availableModes")?.arrayValue
            if (availableModes != null) {
                modes = availableModes.mapNotNull { modeValue ->
                    val modeObj = modeValue.objectValue ?: return@mapNotNull null
                    val modeId = modeObj["id"]?.stringValue ?: return@mapNotNull null
                    val modeName = modeObj["name"]?.stringValue ?: return@mapNotNull null
                    AgentModeOption(
                        id = modeId,
                        name = modeName,
                        description = modeObj["description"]?.stringValue
                    )
                }
            }

            var capabilities = AgentCapabilityState()
            val capabilitiesObj = result["agentCapabilities"]?.objectValue
                ?: result["capabilities"]?.objectValue
                ?: emptyMap()

            capabilitiesObj["loadSession"]?.boolValue?.let {
                capabilities = capabilities.copy(loadSession = it)
            }

            val sessionCaps = capabilitiesObj["sessionCapabilities"]?.objectValue
            if (sessionCaps?.containsKey("resume") == true) {
                capabilities = capabilities.copy(resumeSession = true)
            }

            capabilitiesObj["listSessions"]?.boolValue?.let {
                capabilities = capabilities.copy(listSessions = it)
            }

            capabilitiesObj["sessionListRequiresCwd"]?.boolValue?.let {
                capabilities = capabilities.copy(sessionListRequiresCwd = it)
            }

            val promptCaps = capabilitiesObj["promptCapabilities"]?.objectValue
            if (promptCaps != null) {
                capabilities = capabilities.copy(
                    promptCapabilities = PromptCapabilityState(
                        audio = promptCaps["audio"]?.boolValue ?: false,
                        image = promptCaps["image"]?.boolValue ?: false,
                        embeddedContext = promptCaps["embeddedContext"]?.boolValue ?: false
                    )
                )
            }

            AgentBehaviorRules.applyRules(name, capabilities).let {
                capabilities = it
            }

            val verifications = AgentBehaviorRules.verifications(name, version)

            return AgentProfile(
                id = id,
                name = name,
                title = title,
                version = version,
                description = description,
                modes = modes,
                capabilities = capabilities,
                verifications = verifications
            )
        }
    }
}

data class AgentModeOption(
    val id: String,
    val name: String,
    val description: String? = null
)

data class SessionCommand(
    val id: String,
    val name: String,
    val description: String,
    val inputHint: String? = null
) {
    companion object {
        fun parse(update: Map<String, JSONValue>): List<SessionCommand> {
            val commandValues = update["availableCommands"]?.arrayValue ?: return emptyList()
            return commandValues.mapNotNull { value ->
                val commandObj = value.objectValue ?: return@mapNotNull null
                val name = commandObj["name"]?.stringValue ?: return@mapNotNull null
                val description = commandObj["description"]?.stringValue ?: return@mapNotNull null
                val inputHint = commandObj["input"]?.objectValue?.get("hint")?.stringValue
                SessionCommand(id = name, name = name, description = description, inputHint = inputHint)
            }
        }
    }
}

data class AgentCapabilityState(
    val loadSession: Boolean = false,
    val resumeSession: Boolean = false,
    val listSessions: Boolean = true,
    val sessionListRequiresCwd: Boolean = false,
    val promptCapabilities: PromptCapabilityState = PromptCapabilityState()
)

data class PromptCapabilityState(
    val audio: Boolean = false,
    val image: Boolean = false,
    val embeddedContext: Boolean = false
)

enum class AgentCapabilityOutcome {
    VERIFIED, WARNING
}

data class AgentCapabilityVerification(
    val feature: String,
    val outcome: AgentCapabilityOutcome,
    val details: String,
    val versionRequirement: VersionRequirement
) {
    fun appliesTo(version: String?): Boolean =
        versionRequirement.matches(version)
}

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    companion object {
        fun parse(string: String): SemanticVersion? {
            val parts = string.split(".").map { it.toIntOrNull() ?: 0 }
            if (parts.isEmpty()) return null
            return SemanticVersion(
                major = parts.getOrElse(0) { 0 },
                minor = parts.getOrElse(1) { 0 },
                patch = parts.getOrElse(2) { 0 }
            )
        }
    }
}

data class VersionRequirement(
    val min: SemanticVersion? = null,
    val max: SemanticVersion? = null
) {
    fun matches(versionString: String?): Boolean {
        if (versionString == null) return min == null && max == null
        val version = SemanticVersion.parse(versionString) ?: return min == null && max == null
        if (min != null && version < min) return false
        if (max != null && version > max) return false
        return true
    }

    companion object {
        val ANY = VersionRequirement()
    }
}

object AgentBehaviorRules {
    fun applyRules(agentName: String, capabilities: AgentCapabilityState): AgentCapabilityState {
        val normalizedName = agentName.lowercase()
        return when (normalizedName) {
            "qwen-code" -> capabilities.copy(sessionListRequiresCwd = true)
            else -> capabilities
        }
    }

    fun verifications(agentName: String, version: String?): List<AgentCapabilityVerification> {
        val normalizedName = agentName.lowercase()
        val results = mutableListOf<AgentCapabilityVerification>()

        if (normalizedName == "qwen" || normalizedName.startsWith("qwen-")) {
            val requirement = VersionRequirement(max = SemanticVersion.parse("3.0.0"))
            val warning = AgentCapabilityVerification(
                feature = "promptCapabilities.image",
                outcome = AgentCapabilityOutcome.WARNING,
                details = "Agent advertises image prompts but current builds reject image content blocks; treat image support as unreliable.",
                versionRequirement = requirement
            )
            if (warning.appliesTo(version)) results.add(warning)
        }

        if (normalizedName.contains("claude")) {
            val resumeReq = VersionRequirement(max = SemanticVersion.parse("0.12.2"))
            val resumeWarning = AgentCapabilityVerification(
                feature = "sessionCapabilities.resume",
                outcome = AgentCapabilityOutcome.WARNING,
                details = "Session resume requires claude-code-acp v0.12.3 or later.",
                versionRequirement = resumeReq
            )
            if (resumeWarning.appliesTo(version)) results.add(resumeWarning)

            val forkReq = VersionRequirement(max = SemanticVersion.parse("0.12.3"))
            val forkWarning = AgentCapabilityVerification(
                feature = "sessionCapabilities.fork",
                outcome = AgentCapabilityOutcome.WARNING,
                details = "Session forking requires claude-code-acp v0.12.4 or later.",
                versionRequirement = forkReq
            )
            if (forkWarning.appliesTo(version)) results.add(forkWarning)
        }

        return results
    }

    fun requiresCwdForSessionList(agentName: String?): Boolean {
        val name = agentName?.lowercase() ?: return false
        return name == "qwen-code"
    }
}
