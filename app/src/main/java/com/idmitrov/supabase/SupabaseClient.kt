package com.idmitrov.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime

object SupabaseManager {
    private const val SUPABASE_URL = "https://rxzwtcfbetdcccexzdvij.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_c2FXQocPtLi7sxVGrkPyLw_ze1A4kna"

    val client: SupabaseClient by lazy {
        createSupabaseClient(SUPABASE_URL, SUPABASE_ANON_KEY) {
            install(Postgrest)
            install(Storage)
            install(Realtime)
        }
    }
}
