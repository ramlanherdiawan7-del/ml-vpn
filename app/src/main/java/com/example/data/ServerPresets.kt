package com.example.data

import com.example.model.GameServer

object ServerPresets {
    val DEFAULT_SERVERS = listOf(
        GameServer(
            id = "server_kh_01",
            country = "Kamboja",
            city = "Phnom Penh",
            flagEmoji = "🇰🇭",
            hostIp = "kh-gateway.dns.cloudflare.com",
            port = 443,
            categoryTag = "Low Ping",
            displayTag = "Recommended (Lowest Ping)",
            matchmakingNote = "Server dengan latensi terendah untuk SEA. Lobi santai, banyak pemain solo pemula, sangat cocok push rank.",
            baselineMs = 45
        ),
        GameServer(
            id = "server_mm_01",
            country = "Myanmar",
            city = "Yangon",
            flagEmoji = "🇲🇲",
            hostIp = "mm-yangon.cloudflare.com",
            port = 443,
            categoryTag = "Easy Match",
            displayTag = "Casual Lobby",
            matchmakingNote = "Lobi match kasual dan tidak terlalu kompetitif. Cocok untuk latihan hero baru atau party santai.",
            baselineMs = 65
        ),
        GameServer(
            id = "server_la_01",
            country = "Laos",
            city = "Vientiane",
            flagEmoji = "🇱🇦",
            hostIp = "vte.cloudflare.com",
            port = 443,
            categoryTag = "Easy Match",
            displayTag = "Low Playerbase",
            matchmakingNote = "Jumlah pemain lokal sedikit, sistem matchmaking sering menyisipkan tim pelengkap AI/bot.",
            baselineMs = 74
        ),
        GameServer(
            id = "server_in_mumbai",
            country = "India",
            city = "Mumbai",
            flagEmoji = "🇮🇳",
            hostIp = "bom.cloudflare.com",
            port = 443,
            categoryTag = "Bot Lobbies",
            displayTag = "High Bot Chance",
            matchmakingNote = "Peluang menemukan lawan Bot/AI tertinggi saat bermain di jam non-peak. Sangat efektif menaikkan winrate.",
            baselineMs = 88
        ),
        GameServer(
            id = "server_in_chennai",
            country = "India",
            city = "Chennai",
            flagEmoji = "🇮🇳",
            hostIp = "maa.cloudflare.com",
            port = 443,
            categoryTag = "Bot Lobbies",
            displayTag = "High Bot Chance",
            matchmakingNote = "Gateway India Selatan dengan koneksi stabil dan frekuensi bot matchmaking konsisten.",
            baselineMs = 92
        ),
        GameServer(
            id = "server_tr_01",
            country = "Turki",
            city = "Istanbul",
            flagEmoji = "🇹🇷",
            hostIp = "ist.cloudflare.com",
            port = 443,
            categoryTag = "Easy Match",
            displayTag = "Europe Timezone",
            matchmakingNote = "Server regional Eropa dengan rotasi pemain berbeda, cocok untuk variasi meta dan lawan dari region EU.",
            baselineMs = 125
        ),
        GameServer(
            id = "server_ph_01",
            country = "Filipina",
            city = "Manila",
            flagEmoji = "🇵🇭",
            hostIp = "mnl.cloudflare.com",
            port = 443,
            categoryTag = "Low Ping",
            displayTag = "SEA Competitive",
            matchmakingNote = "Ping rendah dengan mekanik gameplay cepat untuk menguji kemampuan makro.",
            baselineMs = 52
        )
    )
}
