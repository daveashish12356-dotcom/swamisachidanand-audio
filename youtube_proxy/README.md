# YouTube Videos Proxy

Agar app me sirf sample videos dikh rahe hon, to ye proxy deploy karo.

## Vercel pe deploy (free)

1. https://vercel.com pe sign up
2. "Add New" → "Project" → "Import" → `youtube_proxy` folder select karo
3. Environment Variables add karo:
   - `YOUTUBE_API_KEY` = naya API key (Google Cloud me "Application restrictions: None" set karo)
4. Deploy karo
5. `gradle.properties` me add karo:
   ```
   YOUTUBE_PROXY_URL=https://YOUR-PROJECT.vercel.app/api/youtube-videos
   ```

## API key "None" restriction

Google Cloud Console → APIs & Services → Credentials → API key edit:
- Application restrictions: **None** (Android ki jagah)
- API restrictions: YouTube Data API v3

Ye key proxy me use karo (gradle.properties me nahi - wo Android key hai).
