// Apple Search Ads — recommended keywords for an app, replayed from the authenticated app-ads session.
// Same mechanics as getKeywordPopularities.js: the fetch runs in the page context so the session
// cookies ride along (credentials:"include"), auth is cookies only (no XSRF header). Endpoint + query
// captured verbatim from the app-ads.apple.com bundle (operation getRecommendedKeywordsGql).
//
// storefronts takes ISO-3166 alpha-2 country codes (e.g. "FR", "US"), like the popularity call.
//
// ⚠️ {TEXT} (the seed) is NOT optional in practice, even though the schema allows null. Verified live:
// with a null seed Apple returns the ~20 biggest apps of the store instead of anything related to the
// app ("instagram", "snapchat", "tiktok"…) — global top charts, useless as candidates. Seeded with a
// term we already track it returns dozens of genuinely related terms, each WITH its popularity — which
// is the whole point: unlike terms we invent, a suggestion comes with proof it is searched.
async function getRecommendedKeywords() {
    const query = `query getRecommendedKeywordsGql($adamId: String!, $text: String, $storefronts: [String]) {
  recommendationV2 {
    getRecommendedKeywords(adamId: $adamId, text: $text, storefronts: $storefronts) {
      id
      name
      popularity
      matchType
    }
  }
}`;
    const response = await fetch("{ENDPOINT}", {
        method: "POST",
        headers: {
            "accept": "application/json, text/plain, */*",
            "content-type": "application/json"
        },
        credentials: "include",
        body: JSON.stringify({
            operationName: "getRecommendedKeywordsGql",
            variables: {adamId: "{ADAM_ID}", text: "{TEXT}", storefronts: ["{STOREFRONT}"]},
            query
        })
    });
    if (!response.ok) return null;
    return await response.json();
}

getRecommendedKeywords();
