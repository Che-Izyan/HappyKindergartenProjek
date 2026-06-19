package com.example.happykindergartenprojek

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://uarbddadhcsrvlhjbbxm.supabase.co",
        supabaseKey = "sb_publishable_Kh-gMPzcNZlRMCQ6PhOQ8g_MGw81GKX"
    ) {
        install(Postgrest)
        install(Auth)
    }
}