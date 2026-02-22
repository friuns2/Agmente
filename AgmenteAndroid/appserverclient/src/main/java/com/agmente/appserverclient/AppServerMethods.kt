package com.agmente.appserverclient

object AppServerMethods {
    const val INITIALIZE = "initialize"
    const val INITIALIZED = "initialized"

    const val THREAD_START = "thread/start"
    const val THREAD_RESUME = "thread/resume"
    const val THREAD_LIST = "thread/list"
    const val THREAD_ARCHIVE = "thread/archive"

    const val TURN_START = "turn/start"
    const val TURN_INTERRUPT = "turn/interrupt"

    const val REVIEW_START = "review/start"
    const val COMMAND_EXEC = "command/exec"

    const val MODEL_LIST = "model/list"
    const val SKILLS_LIST = "skills/list"

    const val MCP_SERVER_OAUTH_LOGIN = "mcpServer/oauth/login"
    const val MCP_SERVER_STATUS_LIST = "mcpServerStatus/list"

    const val FEEDBACK_UPLOAD = "feedback/upload"

    const val CONFIG_READ = "config/read"
    const val CONFIG_VALUE_WRITE = "config/value/write"
    const val CONFIG_BATCH_WRITE = "config/batchWrite"
}
