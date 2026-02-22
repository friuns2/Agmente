package com.agmente.model

sealed class Screen(val route: String) {
    data object ServerList : Screen("server_list")
    data object AddServer : Screen("add_server")
    data class EditServer(val serverId: String) : Screen("edit_server/$serverId")
    data class SessionDetail(val sessionId: String) : Screen("session/$sessionId")
    data object Settings : Screen("settings")
    data object DeveloperLogs : Screen("developer_logs")
}
