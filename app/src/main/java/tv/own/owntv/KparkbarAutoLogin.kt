package tv.own.owntv

import android.util.Log
import kotlinx.coroutines.flow.first
import tv.own.owntv.core.database.dao.ProfileDao
import tv.own.owntv.core.database.dao.SourceDao
import tv.own.owntv.core.settings.PlaylistRefresh
import tv.own.owntv.core.settings.SettingsRepository
import tv.own.owntv.core.setup.SourceImporter
import tv.own.owntv.core.sync.SyncScopeChoice

/**
 * Adds the build-time Xtream account (BuildConfig.KPB_XTREAM_*) on first launch so the app opens
 * straight to channels. Does nothing when the values are blank or the profile already has a source.
 */
object KparkbarAutoLogin {
    private const val TAG = "KparkbarAutoLogin"

    suspend fun seedIfNeeded(
        settings: SettingsRepository,
        profileDao: ProfileDao,
        sourceDao: SourceDao,
        importer: SourceImporter,
    ) {
        val server = BuildConfig.KPB_XTREAM_SERVER
        val user = BuildConfig.KPB_XTREAM_USER
        val pass = BuildConfig.KPB_XTREAM_PASS
        if (server.isBlank() || user.isBlank() || pass.isBlank()) return
        val pid = profileDao.resolveExistingProfileId(settings.activeProfileId.first()) ?: return
        if (sourceDao.observeForProfile(pid).first().isNotEmpty()) return
        importer.useProfile(pid)
        importer.xtream(
            name = "K PARK BAR",
            server = server,
            username = user,
            password = pass,
            userAgent = "",
            epgUrl = "",
            autoRefresh = PlaylistRefresh.OFF,
            live = SyncScopeChoice.Now,
            movies = SyncScopeChoice.Now,
            series = SyncScopeChoice.Now,
            preferHls = false,
            makeDefault = true,
            httpReferer = "",
        )
        Log.d(TAG, "seeded Xtream source for profile=$pid")
    }
}
