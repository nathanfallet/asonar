package me.nathanfallet.asonar.domain.usecases.apps

import me.nathanfallet.asonar.domain.models.keywords.OpportunityVerdict
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Locks which keywords the app-load background refresh re-fetches. */
class RefreshAppKeywordsSelectionTest {

    private fun refresh(ranked: Boolean, verdict: OpportunityVerdict) =
        RefreshAppKeywordsUseCaseImpl.shouldRefresh(ranked, verdict)

    @Test
    fun opportunitiesAndPending_areRefreshed() {
        assertTrue(refresh(false, OpportunityVerdict.YES))
        assertTrue(refresh(false, OpportunityVerdict.YES_BUT))
        // Pending: not enough data yet → keep refreshing so it resolves.
        assertTrue(refresh(false, OpportunityVerdict.UNKNOWN))
    }

    @Test
    fun settledWallsAndParkedTerms_areSkipped() {
        assertFalse(refresh(false, OpportunityVerdict.NO))
        assertFalse(refresh(false, OpportunityVerdict.RESERVE))
    }

    @Test
    fun anythingWeRankOn_isRefreshed_regardlessOfVerdict() {
        assertTrue(refresh(true, OpportunityVerdict.NO))
        assertTrue(refresh(true, OpportunityVerdict.RESERVE))
    }
}
