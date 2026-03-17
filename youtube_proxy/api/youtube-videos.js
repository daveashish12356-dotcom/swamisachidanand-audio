/**
 * YouTube Proxy - Deploy to Vercel (free).
 * Set env: YOUTUBE_API_KEY (create new key with "None" restriction for server)
 * https://vercel.com/docs/functions
 */
module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  if (req.method === 'OPTIONS') return res.status(200).end();

  const apiKey = process.env.YOUTUBE_API_KEY;
  if (!apiKey) {
    return res.status(500).json({ error: 'YOUTUBE_API_KEY not set' });
  }

  const channelIds = (req.query.channel_ids || '').split(',').map(s => s.trim()).filter(Boolean);
  if (channelIds.length === 0) {
    return res.status(400).json({ error: 'channel_ids required (comma-separated)' });
  }

  const allVideos = [];
  for (const cid of channelIds.slice(0, 5)) {
    const playlistId = cid.startsWith('UC') ? 'UU' + cid.slice(2) : cid;
    const url = `https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=${playlistId}&maxResults=15&key=${apiKey}`;
    try {
      const r = await fetch(url, { headers: { 'User-Agent': 'SwamiSachidanand/1.0' } });
      if (!r.ok) continue;
      const data = await r.json();
      for (const it of data.items || []) {
        const sn = it.snippet || {};
        const vid = (sn.resourceId || {}).videoId;
        if (!vid) continue;
        const thumbs = sn.thumbnails || {};
        const thumb = (thumbs.medium || thumbs.high || {}).url || `https://img.youtube.com/vi/${vid}/hqdefault.jpg`;
        allVideos.push({
          videoId: vid,
          title: sn.title || '',
          thumbnailUrl: thumb,
          publishedAt: sn.publishedAt || ''
        });
      }
    } catch (e) {}
  }

  return res.status(200).json({ videos: allVideos });
};
