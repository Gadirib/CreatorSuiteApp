# Publish legal pages (GitHub Pages)

## 1) Create a public repo (or use existing)
- Repo name example: `tiktokclone-legal`

## 2) Copy files
Upload these files to repo root:
- `privacy-policy.html`
- `terms-of-service.html`

## 3) Enable Pages
- GitHub -> Repo -> Settings -> Pages
- Source: `Deploy from a branch`
- Branch: `main` (or `master`), folder `/ (root)`

## 4) Your final URLs
- `https://<your-github-username>.github.io/<repo>/privacy-policy.html`
- `https://<your-github-username>.github.io/<repo>/terms-of-service.html`

## 5) Put URLs into TikTok Developer Console
- Privacy Policy URL -> privacy page URL
- Terms of Service URL -> terms page URL

## 6) OAuth config in TikTok console
- Redirect URI: `tiktokclone://oauth`
- Android package: `com.example.tiktokclone`

## 7) In app/build.gradle.kts
Set only client key in app (never put secret in app):
```
defaultConfig {
    buildConfigField("String", "TIKTOK_CLIENT_KEY", "\"YOUR_CLIENT_KEY\"")
}
```

Then Sync + Rebuild.
