package com.xprox.sentinel.data

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageManager {
    private const val PREFS_NAME = "x_prox_language_prefs"
    private const val KEY_LANG = "selected_language"

    enum class Language(val code: String, val displayName: String) {
        RU("ru", "Русский"),
        EN("en", "English")
    }

    private val _currentLanguage = MutableStateFlow(Language.RU)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANG, Language.RU.code) ?: Language.RU.code
        val lang = Language.values().firstOrNull { it.code == code } ?: Language.RU
        _currentLanguage.value = lang
    }

    fun setLanguage(context: Context, lang: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, lang.code).apply()
        _currentLanguage.value = lang
    }

    fun getString(key: String): String {
        val lang = _currentLanguage.value
        return translations[key]?.get(lang) ?: key
    }

    private val translations = mapOf(
        // Tabs
        "tab_dashboard" to mapOf(Language.RU to "Панель", Language.EN to "Dashboard"),
        "tab_profiles" to mapOf(Language.RU to "Маршруты", Language.EN to "Routing"),
        "tab_logs" to mapOf(Language.RU to "Аудит логов", Language.EN to "Traffic Logs"),
        "tab_settings" to mapOf(Language.RU to "Настройки", Language.EN to "Settings"),
        
        // Navigation / HUD
        "app_title" to mapOf(Language.RU to "SENTINEL SECURE SHIELD", Language.EN to "SENTINEL SECURE SHIELD"),
        "app_subtitle" to mapOf(Language.RU to "ЗАШИФРОВАННЫЙ КАНАЛ С АВТОРИЗАЦИЕЙ", Language.EN to "AUTHENTICATION ENCRYPTED TUNNEL"),
        
        // Dashboard
        "active_profile" to mapOf(Language.RU to "АКТИВНЫЙ ПРОФИЛЬ", Language.EN to "ACTIVE PROFILE"),
        "connection_profiles" to mapOf(Language.RU to "ПРОФИЛИ ПОДКЛЮЧЕНИЯ", Language.EN to "CONNECTION PROFILES"),
        "no_profile_warning" to mapOf(Language.RU to "Нет настроенных подключений! Импортируйте ссылку VLESS или добавьте профиль вручную ниже.", Language.EN to "No connection configured! Please import a VLESS configuration or configure one manually below."),
        "configure_profile_toast" to mapOf(Language.RU to "Пожалуйста, сначала настройте профиль подключения!", Language.EN to "Please configure/import an active connection first!"),
        "clipboard_empty" to mapOf(Language.RU to "Буфер обмена пуст!", Language.EN to "Clipboard is empty!"),
        "profile_added" to mapOf(Language.RU to "Профиль добавлен", Language.EN to "Profile added"),
        "invalid_link" to mapOf(Language.RU to "Некорректная ссылка VLESS Reality!", Language.EN to "Invalid VLESS Reality link!"),
        "latency" to mapOf(Language.RU to "Задержка", Language.EN to "Latency"),
        "public_ip" to mapOf(Language.RU to "Публичный IP", Language.EN to "Public IP"),
        "shield_active" to mapOf(Language.RU to "ЗАЩИТА АКТИВНА", Language.EN to "SHIELD ACTIVE"),
        "checking_ip" to mapOf(Language.RU to "Проверка IP...", Language.EN to "Checking IP..."),
        "ping_na" to mapOf(Language.RU to "Пинг: Н/Д (Выбрать сервер)", Language.EN to "Ping: N/A (Select Server)"),
        "ping" to mapOf(Language.RU to "Пинг", Language.EN to "Ping"),
        "checking_ping" to mapOf(Language.RU to "Проверка пинга...", Language.EN to "Checking ping..."),
        "insecure_connection" to mapOf(Language.RU to "Незащищенное локальное соединение", Language.EN to "Insecure local connection"),
        "vpn_protection_enabled" to mapOf(Language.RU to "Защита VPN-туннеля активна", Language.EN to "VPN Tunnel Protection Enabled"),
        "socks_protection_active" to mapOf(Language.RU to "ЛОКАЛЬНАЯ ЗАЩИТА ОТ УТЕЧЕК (SOCKS5 АКТИВНА)", Language.EN to "LOCAL LEAK PROTECTION (SOCKS5 ACTIVE)"),
        "bound_port" to mapOf(Language.RU to "Привязанный локальный порт", Language.EN to "Bound Ephemeral Port"),
        "session_token" to mapOf(Language.RU to "Токен сессии (User)", Language.EN to "Username Session Token"),
        "vpn_public_ip" to mapOf(Language.RU to "Публичный IP-адрес VPN", Language.EN to "VPN Remote Public IP"),
        "vulnerability_proof" to mapOf(Language.RU to "Защита: сторонние локальные подключения отклоняются.", Language.EN to "Vulnerability Proof: Unauthorized local connections are automatically rejected."),
        "decloaking_prevention" to mapOf(Language.RU to "Защита от утечки SOCKS5 включится при запуске туннеля.", Language.EN to "SOCKS5 leak prevention will engage automatically upon tunnel startup."),
        
        // Profiles Screen
        "profiles_title" to mapOf(Language.RU to "МАРШРУТИЗАЦИЯ ПРИЛОЖЕНИЙ", Language.EN to "APPLICATION ROUTING"),
        "profiles_subtitle" to mapOf(Language.RU to "Выберите приложения, которые будут работать через VPN", Language.EN to "Choose which apps tunnel through the encrypted proxy"),
        "routing_settings" to mapOf(Language.RU to "РАЗДЕЛЬНОЕ ТУННЕЛИРОВАНИЕ ПО ПРИЛОЖЕНИЯМ", Language.EN to "SPLIT TUNNELING BY APPLICATION"),
        "add_profile_btn" to mapOf(Language.RU to "ВРУЧНУЮ", Language.EN to "MANUAL"),
        "import_clipboard" to mapOf(Language.RU to "БУФЕР", Language.EN to "CLIPBOARD"),
        "connected" to mapOf(Language.RU to "ПОДКЛЮЧЕНО", Language.EN to "CONNECTED"),
        "disconnected" to mapOf(Language.RU to "ОТКЛЮЧЕНО", Language.EN to "DISCONNECTED"),
        "connecting" to mapOf(Language.RU to "ПОДКЛЮЧЕНИЕ...", Language.EN to "CONNECTING..."),
        
        // Split Tunneling Mode Selectors
        "bypass_mode" to mapOf(Language.RU to "Режим обхода", Language.EN to "Bypass Mode"),
        "selection_mode" to mapOf(Language.RU to "Режим выбора", Language.EN to "Selection Mode"),
        "search_apps" to mapOf(Language.RU to "Поиск приложений...", Language.EN to "Search applications..."),
        "nothing_found" to mapOf(Language.RU to "Ничего не найдено", Language.EN to "Nothing found"),
        
        // Traffic Logs Screen
        "logs_title" to mapOf(Language.RU to "МОНИТОРИНГ И АУДИТ ТРАФИКА", Language.EN to "TRAFFIC MONITOR & AUDIT"),
        "logs_subtitle" to mapOf(Language.RU to "Аудит сетевых соединений приложений в реальном времени", Language.EN to "Real-time network connection auditing for apps"),
        "logs_tab_traffic" to mapOf(Language.RU to "Трафик", Language.EN to "Traffic"),
        "logs_tab_xray" to mapOf(Language.RU to "Логи Xray", Language.EN to "Xray Logs"),
        "filter_session" to mapOf(Language.RU to "ВЫБОР СЕССИИ", Language.EN to "SELECT SESSION"),
        "filter_app" to mapOf(Language.RU to "ФИЛЬТР ПО ПРИЛОЖЕНИЮ", Language.EN to "FILTER BY APPLICATION"),
        "sensitive_only" to mapOf(Language.RU to "ТОЛЬКО ОПАСНЫЕ ПОРТЫ", Language.EN to "SENSITIVE PORTS ONLY"),
        "export_logs" to mapOf(Language.RU to "ЭКСПОРТ ЛОГОВ", Language.EN to "EXPORT LOGS"),
        "clear_logs" to mapOf(Language.RU to "ОЧИСТИТЬ ЛОГИ", Language.EN to "CLEAR LOGS"),
        "hud_idle" to mapOf(Language.RU to "[СИСТЕМНЫЙ HUD СТАТУС: ОЖИДАНИЕ - Прослушивание сокетов...]", Language.EN to "[SYSTEM HUD STATUS: IDLE - Listening on TCP/UDP Sockets...]"),
        "no_logs_to_export" to mapOf(Language.RU to "Нет записей для экспорта во всех сессиях", Language.EN to "No records to export in all sessions"),
        
        // Session Dialog
        "session_history_title" to mapOf(Language.RU to "ИСТОРИЯ СЕССИЙ", Language.EN to "SESSION HISTORY"),
        "session_history_desc" to mapOf(Language.RU to "Выберите сессию для просмотра логов", Language.EN to "Select a session to view logs"),
        "active_session_name" to mapOf(Language.RU to "Активная сессия", Language.EN to "Active session"),
        "prev_session_name" to mapOf(Language.RU to "Предыдущая сессия", Language.EN to "Previous session"),
        "size" to mapOf(Language.RU to "Размер", Language.EN to "Size"),
        "lines" to mapOf(Language.RU to "Строк", Language.EN to "Lines"),
        "no_file" to mapOf(Language.RU to "Нет файла", Language.EN to "No file"),
        
        // App Selector Dialog
        "app_selector_title" to mapOf(Language.RU to "ВЫБОР ПРИЛОЖЕНИЯ", Language.EN to "SELECT APPLICATION"),
        "app_selector_desc" to mapOf(Language.RU to "Выберите цель для аудита трафика", Language.EN to "Select a target for traffic auditing"),
        "all_apps" to mapOf(Language.RU to "Все приложения", Language.EN to "All applications"),
        "all_traffic_desc" to mapOf(Language.RU to "Показывать весь трафик устройства", Language.EN to "Show all device traffic"),
        "search_app_placeholder" to mapOf(Language.RU to "Поиск по названию или пакету...", Language.EN to "Search by name or package..."),
        
        // Settings Screen
        "settings_title" to mapOf(Language.RU to "СИСТЕМНЫЕ НАСТРОЙКИ", Language.EN to "SYSTEM SETTINGS"),
        "settings_subtitle" to mapOf(Language.RU to "Управление ядром Xray, языком интерфейса и портами аудита", Language.EN to "Manage Xray core, interface language and audit ports"),
        "kill_switch_title" to mapOf(Language.RU to "АВАРИЙНЫЙ ВЫКЛЮЧАТЕЛЬ (KILL SWITCH)", Language.EN to "BLOCK LEAKAGE (KILL SWITCH)"),
        "kill_switch_desc" to mapOf(Language.RU to "Блокировать весь исходящий трафик, когда VPN включен, но ядро прокси (Xray) не запущено, находится в режиме ожидания или аварийно завершило работу. Это защищает от утечки незашифрованных данных.", Language.EN to "Block all outgoing traffic when VPN is active but the proxy core (Xray) is not running, is in standby mode, or crashed. This prevents unencrypted data leaks."),
        "battery_optimization_title" to mapOf(Language.RU to "ОПТИМИЗАЦИЯ БАТАРЕИ", Language.EN to "BATTERY OPTIMIZATION"),
        "battery_optimization_desc" to mapOf(Language.RU to "Разрешить приложению работать в фоновом режиме без ограничений системы. Это предотвратит отключение VPN при выключенном экране и в режиме ожидания.", Language.EN to "Allow the app to run in the background without system restrictions. This prevents the VPN from disconnecting when the screen is off or in standby mode."),
        "battery_optimization_whitelisted" to mapOf(Language.RU to "🛡 ФОНОВЫЙ РЕЖИМ: БЕЗ ОГРАНИЧЕНИЙ", Language.EN to "🛡 BACKGROUND STATUS: UNRESTRICTED"),
        "battery_optimization_restricted" to mapOf(Language.RU to "⚠️ РЕЖИМ ОГРАНИЧЕН (VPN может отключаться)", Language.EN to "⚠️ BATTERY OPTIMIZED (VPN may drop in standby)"),
        "btn_disable_optimization" to mapOf(Language.RU to "РАЗРЕШИТЬ ФОНОВУЮ РАБОТУ", Language.EN to "ALLOW BACKGROUND WORK"),
        "lang_card_title" to mapOf(Language.RU to "ЯЗЫК ИНТЕРФЕЙСА (LANGUAGE)", Language.EN to "INTERFACE LANGUAGE (ЯЗЫК)"),
        "lang_card_desc" to mapOf(Language.RU to "Выберите основной язык для приложения Sentinel", Language.EN to "Select the primary language for Sentinel app"),
        "core_engine" to mapOf(Language.RU to "ЯДРО ПРОКСИ-СЕРВЕРА (XRAY-CORE)", Language.EN to "NATIVE PROXY CORE ENGINE"),
        "log_diagnostics" to mapOf(Language.RU to "ДИАГНОСТИКА И ЛОГ-ФАЙЛЫ", Language.EN to "LOG ENGINE & DIAGNOSTICS"),
        "sensitive_ports" to mapOf(Language.RU to "КОНТРОЛИРУЕМЫЕ ПОРТЫ АУДИТА", Language.EN to "SENSITIVE AUDIT PORTS"),
        "log_path" to mapOf(Language.RU to "Расположение файла логов (LIVE):", Language.EN to "Log file location (LIVE):"),
        "backups_desc" to mapOf(Language.RU to "История последних 5 сессий сохраняется на диске. Полный экспорт доступен во вкладке логов.", Language.EN to "The last 5 sessions are backed up on disk. Full export is available in the logs tab."),
        
        // Core required dialog & settings
        "xray_required" to mapOf(Language.RU to "ТРЕБУЕТСЯ ЯДРО XRAY", Language.EN to "XRAY-CORE REQUIRED"),
        "xray_required_desc" to mapOf(Language.RU to "официальный бинарный файл Xray-core требуется для безопасного проксирования трафика через VLESS Reality.\n\nХотите перейти в Настройки для его загрузки?", Language.EN to "To routing your traffic securely through your VLESS proxy, the official Xray-core binary is required.\n\nWould you like to navigate to Settings to download it now?"),
        "go_to_settings" to mapOf(Language.RU to "В Настройки", Language.EN to "Go to Settings"),
        "cancel" to mapOf(Language.RU to "Отмена", Language.EN to "Cancel"),
        
        // Ports Selector
        "ports_selector_title" to mapOf(Language.RU to "КОНТРОЛИРУЕМЫЕ ПОРТЫ АУДИТА", Language.EN to "AUDITED SENSITIVE PORTS"),
        "ports_selector_desc" to mapOf(Language.RU to "Выберите порты для перехвата и выделения красным цветом как опасные сетевые события", Language.EN to "Select TCP/UDP ports to intercept, audit, and log as sensitive traffic events"),

        // Server Profile Dialog
        "profile_dialog_add_title" to mapOf(Language.RU to "ДОБАВИТЬ НОВЫЙ ПРОФИЛЬ СЕРВЕРА", Language.EN to "ADD NEW SERVER PROFILE"),
        "profile_dialog_edit_title" to mapOf(Language.RU to "РЕДАКТИРОВАТЬ ПРОФИЛЬ ПРОКСИ", Language.EN to "EDIT PROXY SERVER PROFILE"),
        "profile_name" to mapOf(Language.RU to "Название профиля", Language.EN to "Profile Name"),
        "server_host" to mapOf(Language.RU to "Адрес сервера / IP", Language.EN to "Server Host / IP"),
        "server_port" to mapOf(Language.RU to "Порт сервера", Language.EN to "Server Port"),
        "vless_uuid" to mapOf(Language.RU to "VLESS UUID", Language.EN to "VLESS UUID"),
        "protocol_label" to mapOf(Language.RU to "Протокол (например, VLESS)", Language.EN to "Protocol (e.g. VLESS)"),
        "security_label" to mapOf(Language.RU to "Безопасность (none / tls / reality)", Language.EN to "Security (none / tls / reality)"),
        "transport_label" to mapOf(Language.RU to "Транспорт сети (tcp / ws / grpc)", Language.EN to "Network Transport (tcp / ws / grpc)"),
        "path_label" to mapOf(Language.RU to "WS Путь / Имя gRPC Сервиса", Language.EN to "WS Path / gRPC Service Name"),
        "sni_label" to mapOf(Language.RU to "SNI (Идентификатор имени сервера)", Language.EN to "SNI (Server Name Indication)"),
        "host_header" to mapOf(Language.RU to "Заголовок Host (e.g. cdn.example.com)", Language.EN to "Host Header (e.g. cdn.example.com)"),
        "tcp_obfuscation" to mapOf(Language.RU to "TCP Маскировка (например, http)", Language.EN to "TCP Obfuscation (e.g. http)"),
        "alpn_label" to mapOf(Language.RU to "ALPN (через запятую, e.g. h2,http/1.1)", Language.EN to "ALPN (comma-separated, e.g. h2,http/1.1)"),
        "allow_insecure_label" to mapOf(Language.RU to "Разрешить небезопасные соединения (Игнорировать ошибки TLS)", Language.EN to "Allow Insecure (Ignore TLS Errors)"),
        "reality_pubkey" to mapOf(Language.RU to "Публичный ключ Reality", Language.EN to "Reality Public Key"),
        "reality_shortid" to mapOf(Language.RU to "Reality Short ID", Language.EN to "Reality Short ID"),
        "tls_fingerprint" to mapOf(Language.RU to "TLS Отпечаток (например, chrome)", Language.EN to "TLS Fingerprint (e.g. chrome)"),
        "reality_spiderx" to mapOf(Language.RU to "Reality SpiderX (например, /path)", Language.EN to "Reality SpiderX (e.g. /path)"),
        "vless_flow" to mapOf(Language.RU to "Поток VLESS (например, xtls-rprx-vision)", Language.EN to "VLESS Flow (e.g. xtls-rprx-vision)"),
        "vless_encryption" to mapOf(Language.RU to "Шифрование VLESS (например, none)", Language.EN to "VLESS Encryption (e.g. none / mlkem...)"),
        "save_changes" to mapOf(Language.RU to "СОХРАНИТЬ ИЗМЕНЕНИЯ", Language.EN to "SAVE CHANGES"),
        
        // Core Downloader Card
        "core_installed" to mapOf(Language.RU to "Ядро Xray: УСТАНОВЛЕНО", Language.EN to "Xray-core: INSTALLED"),
        "core_not_installed" to mapOf(Language.RU to "Ядро Xray: НЕ УСТАНОВЛЕНО", Language.EN to "Xray-core: NOT INSTALLED"),
        "core_version_label" to mapOf(Language.RU to "Версия", Language.EN to "Version"),
        "core_requires_download" to mapOf(Language.RU to "Требуется загрузка скомпилированного бинарного файла XTLS.", Language.EN to "Requires download of compiled XTLS binary."),
        "core_checking_github" to mapOf(Language.RU to "Проверка GitHub на наличие обновлений...", Language.EN to "Checking GitHub for latest release..."),
        "core_check_updates" to mapOf(Language.RU to "ПРОВЕРИТЬ ОБНОВЛЕНИЯ", Language.EN to "CHECK FOR UPDATES"),
        "core_update_available" to mapOf(Language.RU to "ДОСТУПНО ОБНОВЛЕНИЕ!", Language.EN to "UPDATE AVAILABLE!"),
        "core_up_to_date" to mapOf(Language.RU to "ЯДРО ОБНОВЛЕНО ДО ПОСЛЕДНЕЙ ВЕРСИИ", Language.EN to "CORE IS UP TO DATE"),
        "core_check_again" to mapOf(Language.RU to "Проверить снова", Language.EN to "Check again"),
        "core_version_available_on_github" to mapOf(Language.RU to "доступна на GitHub", Language.EN to "is available on GitHub"),
        "core_installed_paren" to mapOf(Language.RU to "установлено", Language.EN to "Installed"),
        "core_latest_version_running" to mapOf(Language.RU to "Вы используете последнюю версию", Language.EN to "You are running the latest version"),
        "core_active_vpn_warning" to mapOf(Language.RU to "Активное VPN-подключение будет остановлено на время обновления.", Language.EN to "Active VPN connection will be stopped during update."),
        "core_force_reinstall" to mapOf(Language.RU to "ПРИНУДИТЕЛЬНО ПЕРЕУСТАНОВИТЬ ЯДРО", Language.EN to "FORCE REINSTALL CORE"),
        "core_update_to" to mapOf(Language.RU to "ОБНОВИТЬ ДО", Language.EN to "UPDATE TO"),
        "core_update_dbs" to mapOf(Language.RU to "ОБНОВИТЬ БАЗЫ GEOIP / GEOSITE", Language.EN to "UPDATE GEOIP / GEOSITE DATABASES"),
        "core_download_official" to mapOf(Language.RU to "СКАЧАТЬ ОФИЦИАЛЬНОЕ ЯДРО XRAY", Language.EN to "DOWNLOAD OFFICIAL XRAY CORE"),
        "core_status_downloading" to mapOf(Language.RU to "Загрузка официального ядра Xray...", Language.EN to "Downloading official Xray core..."),
        "core_status_updating" to mapOf(Language.RU to "Обновление ядра Xray...", Language.EN to "Updating Xray core..."),
        "core_status_updating_dbs" to mapOf(Language.RU to "Обновление баз GeoIP/GeoSite...", Language.EN to "Updating GeoIP/GeoSite databases..."),
        
        // VPN Service Notifications
        "notification_channel_name" to mapOf(Language.RU to "Служба подключения Sentinel", Language.EN to "Sentinel Connection Service"),
        "notification_channel_desc" to mapOf(Language.RU to "Показывает статус безопасного подключения Sentinel", Language.EN to "Shows secure Sentinel connectivity status"),
        "notification_title" to mapOf(Language.RU to "Защита Sentinel Активна", Language.EN to "Sentinel Protection Active"),
        "notification_socks_protected" to mapOf(Language.RU to "Локальная защита SOCKS5 активна. Случайный порт:", Language.EN to "SOCKS5 Loopback Protected. Port Random:"),
        "show_speed_in_notification_title" to mapOf(Language.RU to "Скорость в уведомлении", Language.EN to "Show Speed in Notification"),
        "show_speed_in_notification_desc" to mapOf(Language.RU to "Показывать входящую и исходящую скорость в строке состояния (уведомлении). Включение этой функции может незначительно увеличить энергопотребление.", Language.EN to "Display download and upload speeds in the status bar notification. Enabling this might slightly increase battery usage."),

        // Network Routing
        "routing_tab_apps" to mapOf(Language.RU to "ПРИЛОЖЕНИЯ", Language.EN to "APPLICATIONS"),
        "routing_tab_network" to mapOf(Language.RU to "ПРАВИЛА СЕТИ", Language.EN to "NETWORK RULES"),
        "geoip_section_title" to mapOf(Language.RU to "ОБХОД ПО GEOIP (ПРЯМОЕ ПОДКЛЮЧЕНИЕ)", Language.EN to "BYPASS BY GEOIP (DIRECT ROUTING)"),
        "geosite_section_title" to mapOf(Language.RU to "ПРОКСИРОВАНИЕ ПО GEOSITE (ЧЕРЕЗ VPN)", Language.EN to "PROXY BY GEOSITE (VPN ROUTING)"),
        "add_custom_rule" to mapOf(Language.RU to "Добавить правило (например, geoip:cn)", Language.EN to "Add custom rule (e.g., geoip:cn)"),
        "btn_add" to mapOf(Language.RU to "ДОБАВИТЬ", Language.EN to "ADD"),
        "preset_local_ips" to mapOf(Language.RU to "Локальные адреса (geoip:private)", Language.EN to "Local Addresses (geoip:private)"),
        "preset_ru_ips" to mapOf(Language.RU to "Российские адреса (geoip:ru)", Language.EN to "Russian Addresses (geoip:ru)"),
        "preset_cn_ips" to mapOf(Language.RU to "Китайские адреса (geoip:cn)", Language.EN to "Chinese Addresses (geoip:cn)"),
        "preset_us_ips" to mapOf(Language.RU to "США адреса (geoip:us)", Language.EN to "US Addresses (geoip:us)"),
        "preset_google" to mapOf(Language.RU to "Сервисы Google (geosite:google)", Language.EN to "Google Services (geosite:google)"),
        "preset_ads" to mapOf(Language.RU to "Блокировка рекламы (geosite:category-ads-all)", Language.EN to "Ad & Tracker Blocking (geosite:category-ads-all)"),
        "preset_youtube" to mapOf(Language.RU to "YouTube (geosite:youtube)", Language.EN to "YouTube (geosite:youtube)"),
        "preset_netflix" to mapOf(Language.RU to "Netflix (geosite:netflix)", Language.EN to "Netflix (geosite:netflix)"),
        "preset_instagram" to mapOf(Language.RU to "Instagram (geosite:instagram)", Language.EN to "Instagram (geosite:instagram)"),
        "preset_facebook" to mapOf(Language.RU to "Facebook (geosite:facebook)", Language.EN to "Facebook (geosite:facebook)"),
        "preset_twitter" to mapOf(Language.RU to "Twitter (geosite:twitter)", Language.EN to "Twitter (geosite:twitter)"),
        
        // LAN / Hotspot Sharing Settings
        "lan_sharing_title" to mapOf(Language.RU to "РАЗДАЧА И МАРШРУТИЗАЦИЯ (HOTSPOT)", Language.EN to "NETWORK SHARING (HOTSPOT)"),
        "lan_sharing_card_title" to mapOf(Language.RU to "Проксировать точку доступа (Hotspot)", Language.EN to "Share proxy over Hotspot / LAN"),
        "lan_sharing_card_desc" to mapOf(Language.RU to "Разрешить устройствам из вашей точки доступа подключаться через Sentinel", Language.EN to "Allow other devices connected to your Hotspot to route via Sentinel"),
        "lan_sharing_instructions_title" to mapOf(Language.RU to "ИНСТРУКЦИЯ ДЛЯ ПОДКЛЮЧЕНИЯ:", Language.EN to "HOW TO CONNECT CLIENT DEVICES:"),
        "lan_sharing_instruction_1" to mapOf(Language.RU to "1. Включите точку доступа (Hotspot) на телефоне и подключите к ней другое устройство.", Language.EN to "1. Turn on the Wi-Fi Hotspot on this phone and connect your client device to it."),
        "lan_sharing_instruction_2" to mapOf(Language.RU to "2. В свойствах Wi-Fi сети на подключенном устройстве настройте Прокси в режим Ручной.", Language.EN to "2. On the client device's Wi-Fi network settings, set Proxy to Manual."),
        "lan_sharing_instruction_3" to mapOf(Language.RU to "3. Укажите IP-адрес прокси (возьмите из блока «Активные адреса раздачи» выше после запуска защиты) и соответствующий Порт.", Language.EN to "3. Enter Proxy Host (retrieve from the 'Active Hotspot Gateways' section above after starting protection) and the corresponding Port."),
        
        // LAN Sharing Security / Habr Fix
        "lan_sharing_auth_title" to mapOf(Language.RU to "Защитить прокси паролем", Language.EN to "Secure Hotspot Proxy with Password"),
        "lan_sharing_auth_desc" to mapOf(Language.RU to "Включает авторизацию по логину и паролю для раздачи.", Language.EN to "Enables login and password authentication for hotspot sharing."),
        "lan_username" to mapOf(Language.RU to "Логин прокси", Language.EN to "Proxy Username"),
        "lan_password" to mapOf(Language.RU to "Пароль прокси", Language.EN to "Proxy Password"),
        "active_hotspot_ips" to mapOf(Language.RU to "Активные адреса раздачи:", Language.EN to "Active Hotspot Gateways:"),
        "binding_stealth" to mapOf(Language.RU to "🛡 Защита активна: прокси скрыт от локального сканирования на телефоне.", Language.EN to "🛡 Stealth Active: proxy is hidden from localhost port scanning."),
        
        // Dynamic Action Buttons
        "btn_show" to mapOf(Language.RU to "[ПОКАЗАТЬ]", Language.EN to "[SHOW]"),
        "btn_hide" to mapOf(Language.RU to "[СКРЫТЬ]", Language.EN to "[HIDE]"),
        "btn_copy" to mapOf(Language.RU to "[КОПИРОВАТЬ]", Language.EN to "[COPY]"),
        "btn_share" to mapOf(Language.RU to "[ПОДЕЛИТЬСЯ]", Language.EN to "[SHARE]"),
        
        // Proxy URL Export
        "lan_proxy_url" to mapOf(Language.RU to "Ссылка подключения (URL)", Language.EN to "Connection URL"),
        "lan_proxy_url_desc" to mapOf(Language.RU to "Используйте эту ссылку для быстрой настройки в Telegram, браузерах или сторонних клиентах.", Language.EN to "Use this link for instant configuration in Telegram, browser extensions, or proxy clients."),
        "lan_socks_title" to mapOf(Language.RU to "SOCKS5 прокси (Для v2rayNG / Nekobox / v2rayN)", Language.EN to "SOCKS5 (For v2rayNG / Nekobox / v2rayN)"),
        "lan_http_title" to mapOf(Language.RU to "HTTP прокси (Для Wi-Fi настроек ТВ / Telegram)", Language.EN to "HTTP (For Wi-Fi & Telegram)"),
        "lan_enable_http" to mapOf(Language.RU to "Запустить HTTP сервер", Language.EN to "Start HTTP Server"),
        "lan_enable_http_desc" to mapOf(Language.RU to "Для ручной настройки Wi-Fi в Android/iOS, телевизорах Smart TV и мессенджере Telegram.", Language.EN to "For manual Wi-Fi proxy settings in Android/iOS, Smart TVs, and Telegram client."),
        "lan_enable_socks" to mapOf(Language.RU to "Запустить SOCKS5 сервер", Language.EN to "Start SOCKS5 Server"),
        "lan_enable_socks_desc" to mapOf(Language.RU to "Для специализированных V2Ray-клиентов (v2rayNG, Nekobox, Shadowrocket).", Language.EN to "For dedicated V2Ray proxy clients (v2rayNG, Nekobox, Shadowrocket)."),
        "feed_btn" to mapOf(Language.RU to "ПОДПИСКИ", Language.EN to "FEEDS"),
        "btn_disconnect" to mapOf(Language.RU to "Отключить", Language.EN to "Disconnect"),
        "add_direct_profile_btn" to mapOf(Language.RU to "АНАЛИЗ", Language.EN to "ANALYZE"),
        "direct_export_error" to mapOf(Language.RU to "Невозможно экспортировать прямое соединение", Language.EN to "Cannot export direct connection"),
        "direct_profile_subtitle" to mapOf(Language.RU to "DIRECT • Режим анализа трафика", Language.EN to "DIRECT • Traffic Analysis Mode"),
        "analytics_collection_warning" to mapOf(Language.RU to "Внимание: Включен сбор и аудит сетевых данных для аналитики угроз.", Language.EN to "Notice: Active collection and audit of network data for threat analytics is enabled."),
        "analytics_confirm_btn" to mapOf(Language.RU to "ПОДТВЕРДИТЬ", Language.EN to "CONFIRM"),
        "analytics_collection_approved" to mapOf(Language.RU to "Аудит и сбор данных подтверждены пользователем", Language.EN to "Data audit and collection approved by user"),
        "disconnect_confirm_title" to mapOf(Language.RU to "Внимание: идет сбор данных", Language.EN to "Warning: Data Capture Active"),
        "disconnect_confirm_desc" to mapOf(Language.RU to "В данный момент выполняется сбор сетевого трафика (PCAP) для анализа угроз. Отключение VPN прервет этот процесс. Вы действительно хотите отключить VPN?", Language.EN to "A network packet capture (PCAP) is currently active for threat detection. Disconnecting the VPN will stop the capture. Are you sure you want to disconnect?"),
        "btn_confirm_disconnect" to mapOf(Language.RU to "Отключить", Language.EN to "Disconnect")
    )
}

@Composable
fun string(key: String): String {
    val lang by LanguageManager.currentLanguage.collectAsState()
    return LanguageManager.getString(key)
}
