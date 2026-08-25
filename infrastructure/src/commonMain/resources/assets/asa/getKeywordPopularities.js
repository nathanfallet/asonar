// Apple Search Ads — keyword popularity (0-100), replayed from the authenticated app-ads session.
// The fetch runs in the page context so the session cookies ride along (credentials:"include").
// Auth is cookies only — no XSRF header is needed (verified from a live captured request).
// Endpoint + query captured verbatim from a logged-in app-ads.apple.com session.
//
// storefronts takes ISO-3166 alpha-2 country codes (e.g. "FR", "US") — NOT the numeric iTunes
// storefront ids. Verified live: ["FR"]/["US"] return per-country popularity; an empty list falls
// back to the session's org storefront. So {STOREFRONT} below is the keyword's country code.
async function getKeywordPopularity() {
    const query = `query getKeywordPopularitiesGql($adamId: String!, $storefronts: [String!]!, $keywordTerms: [String!]!) {
  recommendationV2 {
    getKeywordPopularities(
      adamId: $adamId
      storefronts: $storefronts
      keywordTerms: $keywordTerms
    ) {
      id
      name
      popularity
      matchType
      __typename
    }
    __typename
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
            operationName: "getKeywordPopularitiesGql",
            variables: {adamId: "{ADAM_ID}", storefronts: ["{STOREFRONT}"], keywordTerms: ["{TERM}"]},
            query
        })
    });
    if (!response.ok) return null;
    return await response.json();
}

getKeywordPopularity();
